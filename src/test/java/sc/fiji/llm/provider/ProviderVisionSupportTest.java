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
import dev.langchain4j.model.openai.OpenAiResponsesChatRequestParameters;

public class ProviderVisionSupportTest {

	@Test
	public void testOpenAiVisionSupport() {
		final OpenAIProvider provider = new OpenAIProvider();
		assertEquals(List.of("Luna 5.6", "Luna 6", "Sol 6"), provider
			.getAvailableModels());
		for (final String model : provider.getAvailableModels()) {
			assertEquals(model, LLMProvider.VisionSupport.SUPPORTED, provider
				.getVisionSupport(model));
		}
		assertEquals(LLMProvider.VisionSupport.UNKNOWN, provider
			.getVisionSupport("future-model"));
	}

	@Test
	public void testOpenAiReasoningEffortByModel() {
		final OpenAIProvider provider = new OpenAIProvider();
		final OpenAiResponsesChatRequestParameters lunaParameters =
			(OpenAiResponsesChatRequestParameters) provider
				.defaultChatRequestParameters(
				"Luna 6");
		final OpenAiResponsesChatRequestParameters solParameters =
			(OpenAiResponsesChatRequestParameters) provider
				.defaultChatRequestParameters("Sol 6");

		assertEquals("none", lunaParameters.reasoningEffort());
		assertEquals("medium", solParameters.reasoningEffort());
	}

	@Test
	public void testOpenAiProviderModelIdUsesSolReasoningEffort() {
		final OpenAiResponsesChatRequestParameters parameters =
			(OpenAiResponsesChatRequestParameters) new OpenAIProvider()
				.defaultChatRequestParameters("gpt-6.1-sol");
		assertEquals("medium", parameters.reasoningEffort());
	}

	@Test
	public void testHostedProviderCosts() {
		final OpenAIProvider openAI = new OpenAIProvider();
		assertEquals(new LLMProvider.ModelCost(0.20, 1.20), openAI.getCost(
			"Luna 5.6").get());
		assertEquals(new LLMProvider.ModelCost(2.00, 10.00), openAI.getCost(
			"Sol 6").get());
		assertEquals(Optional.empty(), openAI.getCost("future-model"));

		final AnthropicProvider anthropic = new AnthropicProvider();
		assertEquals(new LLMProvider.ModelCost(1.00, 5.00), anthropic.getCost(
			"Haiku").get());
		assertEquals(new LLMProvider.ModelCost(4.00, 20.00), anthropic.getCost(
			"Opus").get());
		assertEquals(Optional.empty(), anthropic.getCost("future-model"));
		assertEquals(Optional.empty(), anthropic.getCost(null));

		assertEquals(Optional.empty(), new GeminiProvider().getCost("Gemini"));
	}

	@Test
	public void testHostedProviderMemoryLimits() {
		assertEquals(691_500, OpenAIProvider.getMemoryTokenLimit());
		assertEquals(786_432, GeminiProvider.getMemoryTokenLimit());
	}

	@Test
	public void testHostedVisionSupport() {
		final GeminiProvider gemini = new GeminiProvider();
		for (final String model : gemini.getAvailableModels()) {
			assertEquals(model, LLMProvider.VisionSupport.SUPPORTED, gemini
				.getVisionSupport(model));
		}
		assertEquals(LLMProvider.VisionSupport.UNKNOWN, gemini
			.getVisionSupport("future-model"));

		final AnthropicProvider anthropic = new AnthropicProvider();
		assertEquals(List.of("Haiku", "Sonnet", "Opus"), anthropic
			.getAvailableModels());
		for (final String model : anthropic.getAvailableModels()) {
			assertEquals(model, LLMProvider.VisionSupport.SUPPORTED, anthropic
				.getVisionSupport(model));
		}
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
	public void testOllamaModelMemoryParser() {
		final String response = "{\"models\":[{" +
			"\"name\":\"llama3:latest\",\"size\":1000,\"size_vram\":750" +
			"}]}";
		final Optional<OllamaProcessManager.ModelMemoryUsage> usage =
			OllamaProcessManager.parseModelMemoryUsage(response, "llama3:latest");

		assertTrue(usage.isPresent());
		assertEquals(1000, usage.get().modelSizeBytes());
		assertEquals(750, usage.get().vramSizeBytes());
		assertEquals(Optional.empty(), OllamaProcessManager.parseModelMemoryUsage(
			response, "missing-model"));
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
