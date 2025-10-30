package sc.fiji.llm.provider;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

/**
 * LLM provider plugin for Google AI (Gemini).
 */
@Plugin(type = LLMProviderPlugin.class, name = "Google")
public class GeminiProviderPlugin implements LLMProviderPlugin {

	@Override
	public String getName() {
		return "Google";
	}

	@Override
	public String getDescription() {
		return "Google's Gemini models";
	}

	@Override
	public List<String> getAvailableModels() {
		// Google AI doesn't provide a public API endpoint to list models
		// Fall back to hard-coded list
		return Arrays.asList(
			"gemini-2.5-pro",
			"gemini-2.5-flash",
			"gemini-2.5-flash-lite",
			"gemini-2.0-flash",
			"gemini-2.0-flash-lite"
		);
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://ai.google.dev/gemini-api/docs/models";
	}

	@Override
	public String getApiKeyUrl() {
		return "https://aistudio.google.com/app/apikey";
	}

	@Override
	public ChatModel createChatModel(final String apiKey, final String modelName) {
		return GoogleAiGeminiChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		return GoogleAiGeminiStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}
}
