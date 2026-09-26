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

package sc.fiji.llm.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What the assistant did while producing one response: its thinking and each
 * tool call. It is displayed alongside the response and saved with the
 * conversation, but never sent to the model. Tool results are truncated, since
 * the record is for display only.
 */
public class ActivityRecord {

	/** Maximum stored length of a tool result. */
	public static final int MAX_RESULT_LENGTH = 600;

	private List<Step> steps = new ArrayList<>();
	private long elapsedSeconds;

	// No-arg constructor for GSON
	public ActivityRecord() {}

	/** Appends streamed thinking text, starting a new step after a tool call. */
	public void appendThinking(final String text) {
		if (text == null || text.isEmpty()) return;
		final Step last = steps.isEmpty() ? null : steps.get(steps.size() - 1);
		if (last != null && last.isThinking()) last.text += text;
		else steps.add(new Step(null, null, text));
	}

	/** Records the start of a tool call. */
	public void toolStarted(final String name, final String arguments) {
		steps.add(new Step(name, arguments, ""));
	}

	/** Records the outcome of the most recent unfinished call to the tool. */
	public void toolFinished(final String name, final boolean failed,
		final long millis, final String result)
	{
		for (int i = steps.size() - 1; i >= 0; i--) {
			final Step step = steps.get(i);
			if (name.equals(step.tool) && !step.isFinished()) {
				step.failed = failed;
				step.millis = millis;
				step.text = truncate(result == null ? "" : result);
				return;
			}
		}
	}

	public List<Step> getSteps() {
		return steps;
	}

	public boolean isEmpty() {
		return steps.isEmpty();
	}

	public long getElapsedSeconds() {
		return elapsedSeconds;
	}

	public void setElapsedSeconds(final long elapsedSeconds) {
		this.elapsedSeconds = elapsedSeconds;
	}

	private static String truncate(final String text) {
		if (text.length() <= MAX_RESULT_LENGTH) return text;
		return text.substring(0, MAX_RESULT_LENGTH) + "\n... (" + (text.length() -
			MAX_RESULT_LENGTH) + " more characters)";
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof ActivityRecord that)) return false;
		return elapsedSeconds == that.elapsedSeconds && Objects.equals(steps,
			that.steps);
	}

	@Override
	public int hashCode() {
		return Objects.hash(steps, elapsedSeconds);
	}

	/** One thinking passage or tool call. */
	public static class Step {

		private String tool;
		private String arguments;
		private String text;
		private boolean failed;
		private Long millis;

		// No-arg constructor for GSON
		public Step() {}

		private Step(final String tool, final String arguments, final String text) {
			this.tool = tool;
			this.arguments = arguments;
			this.text = text;
		}

		public boolean isThinking() {
			return tool == null;
		}

		/** @return the tool name, or null for a thinking step */
		public String getTool() {
			return tool;
		}

		public String getArguments() {
			return arguments;
		}

		/** @return the thinking text, or the (truncated) tool result */
		public String getText() {
			return text;
		}

		public boolean isFailed() {
			return failed;
		}

		/** @return whether a tool call has finished; always true for thinking */
		public boolean isFinished() {
			return isThinking() || millis != null;
		}

		/** @return the tool call duration, or null while running */
		public Long getMillis() {
			return millis;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) return true;
			if (!(o instanceof Step that)) return false;
			return failed == that.failed && Objects.equals(tool, that.tool) && Objects
				.equals(arguments, that.arguments) && Objects.equals(text, that.text) &&
				Objects.equals(millis, that.millis);
		}

		@Override
		public int hashCode() {
			return Objects.hash(tool, arguments, text, failed, millis);
		}
	}
}
