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

import java.util.List;
import java.util.Locale;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;

/** Formats tool results for logs and activity records without assuming text-only content. */
public final class ToolResultFormatter {

	private ToolResultFormatter() {}

	public static String format(final List<Content> contents) {
		if (contents == null || contents.isEmpty()) return "";

		final StringBuilder result = new StringBuilder();
		for (final Content content : contents) {
			if (content instanceof TextContent textContent) {
				appendPart(result, textContent.text());
			}
			else if (content != null) {
				final String type = content.type() == null ? content.getClass()
					.getSimpleName() : content.type().name().toLowerCase(Locale.ROOT);
				appendPart(result, "[" + type + " content]");
			}
		}
		return result.toString();
	}

	private static void appendPart(final StringBuilder result, final String part) {
		if (part == null || part.isEmpty()) return;
		if (result.length() > 0) result.append('\n');
		result.append(part);
	}
}
