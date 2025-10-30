package sc.fiji.llm.provider;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
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
		return Arrays.asList(
			"gpt-4o",
			"gpt-4o-mini",
			"gpt-4-turbo",
			"gpt-4",
			"gpt-3.5-turbo"
		);
	}

	@Override
	public String getApiKeyUrl() {
		return "https://platform.openai.com/api-keys";
	}

	@Override
	public ChatLanguageModel createChatModel(final String apiKey, final String modelName) {
		return OpenAiChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}

	@Override
	public StreamingChatLanguageModel createStreamingChatModel(final String apiKey, final String modelName) {
		return OpenAiStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}
}
