package sc.fiji.llm.provider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
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
	public List<String> getAvailableModels() {
		// Use the models from langchain4j's AnthropicChatModelName enum
		// Filter to show only the main/latest models to avoid overwhelming users
		return Stream.of(AnthropicChatModelName.values())
			.map(AnthropicChatModelName::toString)
			.collect(Collectors.toList());
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
			.maxRetries(DEFAULT_MAX_RETRIES)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		return AnthropicStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}
}
