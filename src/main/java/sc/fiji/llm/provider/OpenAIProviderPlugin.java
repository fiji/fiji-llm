package sc.fiji.llm.provider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * LLM provider plugin for OpenAI (ChatGPT).
 */
@Plugin(type = LLMProviderPlugin.class, name = "OpenAI")
public class OpenAIProviderPlugin implements LLMProviderPlugin {

	@Override
	public String getName() {
		return "OpenAI";
	}

	@Override
	public String getDescription() {
		return "OpenAI's GPT models (ChatGPT)";
	}

	@Override
	public List<String> getAvailableModels() {
		// Use the models from langchain4j's OpenAiChatModelName enum
		// Filter to show only the main/latest models to avoid overwhelming users
		return Stream.of(OpenAiChatModelName.values())
			.map(OpenAiChatModelName::toString)
			.filter(this::isMainChatModel)
			.collect(Collectors.toList());
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://platform.openai.com/docs/models/";
	}

	/**
	 * Check if a model ID represents a main chat model we want to show.
	 * Filters out preview models and dated versions, keeping only the main model names.
	 */
	private boolean isMainChatModel(final String modelId) {
		// Exclude preview models
		if (modelId.contains("preview")) {
			return false;
		}
		// Exclude dated versions (models with dates like YYYY-MM-DD or YYYY_MM_DD)
		return !modelId.matches(".*\\d{4}[_-]\\d{2}[_-]\\d{2}.*");
	}

	@Override
	public String getApiKeyUrl() {
		return "https://platform.openai.com/api-keys";
	}

	@Override
	public ChatModel createChatModel(final String apiKey, final String modelName) {
		return OpenAiChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.maxRetries(DEFAULT_MAX_RETRIES)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		return OpenAiStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}
}
