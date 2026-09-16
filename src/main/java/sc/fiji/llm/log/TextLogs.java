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

package sc.fiji.llm.log;

/** A pair of text channels captured from an execution surface. */
public final class TextLogs {

	private final String output;
	private final String errors;

	public TextLogs(final String output, final String errors) {
		this.output = output == null ? "" : output;
		this.errors = errors == null ? "" : errors;
	}

	public String getOutput() {
		return output;
	}

	public String getErrors() {
		return errors;
	}

	public TextLogs deltaFrom(final TextLogs initial) {
		return new TextLogs(LogUtils.delta(initial == null ? "" : initial.output,
			output), LogUtils.delta(initial == null ? "" : initial.errors, errors));
	}

	public TextLogs withoutStartedBanners() {
		return withoutKnownNoise();
	}

	public TextLogs withoutGenericMacroInterpreterMessages() {
		return withoutKnownNoise();
	}

	private TextLogs withoutKnownNoise() {
		return new TextLogs(stripKnownNoise(output), stripKnownNoise(errors));
	}

	private static String stripKnownNoise(final String logs) {
		String cleaned = stripStartedBanners(logs);
		return stripGenericMacroInterpreterMessages(cleaned);
	}

	private static String stripStartedBanners(final String logs) {
		return logs.replaceAll("(?m)^Started .* at .*\\r?\\n?", "");
	}

	private static String stripGenericMacroInterpreterMessages(final String logs) {
		if (logs == null || logs.isBlank()) return logs == null ? "" : logs;
		return logs.replaceAll("(?im)^[^\\r\\n]*execution errors handled by the macro " +
			"interpreter[^\\r\\n]*(?:\\r?\\n|$)", "");
	}
}