package sc.fiji.llm.provider;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
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
		return Arrays.asList(
			"gemini-2.0-flash-exp",
			"gemini-1.5-pro",
			"gemini-1.5-flash",
			"gemini-1.5-flash-8b"
		);
	}

	@Override
	public String getApiKeyUrl() {
		return "https://aistudio.google.com/app/apikey";
	}

	@Override
	public ChatLanguageModel createChatModel(final String apiKey, final String modelName) {
		return GoogleAiGeminiChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}

	@Override
	public StreamingChatLanguageModel createStreamingChatModel(final String apiKey, final String modelName) {
		return GoogleAiGeminiStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}
}
