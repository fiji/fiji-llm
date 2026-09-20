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

package sc.fiji.llm.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class ProviderVisionSupportTest {

	@Test
	public void testOpenAiVisionSupport() {
		final OpenAIProvider provider = new OpenAIProvider();
		assertEquals(LLMProvider.VisionSupport.SUPPORTED, provider
			.getVisionSupport("gpt-4o"));
		assertEquals(LLMProvider.VisionSupport.UNSUPPORTED, provider
			.getVisionSupport("gpt-4"));
		assertEquals(LLMProvider.VisionSupport.UNKNOWN, provider
			.getVisionSupport("future-model"));
	}

	@Test
	public void testHostedVisionSupport() {
		final GeminiProvider gemini = new GeminiProvider();
		assertEquals(LLMProvider.VisionSupport.SUPPORTED, gemini
			.getVisionSupport("gemini-2.5-flash"));

		final AnthropicProvider anthropic = new AnthropicProvider();
		assertEquals(LLMProvider.VisionSupport.SUPPORTED, anthropic
			.getVisionSupport(anthropic.getAvailableModels().get(0)));
		assertEquals(LLMProvider.VisionSupport.UNKNOWN, anthropic
			.getVisionSupport("future-model"));
	}

	@Test
	public void testOllamaCapabilitiesParser() {
		final Optional<Set<String>> capabilities = OllamaProcessManager
			.parseModelCapabilities("{\"capabilities\":[\"completion\",\"vision\"]}");
		assertTrue(capabilities.isPresent());
		assertTrue(capabilities.get().contains("vision"));
		assertEquals(Optional.empty(), OllamaProcessManager.parseModelCapabilities(
			"{\"model\":\"text-only\"}"));
	}

	@Test
	public void testOllamaTokenEstimatorSupportsImageContent() {
		final UserMessage textOnlyMessage = UserMessage.builder().addContent(
			new TextContent("Describe this image")).build();
		final UserMessage message = UserMessage.builder().addContent(new TextContent(
			"Describe this image")).addContent(ImageContent.from("AQID",
				"image/png")).build();

		final int tokenCount = new AbstractOllamaProvider.OllamaTokenCountEstimator()
			.estimateTokenCountInMessage(message);
		final int textOnlyTokenCount = new AbstractOllamaProvider.OllamaTokenCountEstimator()
			.estimateTokenCountInMessage(textOnlyMessage);

		assertTrue(tokenCount > textOnlyTokenCount);

		final ToolExecutionResultMessage toolResult = ToolExecutionResultMessage
			.builder().id("result-1").toolName("fiji_image_view").contents(List.of(
				new TextContent("image rendered"), ImageContent.from("AQID",
					"image/png"))).build();
		final int toolResultTokenCount = new AbstractOllamaProvider
			.OllamaTokenCountEstimator().estimateTokenCountInMessage(toolResult);
		assertTrue(toolResultTokenCount > 0);
	}
}
