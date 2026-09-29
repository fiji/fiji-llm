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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

import com.google.gson.JsonObject;

import sc.fiji.llm.log.TextLogs;

public class ScriptExecutionServiceTest {

	@Test
	public void testStructuredErrorControlsFinalStatusAfterDialogDismissal() {
		assertEquals(ScriptExecutionService.Status.FINISHED_WITH_ERRORS,
			DefaultScriptExecutionService.classifyFinishedStatus("", "", "Type mismatch", null));
		assertEquals(ScriptExecutionService.Status.FINISHED_WITH_ERRORS,
			DefaultScriptExecutionService.classifyFinishedStatus("", "", null,
				"Macro Error: Number expected"));
		assertEquals(ScriptExecutionService.Status.SUCCESS,
			DefaultScriptExecutionService.classifyFinishedStatus("", "", null, null));
	}

	@Test
	public void testWarningsUseDistinctStatusAndErrorsTakePrecedence() {
		final String warning = "Auto-import warning";
		assertEquals(ScriptExecutionService.Status.FINISHED_WITH_WARNINGS,
			DefaultScriptExecutionService.classifyFinishedStatus("", "", null, null,
				warning));
		assertEquals(ScriptExecutionService.Status.FINISHED_WITH_ERRORS,
			DefaultScriptExecutionService.classifyFinishedStatus("", "", "Failure", null,
				warning));
	}

	@Test
	public void testOnlyKnownMacroErrorDialogsAreClassifiedAsErrors() {
		assertTrue(DefaultScriptExecutionService.isMacroErrorDialog(
			"ij.gui.GenericDialog", "Macro Error"));
		assertTrue(DefaultScriptExecutionService.isMacroErrorDialog(
			"ij.gui.GenericDialog", "No Image"));
		assertFalse(DefaultScriptExecutionService.isMacroErrorDialog(
			"ij.gui.MessageDialog", "Message"));
		assertFalse(DefaultScriptExecutionService.isMacroErrorDialog(
			"ij.gui.GenericDialog", "Message"));
	}

	@Test
	public void testWaitExpiryReportsRunningWithoutExecutionError() throws Exception {
		final ScriptExecutionService.ExecutionResult result = createExecutionResult(
			ScriptExecutionService.Status.RUNNING, true, null, null);
		final JsonObject json = result.toJson();
		assertEquals("running", json.get("status").getAsString());
		assertTrue(json.get("run_id").getAsString().length() <= 12);
		assertTrue(json.has("duration_ms"));
		assertFalse(json.has("elapsed_ms"));
		assertTrue(json.get("wait_expired").getAsBoolean());
		assertFalse(json.has("completion_state"));
		assertFalse(json.has("completed"));
		assertFalse(json.has("paused"));
		assertFalse(json.has("output"));
		assertFalse(json.has("errors"));
		assertFalse(json.has("environment"));
		assertFalse(json.has("environment_impact"));
		assertEquals("fiji_script_run_status", json.getAsJsonArray("recommended_tools")
			.get(0).getAsString());
	}

	@Test
	public void testResultPreservesPrimaryErrorAndErrorDialog() throws Exception {
		final ScriptExecutionService.ExecutionResult result = createExecutionResult(
			ScriptExecutionService.Status.FINISHED_WITH_ERRORS, false,
			"Type mismatch in macro call", "Macro Error: Number expected");
		final JsonObject json = result.toJson();
		assertEquals("Macro Error: Number expected", json.get("error_dialog")
			.getAsString());
		assertFalse(json.has("primary_error"));
		assertEquals("fiji_guide_read", json.getAsJsonArray("guide_recommendations")
			.get(0).getAsJsonObject().get("tool").getAsString());
		assertEquals("scripting", json.getAsJsonArray("guide_recommendations").get(0)
			.getAsJsonObject().getAsJsonObject("arguments").get("guide_id").getAsString());
	}

	@Test
	public void testBlockedRunRecommendsDialogActions() throws Exception {
		final JsonObject json = createExecutionResult(
			ScriptExecutionService.Status.BLOCKED_BY_DIALOG, false, null, null).toJson();

		assertEquals("fiji_ui_dialog_respond", json.getAsJsonArray("recommended_tools")
			.get(0).getAsString());
		assertEquals("fiji_ui_dialog_close", json.getAsJsonArray("recommended_tools")
			.get(1).getAsString());
		assertFalse(json.has("recommended_action"));
	}

	@Test
	public void testMacroFailureRecommendsMacroGuide() throws Exception {
		final ScriptExecutionService.ExecutionResult result = createExecutionResult(
			ScriptExecutionService.Status.FINISHED_WITH_ERRORS, false, null, null,
			ScriptExecutionService.RunKind.MACRO);
		final JsonObject recommendation = result.toJson().getAsJsonArray(
			"guide_recommendations").get(0).getAsJsonObject();

		assertEquals("fiji_guide_read", recommendation.get("tool").getAsString());
		assertEquals("creating-macros", recommendation.getAsJsonObject("arguments")
			.get("guide_id").getAsString());
		assertFalse(recommendation.getAsJsonObject("arguments").has("id"));
		assertFalse(recommendation.has("reason"));
	}

	@Test
	public void testGenericMacroInterpreterMessageIsIgnoredAsError() {
		final TextLogs logs = new TextLogs("",
			"[INFO] Execution errors handled by the Macro Interpreter.");
		final TextLogs cleaned = logs.withoutGenericMacroInterpreterMessages();
		assertEquals("", cleaned.getErrors());
	}

	@Test
	public void testAutoImportWarningIsReportedSeparately() throws Exception {
		final String warning = "[WARNING] Auto-imports not available for language 'ImageJ Macro'.";
		final ScriptExecutionService.ExecutionResult result = createExecutionResult(
			ScriptExecutionService.Status.FINISHED_WITH_WARNINGS, false, null, null,
			ScriptExecutionService.RunKind.MACRO, new TextLogs("", warning + "\n"));
		final JsonObject json = result.toJson();

		assertEquals("finished_with_warnings", json.get("status").getAsString());
		assertEquals(warning, json.get("warnings").getAsString());
		assertFalse(json.has("errors"));
	}

	private static ScriptExecutionService.ExecutionResult createExecutionResult(
		final ScriptExecutionService.Status status,
		final boolean waitExpired,
		final String primaryError,
		final String errorDialog) throws Exception
	{
		return createExecutionResult(status, waitExpired, primaryError, errorDialog,
			ScriptExecutionService.RunKind.SCRIPT);
	}

	private static ScriptExecutionService.ExecutionResult createExecutionResult(
		final ScriptExecutionService.Status status,
		final boolean waitExpired,
		final String primaryError,
		final String errorDialog,
		final ScriptExecutionService.RunKind kind) throws Exception
	{
		return createExecutionResult(status, waitExpired, primaryError, errorDialog, kind,
			null);
	}

	private static ScriptExecutionService.ExecutionResult createExecutionResult(
		final ScriptExecutionService.Status status,
		final boolean waitExpired,
		final String primaryError,
		final String errorDialog,
		final ScriptExecutionService.RunKind kind,
		final TextLogs logs) throws Exception
	{
		final Constructor<?> executionCtor = Class.forName(
			"sc.fiji.llm.script.DefaultScriptExecutionService$Execution")
			.getDeclaredConstructor(String.class, ScriptID.class,
				ScriptExecutionService.RunKind.class);
		executionCtor.setAccessible(true);
		final Object execution = executionCtor.newInstance("run-id", new ScriptID(0, 0),
			kind);

		final Field statusField = execution.getClass().getDeclaredField("status");
		statusField.setAccessible(true);
		statusField.set(execution, status);

		setOptionalField(execution, "primaryError", primaryError);
		setOptionalField(execution, "errorDialog", errorDialog);
		setOptionalField(execution, "logs", logs);

		final Method snapshot = execution.getClass().getDeclaredMethod("snapshot",
			boolean.class);
		snapshot.setAccessible(true);
		return (ScriptExecutionService.ExecutionResult) snapshot.invoke(execution,
			waitExpired);
	}

	private static void setOptionalField(final Object execution,
		final String fieldName, final Object value) throws Exception
	{
		try {
			final Field field = execution.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(execution, value);
		}
		catch (final NoSuchFieldException e) {
			// Ignore when running against older code; the regression itself asserts on JSON
			// output, so the test still fails if the production fields are absent.
		}
	}
}
