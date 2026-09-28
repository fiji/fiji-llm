/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.script;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditor.Executer;
import org.scijava.ui.swing.script.TextEditorTab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.imagej.legacy.LegacyService;
import sc.fiji.llm.execution.EnvironmentSnapshotService;
import sc.fiji.llm.execution.EnvironmentSnapshotService.EnvironmentImpact;
import sc.fiji.llm.execution.EnvironmentSnapshotService.PixelChangeTracking;
import sc.fiji.llm.log.TextLogs;
import sc.fiji.llm.ui.AWTDialogUtils;
import sc.fiji.llm.ui.TextEditorUtils;

/** Runs scripts through the SciJava Script Editor and tracks their outcomes. */
@Plugin(type = Service.class, priority = Priority.HIGH)
public final class DefaultScriptExecutionService extends AbstractService implements
	ScriptExecutionService
{

	private static final long POLL_INTERVAL_MS = 50;
	private static final long EXECUTER_START_TIMEOUT_MS = 500;

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private LogService logService;

	@Parameter
	private EnvironmentSnapshotService environmentSnapshotService;

	private final Map<String, Execution> executions = new ConcurrentHashMap<>();

	/** Starts a run and waits for completion, a blocking dialog, or the wait limit. */
	@Override
	public ExecutionResult run(final ScriptID scriptID, final RunKind kind,
		final boolean returnWhenBlocked)
	{
		if (scriptID == null) throw new IllegalArgumentException(
			"scriptID cannot be null");
		if (kind == null) throw new IllegalArgumentException("run kind cannot be null");

		final Execution execution = new Execution(newRunID(),
			scriptID, kind);
		executions.put(execution.runID, execution);

		try {
			start(execution);
		}
		catch (final Exception e) {
			finishWithInfrastructureError(execution, e);
			return execution.snapshot();
		}

		execution.monitorThread = new Thread(() -> monitor(execution),
			"Fiji-script-run-" + execution.runID);
		execution.monitorThread.setDaemon(true);
		execution.monitorThread.start();
		return await(execution, returnWhenBlocked);
	}

	/** Returns the current state of a previously started run. */
	@Override
	public ExecutionResult status(final String runID, final RunKind expectedKind) {
		if (runID == null || runID.isBlank()) return null;
		final Execution execution = executions.get(runID);
		if (execution == null || execution.kind != expectedKind) return null;
		if (execution.status == Status.RUNNING || execution.status == Status.BLOCKED_BY_DIALOG) {
			refreshLiveState(execution);
		}
		return execution.snapshot();
	}

	private void start(final Execution execution) throws Exception {
		execution.startedAt = System.currentTimeMillis();
		final Throwable[] preparationFailure = new Throwable[1];
		final Runnable prepareScript = () -> {
			try {
				if (TextEditor.instances == null || execution.scriptID.editorIndex < 0 ||
					execution.scriptID.editorIndex >= TextEditor.instances.size())
				{
					throw new IllegalStateException("No visible Script Editor found for script_id " +
						execution.scriptID);
				}
				execution.textEditor = TextEditor.instances.get(execution.scriptID.editorIndex);
				if (execution.textEditor == null || !execution.textEditor.isVisible()) {
					throw new IllegalStateException(
						"A visible Script Editor is required for script_id " + execution.scriptID);
				}
				final ScriptID activeScriptID = TextEditorUtils.getActiveScriptID();
				if (!execution.scriptID.equals(activeScriptID)) {
					throw new IllegalStateException("script_id " + execution.scriptID +
						" is not the active script");
				}
				execution.tab = execution.textEditor.getTab(execution.scriptID.tabIndex);
				if (execution.tab == null) {
					throw new IllegalStateException("No script found for script_id " +
						execution.scriptID);
				}
				final ScriptContextItem context = ScriptContextUtilities
					.buildScriptContextItem(execution.scriptID.editorIndex,
						execution.scriptID.tabIndex);
				execution.scriptName = context.getScriptName();
				if (execution.kind == RunKind.MACRO && !ScriptExecutionService.isMacroScript(
					execution.scriptName)) {
					throw new IllegalStateException("The active script is not an .ijm macro");
				}
				if (execution.kind == RunKind.SCRIPT && ScriptExecutionService.isMacroScript(
					execution.scriptName)) {
					throw new IllegalStateException(
						"The active .ijm script must be run with fiji_macro_run");
				}
				execution.logsBefore = TextEditorUtils.getLogs(execution.textEditor,
					execution.tab);
			}
			catch (final Throwable t) {
				preparationFailure[0] = t;
			}
		};

		if (SwingUtilities.isEventDispatchThread()) prepareScript.run();
		else SwingUtilities.invokeAndWait(prepareScript);
		if (preparationFailure[0] != null) {
			throw new IllegalStateException("Could not start script execution",
				preparationFailure[0]);
		}

		execution.environmentCapture = environmentSnapshotService.capture(
			PixelChangeTracking.FINAL_SHA256);

		final Throwable[] startupFailure = new Throwable[1];
		final Runnable startScript = () -> {
			try {
				execution.textEditor.runText();
				execution.executer = execution.tab.getExecuter();
			}
			catch (final Throwable t) {
				startupFailure[0] = t;
			}
		};

		if (SwingUtilities.isEventDispatchThread()) startScript.run();
		else SwingUtilities.invokeAndWait(startScript);
		if (startupFailure[0] != null) {
			throw new IllegalStateException("Could not start script execution",
				startupFailure[0]);
		}
	}

	private ExecutionResult await(final Execution execution,
		final boolean returnWhenBlocked)
	{
		final long waitDeadline = System.currentTimeMillis() +
			ScriptExecutionService.DEFAULT_WAIT_MS;
		while (true) {
			final Status status = execution.status;
			if (isTerminal(status) || returnWhenBlocked && status == Status.BLOCKED_BY_DIALOG)
			{
				if (status == Status.RUNNING || status == Status.BLOCKED_BY_DIALOG) {
					refreshLiveState(execution);
				}
				return execution.snapshot();
			}
			final long remainingWait = waitDeadline - System.currentTimeMillis();
			if (remainingWait <= 0) {
				refreshLiveState(execution);
				return execution.snapshot(true);
			}
			synchronized (execution) {
				try {
					execution.wait(Math.min(POLL_INTERVAL_MS, remainingWait));
				}
				catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return execution.snapshotWith(Status.INFRASTRUCTURE_ERROR,
						"Interrupted while waiting for script execution");
				}
			}
		}
	}

	private void monitor(final Execution execution) {
		try {
			final long startupDeadline = System.currentTimeMillis() +
				EXECUTER_START_TIMEOUT_MS;
			while (execution.executer == null && System.currentTimeMillis() < startupDeadline)
			{
				execution.executer = execution.tab.getExecuter();
				if (execution.executer == null) Thread.sleep(POLL_INTERVAL_MS);
			}

			while (execution.executer != null && isExecuting(execution)) {
				final EnvironmentImpact environment = refreshEnvironment(execution);
				final List<AWTDialogUtils.DialogInfo> dialogs = environment == null ?
					Collections.emptyList() : environment.getNewModalDialogs();
				if (!dialogs.isEmpty()) {
					execution.status = Status.BLOCKED_BY_DIALOG;
				}
				else if (execution.status == Status.BLOCKED_BY_DIALOG) {
					execution.status = Status.RUNNING;
				}

				Thread.sleep(POLL_INTERVAL_MS);
			}

			if (execution.executer == null || !isExecuting(execution)) {
				finishFromLogs(execution);
			}
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			finishWithInfrastructureError(execution, e);
		}
		catch (final Throwable t) {
			finishWithInfrastructureError(execution, t);
		}
	}

	private boolean isExecuting(final Execution execution) {
		return execution.textEditor.getExecutingTasks().contains(execution.executer);
	}

	private void refreshLiveState(final Execution execution) {
		if (execution.textEditor != null && execution.tab != null) {
			try {
				execution.logs = readLogs(execution).deltaFrom(execution.logsBefore)
					.withoutStartedBanners();
			}
			catch (final Exception e) {
				execution.diagnostic = e.toString();
			}
		}
		final EnvironmentImpact environment = refreshEnvironment(execution);
		final List<AWTDialogUtils.DialogInfo> dialogs = environment == null ?
			Collections.emptyList() : environment.getNewModalDialogs();
		if (!dialogs.isEmpty()) {
			execution.status = Status.BLOCKED_BY_DIALOG;
			captureErrorDialog(execution, environment);
		}
		else if (execution.status == Status.BLOCKED_BY_DIALOG) {
			execution.status = Status.RUNNING;
		}
	}

	private EnvironmentImpact refreshEnvironment(final Execution execution) {
		if (execution.environmentCapture == null) return execution.environment;
		execution.environment = execution.environmentCapture.current();
		return execution.environment;
	}

	static Status classifyFinishedStatus(final String logErrors,
		final String consoleStderr, final String primaryError,
		final String errorDialog)
	{
		return hasText(logErrors) || hasText(consoleStderr) ||
			hasText(primaryError) || hasText(errorDialog) ?
			Status.FINISHED_WITH_ERRORS : Status.SUCCESS;
	}

	private static boolean hasText(final String value) {
		return value != null && !value.isBlank();
	}

	private void finishFromLogs(final Execution execution) {
		try {
			final TextLogs finalLogs = readLogs(execution);
			final TextLogs delta = finalLogs.deltaFrom(execution.logsBefore)
				.withoutStartedBanners();
			refreshEnvironment(execution);
			final String consoleStderr = execution.environment == null ? "" : execution
				.environment.getConsoleStderr().trim();
			final String diagnostic = delta.getErrors().trim();
			if (!diagnostic.isEmpty() && (execution.primaryError == null ||
				execution.primaryError.isBlank())) execution.primaryError = diagnostic;
			if (!consoleStderr.isEmpty() && (execution.primaryError == null ||
				execution.primaryError.isBlank())) execution.primaryError = consoleStderr.lines()
				.findFirst().orElse(consoleStderr);
			finish(execution, classifyFinishedStatus(diagnostic, consoleStderr,
				execution.primaryError, execution.errorDialog), null, delta);
		}
		catch (final Throwable t) {
			finishWithInfrastructureError(execution, t);
		}
	}

	private static void captureErrorDialog(final Execution execution,
		final EnvironmentImpact environment)
	{
		if (environment == null) return;
		final String dialogError = extractErrorDialog(environment);
		if (dialogError == null || dialogError.isBlank()) return;
		execution.errorDialog = dialogError;
		if (execution.primaryError == null || execution.primaryError.isBlank()) {
			execution.primaryError = dialogError.lines().findFirst().orElse(dialogError);
		}
	}

	static boolean isMacroErrorDialog(final String className,
		final String title)
	{
		if (className == null || !className.endsWith("GenericDialog")) return false;
		return "Macro Error".equals(title) || "No Image".equals(title);
	}

	private static String extractErrorDialog(final EnvironmentImpact environment) {
		final List<AWTDialogUtils.DialogInfo> dialogs = environment == null ?
			Collections.emptyList() : environment.getNewModalDialogs();
		final StringBuilder messages = new StringBuilder();
		for (final AWTDialogUtils.DialogInfo dialog : dialogs) {
			if (!isMacroErrorDialog(dialog.getClassName(), dialog.getTitle())) continue;
			for (final String message : dialog.getMessages()) {
				if (message == null || message.isBlank()) continue;
				if (messages.length() > 0) messages.append(System.lineSeparator());
				messages.append(message);
			}
		}
		return messages.length() == 0 ? null : messages.toString();
	}

	private TextLogs readLogs(final Execution execution) throws Exception {
		final TextLogs[] result = new TextLogs[1];
		final Runnable read = () -> result[0] = TextEditorUtils.getLogs(
			execution.textEditor, execution.tab);
		if (SwingUtilities.isEventDispatchThread()) read.run();
		else SwingUtilities.invokeAndWait(read);
		return result[0];
	}

	private void finishWithInfrastructureError(final Execution execution,
		final Throwable error)
	{
		finish(execution, Status.INFRASTRUCTURE_ERROR, error.toString());
	}

	private void finish(final Execution execution, final Status status,
		final String diagnostic)
	{
		finish(execution, status, diagnostic, null);
	}

	private synchronized void finish(final Execution execution,
		final Status status, final String diagnostic, final TextLogs logs)
	{
		if (isTerminal(execution.status)) return;
		execution.status = status;
		execution.diagnostic = diagnostic;
		if (logs != null) execution.logs = logs;
		else if (execution.logs == null && execution.textEditor != null) {
			try {
				execution.logs = readLogs(execution).deltaFrom(execution.logsBefore)
					.withoutStartedBanners();
			}
			catch (final Exception e) {
				execution.diagnostic = e.toString();
			}
		}
		if (execution.environmentCapture != null) execution.environment = execution
			.environmentCapture.finish();
		execution.finishedAt = System.currentTimeMillis();
		synchronized (execution) {
			execution.notifyAll();
		}
	}

	private static boolean isTerminal(final Status status) {
		return status == Status.SUCCESS || status == Status.FINISHED_WITH_ERRORS ||
			status == Status.INFRASTRUCTURE_ERROR;
	}

	private static String newRunID() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	public static final class ExecutionResult implements ScriptExecutionService.ExecutionResult {

		private final Execution execution;
		private final boolean waitExpired;

		private ExecutionResult(final Execution execution, final boolean waitExpired) {
			this.execution = execution;
			this.waitExpired = waitExpired;
		}

		@Override
		public JsonObject toJson() {
			final JsonObject result = new JsonObject();
			result.addProperty("run_id", execution.runID);
			result.addProperty("status", execution.status.toString());
			if (waitExpired) result.addProperty("wait_expired", true);
			result.addProperty("duration_ms", execution.elapsedMillis());
			result.addProperty("script_id", execution.scriptID.toString());
			addTextProperty(result, "script_name", execution.scriptName);

			final TextLogs logs = execution.logs == null ? new TextLogs("", "") :
				execution.logs.withoutGenericMacroInterpreterMessages();
			final EnvironmentImpact environment = execution.environment;
			final String consoleStderr = environment == null ? "" : environment
				.getConsoleStderr();
			addTextProperty(result, "output", logs.getOutput());
			addTextProperty(result, "errors", appendDiagnostic(logs.getErrors(),
				consoleStderr));
			addTextProperty(result, "error_dialog", execution.errorDialog);

			final JsonArray dialogs = new JsonArray();
			final List<AWTDialogUtils.DialogInfo> blockingDialogs = environment == null ?
				Collections.emptyList() : environment.getNewModalDialogs();
			for (final AWTDialogUtils.DialogInfo dialog : blockingDialogs) {
				final JsonObject dialogJson = new JsonObject();
						dialogJson.addProperty("dialog_title", dialog.getTitle());
						dialogJson.addProperty("dialog_class_name", dialog.getClassName());
				dialogJson.addProperty("visible", dialog.isVisible());
				dialogJson.addProperty("active", dialog.isActive());
				dialogJson.addProperty("modal", dialog.isModal());
				dialogJson.addProperty("modality_type", dialog.getModalityType());
				if (!dialog.getOwnerName().isBlank()) dialogJson.addProperty("owner_name",
					dialog.getOwnerName());
				final JsonArray messages = new JsonArray();
				for (final String message : dialog.getMessages()) messages.add(message);
				if (messages.size() > 0) dialogJson.add("messages", messages);
				final JsonArray buttons = new JsonArray();
				for (final AWTDialogUtils.ButtonInfo button : dialog.getButtons()) {
					final JsonObject buttonJson = new JsonObject();
							buttonJson.addProperty("button_text", button.getText());
					if (button.getActionCommand() != null && !button.getActionCommand().isBlank())
						buttonJson.addProperty("action_command", button.getActionCommand());
					buttonJson.addProperty("enabled", button.isEnabled());
					buttonJson.addProperty("visible", button.isVisible());
					buttons.add(buttonJson);
				}
				if (buttons.size() > 0) dialogJson.add("buttons", buttons);
				dialogs.add(dialogJson);
			}
			if (dialogs.size() > 0) result.add("dialogs", dialogs);
			if (environment != null) {
				final JsonObject environmentJson = environment.toJson();
				if (environmentJson.size() > 0) result.add("environment_impact", environmentJson);
			}
			addTextProperty(result, "diagnostic", execution.diagnostic);
			if (execution.status == Status.FINISHED_WITH_ERRORS || execution.status ==
				Status.INFRASTRUCTURE_ERROR)
			{
				addGuideRecommendation(result, execution.kind == RunKind.MACRO ?
					"creating-macros" : "scripting");
			}
			if (execution.status == Status.BLOCKED_BY_DIALOG) result.addProperty(
				"recommended_action",
				"Inspect the dialog with fiji_ui_dialogs_read, then use " +
					"fiji_ui_dialog_respond with its exact title and button text, or " +
					"fiji_ui_dialog_close with its exact title. " +
					"This run remains active.");
			if (waitExpired && execution.status == Status.RUNNING) {
				result.addProperty("recommended_action",
					"The run is still active. Poll the corresponding run-status tool with run_id; do not start the script again.");
			}
			return result;
		}

		private static String appendDiagnostic(final String errors,
			final String consoleStderr)
		{
			if (consoleStderr == null || consoleStderr.isBlank()) return errors;
			if (errors == null || errors.isBlank()) return consoleStderr;
			if (errors.contains(consoleStderr)) return errors;
			return errors + System.lineSeparator() + consoleStderr;
		}

		private static void addTextProperty(final JsonObject result, final String name,
			final String value)
		{
			if (value != null && !value.isBlank()) result.addProperty(name, value);
		}

		private static void addGuideRecommendation(final JsonObject result,
			final String guideId)
		{
			final JsonArray recommendations = new JsonArray();
			final JsonObject recommendation = new JsonObject();
			recommendation.addProperty("tool", "fiji_guide_read");
			final JsonObject arguments = new JsonObject();
			arguments.addProperty("guide_id", guideId);
			recommendation.add("arguments", arguments);
			recommendations.add(recommendation);
			result.add("guide_recommendations", recommendations);
		}
	}

	private static final class Execution {

		private final String runID;
		private final ScriptID scriptID;
		private final RunKind kind;
		private volatile Status status = Status.RUNNING;
		private volatile String scriptName;
		private volatile long startedAt;
		private volatile long finishedAt;
		private volatile String diagnostic;
		private volatile TextLogs logs;
		private volatile TextLogs logsBefore;
		private volatile EnvironmentSnapshotService.EnvironmentCapture environmentCapture;
		private volatile EnvironmentImpact environment;
		private volatile TextEditor textEditor;
		private volatile TextEditorTab tab;
		private volatile Executer executer;
		private volatile Thread monitorThread;
		private volatile String primaryError;
		private volatile String errorDialog;

		private Execution(final String runID, final ScriptID scriptID,
			final RunKind kind)
		{
			this.runID = runID;
			this.scriptID = scriptID;
			this.kind = kind;
		}

		private long elapsedMillis() {
			final long end = finishedAt > 0 ? finishedAt : System.currentTimeMillis();
			return Math.max(0, end - startedAt);
		}

		private ExecutionResult snapshot() {
			return snapshot(false);
		}

		private ExecutionResult snapshot(final boolean waitExpired) {
			return new ExecutionResult(this, waitExpired);
		}

		private ExecutionResult snapshotWith(final Status status,
			final String diagnostic)
		{
			this.status = status;
			this.diagnostic = diagnostic;
			if (environmentCapture != null) environment = environmentCapture.finish();
			return snapshot();
		}
	}
}
