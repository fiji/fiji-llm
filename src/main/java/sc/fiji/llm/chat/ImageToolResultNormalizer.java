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

package sc.fiji.llm.chat;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

/**
 * Adapts image-bearing tool results for transports that only support images in
 * user messages.
 */
public final class ImageToolResultNormalizer {

	private static final String IMAGE_RESULT_TEXT = "Image content returned by tool.";

	private ImageToolResultNormalizer() {}

	/**
	 * Keeps tool-result text in its tool message and moves image content into an
	 * adjacent user message for the outbound request.
	 *
	 * @param chatRequest the request to adapt
	 * @return the original request when no image tool result is present, otherwise
	 *         an adapted request
	 */
	public static ChatRequest normalize(final ChatRequest chatRequest) {
		final List<ChatMessage> messages = new ArrayList<>();
		final List<Content> pendingImages = new ArrayList<>();
		boolean changed = false;

		for (ChatMessage message : chatRequest.messages()) {
			if (message instanceof ToolExecutionResultMessage) {
				final ToolExecutionResultMessage result =
					(ToolExecutionResultMessage) message;
				final List<Content> images = result.contents().stream().filter(
					ImageContent.class::isInstance).toList();
				if (!images.isEmpty()) {
					final List<Content> textContents = result.contents().stream().filter(
						TextContent.class::isInstance).toList();
					final ToolExecutionResultMessage.Builder normalizedResult =
						ToolExecutionResultMessage.builder().id(result.id()).toolName(
							result.toolName());
					if (textContents.isEmpty()) {
						normalizedResult.text(IMAGE_RESULT_TEXT);
					}
					else {
						normalizedResult.contents(textContents);
					}
					messages.add(normalizedResult.build());
					pendingImages.addAll(images);
					changed = true;
					continue;
				}
			}

			appendPendingImages(messages, pendingImages);
			messages.add(message);
		}
		appendPendingImages(messages, pendingImages);

		return changed ? chatRequest.toBuilder().messages(messages).build() :
			chatRequest;
	}

	private static void appendPendingImages(final List<ChatMessage> messages,
		final List<Content> pendingImages)
	{
		if (pendingImages.isEmpty()) return;
		final UserMessage.Builder userMessage = UserMessage.builder();
		userMessage.addContent(TextContent.from(IMAGE_RESULT_TEXT));
		pendingImages.forEach(userMessage::addContent);
		messages.add(userMessage.build());
		pendingImages.clear();
	}
}
