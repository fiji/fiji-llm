package sc.fiji.llm.provider;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * LLM provider plugin for Anthropic (Claude).
 */
@Plugin(type = LLMProviderPlugin.class, name = "Anthropic")
public class AnthropicProviderPlugin implements LLMProviderPlugin {

	@Override
	public String getName() {
		return "Anthropic";
	}

	@Override
	public String getDescription() {
		return "Anthropic's Claude models";
	}

	@Override
	public List<String> getAvailableModels() {
		return Arrays.asList(
			"claude-3-5-sonnet-20241022",
			"claude-3-5-haiku-20241022",
			"claude-3-opus-20240229",
			"claude-3-sonnet-20240229",
			"claude-3-haiku-20240307"
		);
	}

	@Override
	public String getApiKeyUrl() {
		return "https://console.anthropic.com/settings/keys";
	}

	@Override
	public ChatLanguageModel createChatModel(final String apiKey, final String modelName) {
		return AnthropicChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}

	@Override
	public StreamingChatLanguageModel createStreamingChatModel(final String apiKey, final String modelName) {
		return AnthropicStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}
}
