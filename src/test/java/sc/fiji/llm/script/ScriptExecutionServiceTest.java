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

public class ScriptExecutionServiceTest {

	@Test
	public void testTimedOutResultReportsTimeoutRequestedAndTerminationFailure() throws Exception {
		final ScriptExecutionService.ExecutionResult result = createExecutionResult(
			ScriptExecutionService.Status.TIMED_OUT, true, false,
			"UnsupportedOperationException: Thread.stop", null, null);
		final JsonObject json = result.toJson();
		assertEquals("timed_out", json.get("status").getAsString());
		assertTrue(json.get("timeout_requested").getAsBoolean());
		assertFalse(json.get("execution_terminated").getAsBoolean());
		assertEquals("failed_to_terminate", json.get("termination_status").getAsString());
		assertEquals("UnsupportedOperationException: Thread.stop",
			json.get("termination_failure").getAsString());
		assertFalse(json.get("completed").getAsBoolean());
	}

	@Test
	public void testResultPreservesPrimaryErrorAndErrorDialog() throws Exception {
		final ScriptExecutionService.ExecutionResult result = createExecutionResult(
			ScriptExecutionService.Status.FINISHED_WITH_ERRORS, false, true, null,
			"Type mismatch in macro call", "Macro Error: Number expected");
		final JsonObject json = result.toJson();
		assertEquals("Type mismatch in macro call", json.get("primary_error")
			.getAsString());
		assertEquals("Macro Error: Number expected", json.get("error_dialog")
			.getAsString());
	}

	private static ScriptExecutionService.ExecutionResult createExecutionResult(
		final ScriptExecutionService.Status status,
		final boolean timeoutRequested,
		final boolean executionTerminated,
		final String terminationFailure,
		final String primaryError,
		final String errorDialog) throws Exception
	{
		final Constructor<?> executionCtor = Class.forName(
			"sc.fiji.llm.script.ScriptExecutionService$Execution")
			.getDeclaredConstructor(String.class, ScriptID.class,
				ScriptExecutionService.RunKind.class);
		executionCtor.setAccessible(true);
		final Object execution = executionCtor.newInstance("run-id", new ScriptID(0, 0),
			ScriptExecutionService.RunKind.SCRIPT);

		final Field statusField = execution.getClass().getDeclaredField("status");
		statusField.setAccessible(true);
		statusField.set(execution, status);

		final Field timeoutRequestedField = execution.getClass()
			.getDeclaredField("timeoutRequested");
		timeoutRequestedField.setAccessible(true);
		timeoutRequestedField.set(execution, timeoutRequested);

		final Field executionTerminatedField = execution.getClass()
			.getDeclaredField("executionTerminated");
		executionTerminatedField.setAccessible(true);
		executionTerminatedField.set(execution, executionTerminated);

		final Field terminationFailureField = execution.getClass()
			.getDeclaredField("terminationFailure");
		terminationFailureField.setAccessible(true);
		terminationFailureField.set(execution, terminationFailure);

		setOptionalField(execution, "primaryError", primaryError);
		setOptionalField(execution, "errorDialog", errorDialog);

		final Method snapshot = execution.getClass().getDeclaredMethod("snapshot");
		snapshot.setAccessible(true);
		return (ScriptExecutionService.ExecutionResult) snapshot.invoke(execution);
	}

	private static void setOptionalField(final Object execution,
		final String fieldName, final String value) throws Exception
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
