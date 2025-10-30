package sc.fiji.llm.provider;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

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
	public List<String> getAvailableModels(final String apiKey) {
		// Anthropic doesn't provide an API endpoint to list models
		// Fall back to hard-coded list
		return Arrays.asList(
			"claude-3-5-sonnet-20241022",
			"claude-3-5-haiku-20241022",
			"claude-3-opus-20240229",
			"claude-3-sonnet-20240229",
			"claude-3-haiku-20240307"
		);
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://docs.anthropic.com/en/docs/about-claude/models";
	}

	@Override
	public String getApiKeyUrl() {
		return "https://console.anthropic.com/settings/keys";
	}

	@Override
	public ChatModel createChatModel(final String apiKey, final String modelName) {
		return AnthropicChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		return AnthropicStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}
}
