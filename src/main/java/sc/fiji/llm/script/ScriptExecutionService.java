/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
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

import net.imagej.ImageJService;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.execution.ExecutionEnvironmentSnapshotService;
import sc.fiji.llm.execution.ExecutionEnvironmentSnapshotService.EnvironmentImpact;
import sc.fiji.llm.log.TextLogs;
import sc.fiji.llm.ui.AWTDialogUtils;
import sc.fiji.llm.ui.TextEditorUtils;

/** Runs scripts through the SciJava Script Editor and tracks their outcomes. */
@Plugin(type = Service.class, priority = Priority.HIGH)
public final class ScriptExecutionService extends AbstractService implements
	ImageJService
{

	public static final long DEFAULT_TIMEOUT_MS = 30_000;

	private static final long POLL_INTERVAL_MS = 50;
	private static final long EXECUTER_START_TIMEOUT_MS = 500;
	private static final long KILL_TIMEOUT_MS = 5_000;

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private LogService logService;

	@Parameter
	private ExecutionEnvironmentSnapshotService environmentSnapshotService;

	private final Map<String, Execution> executions = new ConcurrentHashMap<>();

	public enum RunKind {
		SCRIPT, MACRO
	}

	public enum Status {
		RUNNING("running"),
		SUCCESS("success"),
		FINISHED_WITH_ERRORS("finished_with_errors"),
		BLOCKED_BY_DIALOG("blocked_by_dialog"),
		TIMED_OUT("timed_out"),
		INFRASTRUCTURE_ERROR("infrastructure_error");

		private final String value;

		Status(final String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/** Starts a run and waits for completion or, optionally, a blocking dialog. */
	public ExecutionResult run(final ScriptID scriptID, final RunKind kind,
		final boolean returnWhenBlocked)
	{
		if (scriptID == null) throw new IllegalArgumentException(
			"scriptID cannot be null");
		if (kind == null) throw new IllegalArgumentException("run kind cannot be null");

		final Execution execution = new Execution(UUID.randomUUID().toString(),
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
	public ExecutionResult status(final String runID, final RunKind expectedKind) {
		if (runID == null || runID.isBlank()) return null;
		final Execution execution = executions.get(runID);
		if (execution == null || execution.kind != expectedKind) return null;
		if (execution.status == Status.RUNNING || execution.status == Status.BLOCKED_BY_DIALOG) {
			refreshLiveState(execution);
		}
		return execution.snapshot();
	}

	public static boolean isMacroScript(final String scriptName) {
		return scriptName != null && scriptName.toLowerCase().endsWith(".ijm");
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
				if (execution.kind == RunKind.MACRO && !isMacroScript(execution.scriptName)) {
					throw new IllegalStateException("The active script is not an .ijm macro");
				}
				if (execution.kind == RunKind.SCRIPT && isMacroScript(execution.scriptName)) {
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

		execution.environmentCapture = environmentSnapshotService.capture();

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
		while (true) {
			final Status status = execution.status;
			if (isTerminal(status) || returnWhenBlocked && status == Status.BLOCKED_BY_DIALOG)
			{
				if (status == Status.RUNNING || status == Status.BLOCKED_BY_DIALOG) {
					refreshLiveState(execution);
				}
				return execution.snapshot();
			}
			synchronized (execution) {
				try {
					execution.wait(POLL_INTERVAL_MS);
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

				if (System.currentTimeMillis() - execution.startedAt >=
					DEFAULT_TIMEOUT_MS)
				{
					execution.timeoutRequested = true;
					try {
						kill(execution);
						execution.executionTerminated = !isExecuting(execution);
						if (!execution.executionTerminated) {
							execution.terminationFailure = "Timeout requested but script execution is still running";
						}
					}
					catch (final Throwable t) {
						execution.executionTerminated = false;
						execution.terminationFailure = t.toString();
					}

					final String diagnostic = execution.terminationFailure == null ? null :
						"Timeout requested, but execution did not terminate: " +
							execution.terminationFailure;
					finish(execution, Status.TIMED_OUT, diagnostic);
					return;
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

	private void kill(final Execution execution) throws Exception {
		execution.timeoutRequested = true;
		try {
			final Runnable kill = () -> execution.textEditor.kill(execution.executer);
			if (SwingUtilities.isEventDispatchThread()) kill.run();
			else SwingUtilities.invokeAndWait(kill);
		}
		catch (final Throwable t) {
			execution.executionTerminated = false;
			execution.terminationFailure = t.toString();
			throw t;
		}

		final long deadline = System.currentTimeMillis() + KILL_TIMEOUT_MS;
		while (isExecuting(execution) && System.currentTimeMillis() < deadline) {
			Thread.sleep(POLL_INTERVAL_MS);
		}
		execution.executionTerminated = !isExecuting(execution);
		if (!execution.executionTerminated) {
			execution.terminationFailure = "Timeout requested but script execution is still running";
		}
	}

	private void finishFromLogs(final Execution execution) {
		try {
			final TextLogs finalLogs = readLogs(execution);
			final TextLogs delta = finalLogs.deltaFrom(execution.logsBefore)
				.withoutStartedBanners();
			final String diagnostic = delta.getErrors().trim();
			if (!diagnostic.isEmpty() && (execution.primaryError == null ||
				execution.primaryError.isBlank())) execution.primaryError = diagnostic;
			finish(execution, diagnostic.isEmpty() ? Status.SUCCESS :
				Status.FINISHED_WITH_ERRORS, null, delta);
		}
		catch (final Throwable t) {
			finishWithInfrastructureError(execution, t);
		}
	}

	private static void captureErrorDialog(final Execution execution,
		final EnvironmentImpact environment)
	{
		if (environment == null) return;
		final String dialogError = extractDialogError(environment);
		if (dialogError == null || dialogError.isBlank()) return;
		execution.errorDialog = dialogError;
		if (execution.primaryError == null || execution.primaryError.isBlank()) {
			execution.primaryError = dialogError.lines().findFirst().orElse(dialogError);
		}
	}

	private static String extractDialogError(final EnvironmentImpact environment) {
		final List<AWTDialogUtils.DialogInfo> dialogs = environment == null ?
			Collections.emptyList() : environment.getNewModalDialogs();
		final StringBuilder messages = new StringBuilder();
		for (final AWTDialogUtils.DialogInfo dialog : dialogs) {
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
		if (status != Status.RUNNING && status != Status.BLOCKED_BY_DIALOG &&
			status != Status.TIMED_OUT)
		{
			execution.executionTerminated = true;
		}
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
			status == Status.TIMED_OUT || status == Status.INFRASTRUCTURE_ERROR;
	}

	public static final class ExecutionResult {

		private final Execution execution;

		private ExecutionResult(final Execution execution) {
			this.execution = execution;
		}

		public JsonObject toJson() {
			final JsonObject result = new JsonObject();
			result.addProperty("run_id", execution.runID);
			result.addProperty("status", execution.status.toString());
			result.addProperty("completion_state", execution.status.toString());
			result.addProperty("timeout_requested", execution.timeoutRequested);
			result.addProperty("execution_terminated", execution.executionTerminated);
			result.addProperty("termination_status", execution.getTerminationStatus());
			if (execution.terminationFailure != null) result.addProperty(
				"termination_failure", execution.terminationFailure);
			result.addProperty("paused", execution.status == Status.BLOCKED_BY_DIALOG);
			result.addProperty("completed",
				isTerminal(execution.status) && (execution.status != Status.TIMED_OUT ||
					execution.executionTerminated));
			result.addProperty("script_id", execution.scriptID.toString());
			result.addProperty("script_name", execution.scriptName == null ? "" :
				execution.scriptName);
			result.addProperty("started_at", execution.startedAt);
			if (execution.finishedAt > 0) result.addProperty("finished_at",
				execution.finishedAt);

			final TextLogs logs = execution.logs == null ? new TextLogs("", "") :
				execution.logs;
			result.addProperty("output", logs.getOutput());
			result.addProperty("errors", logs.getErrors());
			if (execution.primaryError != null && !execution.primaryError.isBlank()) {
				result.addProperty("primary_error", execution.primaryError);
			}
			if (execution.errorDialog != null && !execution.errorDialog.isBlank()) {
				result.addProperty("error_dialog", execution.errorDialog);
			}
			final EnvironmentImpact environment = execution.environment;
			result.addProperty("imagej_log", environment == null ? "" : environment
				.getImageJLog());
			result.addProperty("scijava_log", environment == null ? "" : environment
				.getSciJavaLog());

			final JsonArray dialogs = new JsonArray();
			final List<AWTDialogUtils.DialogInfo> blockingDialogs = environment == null ?
				Collections.emptyList() : environment.getNewModalDialogs();
			for (final AWTDialogUtils.DialogInfo dialog : blockingDialogs) {
				final JsonObject dialogJson = new JsonObject();
				dialogJson.addProperty("title", dialog.getTitle());
				dialogJson.addProperty("class_name", dialog.getClassName());
				dialogJson.addProperty("visible", dialog.isVisible());
				dialogJson.addProperty("active", dialog.isActive());
				dialogJson.addProperty("modal", dialog.isModal());
				dialogJson.addProperty("modality_type", dialog.getModalityType());
				dialogJson.addProperty("owner_name", dialog.getOwnerName());
				final JsonArray messages = new JsonArray();
				for (final String message : dialog.getMessages()) messages.add(message);
				dialogJson.add("messages", messages);
				final JsonArray buttons = new JsonArray();
				for (final AWTDialogUtils.ButtonInfo button : dialog.getButtons()) {
					final JsonObject buttonJson = new JsonObject();
					buttonJson.addProperty("text", button.getText());
					buttonJson.addProperty("action_command", button.getActionCommand());
					buttonJson.addProperty("enabled", button.isEnabled());
					buttonJson.addProperty("visible", button.isVisible());
					buttons.add(buttonJson);
				}
				dialogJson.add("buttons", buttons);
				dialogs.add(dialogJson);
			}
			result.add("dialogs", dialogs);
			if (environment != null) result.add("environment", environment.toJson());
			if (execution.diagnostic != null) result.addProperty("diagnostic",
				execution.diagnostic);
			if (execution.status == Status.BLOCKED_BY_DIALOG) result.addProperty(
				"recommended_action",
				"Inspect the dialog with fiji_ui_dialogs_read, then use " +
					"fiji_ui_dialog_respond with its exact title and button text. " +
					"This run remains active.");
			if (execution.status == Status.TIMED_OUT) {
				result.addProperty("recommended_action",
					execution.executionTerminated ?
						"Ask the user to run this script manually in the Fiji Script Editor; execution is limited to 30 seconds" :
						"Timeout was requested, but the Script Editor reported the execution did not terminate. Inspect the script and logs before retrying.");
			}
			return result;
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
		private volatile ExecutionEnvironmentSnapshotService.EnvironmentCapture environmentCapture;
		private volatile EnvironmentImpact environment;
		private volatile TextEditor textEditor;
		private volatile TextEditorTab tab;
		private volatile Executer executer;
		private volatile Thread monitorThread;
		private volatile boolean timeoutRequested;
		private volatile boolean executionTerminated;
		private volatile String terminationFailure;
		private volatile String primaryError;
		private volatile String errorDialog;

		private Execution(final String runID, final ScriptID scriptID,
			final RunKind kind)
		{
			this.runID = runID;
			this.scriptID = scriptID;
			this.kind = kind;
		}

		private String getTerminationStatus() {
			if (status == Status.TIMED_OUT) {
				if (timeoutRequested && !executionTerminated) return "failed_to_terminate";
				if (timeoutRequested) return "terminated";
				return "timeout_requested";
			}
			return executionTerminated ? "terminated" : "not_terminated";
		}

		private ExecutionResult snapshot() {
			return new ExecutionResult(this);
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
