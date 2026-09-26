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

package sc.fiji.llm.log;

/** Utility methods shared by log snapshot implementations. */
public final class LogUtils {

	private LogUtils() {
		// utility class
	}

	/**
	 * Returns the newly appended portion of {@code current}, when it extends
	 * {@code initial}. If the log was cleared or otherwise changed, the current
	 * contents are returned instead.
	 */
	public static String delta(final String initial, final String current) {
		final String initialText = initial == null ? "" : initial;
		final String currentText = current == null ? "" : current;
		return currentText.startsWith(initialText) ? currentText.substring(initialText
			.length()) : currentText;
	}

	/**
	 * Shortens text for a single log line, collapsing whitespace and noting how
	 * many characters were omitted.
	 */
	public static String abbreviate(final String text, final int maxLength) {
		if (text == null) return "";
		final String oneLine = text.replaceAll("\\s+", " ").trim();
		if (oneLine.length() <= maxLength) return oneLine;
		return oneLine.substring(0, maxLength) + "... (" + (oneLine.length() -
			maxLength) + " more chars)";
	}
}
