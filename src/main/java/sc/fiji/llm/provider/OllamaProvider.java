package sc.fiji.llm.provider;

import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

/**
 * LLM provider plugin for Ollama (locally run models).
 */
@Plugin(type = LLMProvider.class, name = "Ollama")
public class OllamaProvider implements LLMProvider {

	private static final String DEFAULT_BASE_URL = "http://localhost:11434";

	@Override
	public String getName() {
		return "Ollama";
	}

	@Override
	public String getDescription() {
		return "Locally run models via Ollama";
	}

	@Override
	public List<String> getAvailableModels() {
		// Ollama doesn't provide a static list - models depend on what the user has pulled
		// Provide a list of popular/recommended models
        // FIXME: Some models don't allow tools - do we want to support those?
		return Arrays.asList(
			"deepseek-r1:8b",
            "qwen3:4b"
		);
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://ollama.com/library";
	}

	@Override
	public String getApiKeyUrl() {
		// Ollama doesn't require an API key for local usage
		// Return the download page instead
		return "https://ollama.com/download";
	}

	@Override
	public ChatModel createChatModel(final String apiKey, final String modelName) {
		// For Ollama, the "apiKey" parameter is repurposed as the base URL
		// If empty or null, use default localhost URL
		String baseUrl = (apiKey == null || apiKey.trim().isEmpty()) ? DEFAULT_BASE_URL : apiKey;
		
		return OllamaChatModel.builder()
			.baseUrl(DEFAULT_BASE_URL)
			.modelName(modelName)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		// For Ollama, the "apiKey" parameter is repurposed as the base URL
		// If empty or null, use default localhost URL
		String baseUrl = (apiKey == null || apiKey.trim().isEmpty()) ? DEFAULT_BASE_URL : apiKey;
		
		return OllamaStreamingChatModel.builder()
			.baseUrl(DEFAULT_BASE_URL)
			.modelName(modelName)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}
}
