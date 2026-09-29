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

import com.google.gson.JsonObject;

import net.imagej.ImageJService;

/** Service for running scripts through the SciJava Script Editor. */
public interface ScriptExecutionService extends ImageJService {

	long DEFAULT_WAIT_MS = 30_000;

	enum RunKind {
		SCRIPT, MACRO
	}

	enum Status {
		RUNNING("running"),
		SUCCESS("success"),
		FINISHED_WITH_WARNINGS("finished_with_warnings"),
		FINISHED_WITH_ERRORS("finished_with_errors"),
		BLOCKED_BY_DIALOG("blocked_by_dialog"),
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

	/** Starts a run and waits for completion, a blocking dialog, or the wait limit. */
	ExecutionResult run(ScriptID scriptID, RunKind kind, boolean returnWhenBlocked);

	/** Returns the current state of a previously started run. */
	ExecutionResult status(String runID, RunKind expectedKind);

	static boolean isMacroScript(final String scriptName) {
		return scriptName != null && scriptName.toLowerCase().endsWith(".ijm");
	}

	interface ExecutionResult {

		JsonObject toJson();
	}
}
