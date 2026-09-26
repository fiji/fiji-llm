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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

public class ImageToolResultNormalizerTest {

	@Test
	public void testMovesImagesOutOfToolResult() {
		final ImageContent image = ImageContent.from("AQID", "image/png");
		final ToolExecutionResultMessage result = ToolExecutionResultMessage
			.builder().id("result-1").toolName("fiji_image_view").contents(List.of(
				TextContent.from("image rendered"), image)).build();
		final ChatRequest request = ChatRequest.builder().messages(List.of(result))
			.build();

		final ChatRequest normalized = ImageToolResultNormalizer.normalize(request);

		assertEquals(2, normalized.messages().size());
		final ToolExecutionResultMessage normalizedResult =
			(ToolExecutionResultMessage) normalized.messages().get(0);
		assertEquals("image rendered", normalizedResult.text());
		assertFalse(normalizedResult.contents().stream().anyMatch(
			ImageContent.class::isInstance));
		final UserMessage imageMessage = (UserMessage) normalized.messages().get(1);
		assertEquals(2, imageMessage.contents().size());
		assertTrue(imageMessage.contents().get(0) instanceof TextContent);
		assertTrue(imageMessage.contents().get(1) instanceof ImageContent);
	}

	@Test
	public void testMovesImageOnlyToolResult() {
		final ToolExecutionResultMessage result = ToolExecutionResultMessage.builder()
			.id("result-1").toolName("fiji_image_view").contents(List.of(
				ImageContent.from("AQID", "image/png"))).build();
		final ChatRequest request = ChatRequest.builder().messages(List.of(result))
			.build();

		final ChatRequest normalized = ImageToolResultNormalizer.normalize(request);

		assertEquals(2, normalized.messages().size());
		final ToolExecutionResultMessage normalizedResult =
			(ToolExecutionResultMessage) normalized.messages().get(0);
		assertEquals("Image content returned by tool.", normalizedResult.text());
		final UserMessage imageMessage = (UserMessage) normalized.messages().get(1);
		assertEquals(2, imageMessage.contents().size());
		assertTrue(imageMessage.contents().get(0) instanceof TextContent);
		assertTrue(imageMessage.contents().get(1) instanceof ImageContent);
	}

	@Test
	public void testLeavesRequestsWithoutImageToolResultsUnchanged() {
		final ToolExecutionResultMessage result = ToolExecutionResultMessage.from(
			"result-1", "fiji_image_details", "metadata");
		final ChatRequest request = ChatRequest.builder().messages(List.of(result))
			.build();

		assertSame(request, ImageToolResultNormalizer.normalize(request));
	}
}
