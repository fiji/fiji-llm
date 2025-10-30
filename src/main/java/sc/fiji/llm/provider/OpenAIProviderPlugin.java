package sc.fiji.llm.provider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scijava.plugin.Plugin;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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
	public List<String> getAvailableModels(final String apiKey) {
		// Try to query the OpenAI API for available models
		if (apiKey != null && !apiKey.isEmpty()) {
			try {
				List<String> models = queryAvailableModels(apiKey);
				if (!models.isEmpty()) {
					return models;
				}
			} catch (IOException | InterruptedException e) {
				// Fall through to default list
				System.err.println("Failed to query OpenAI models: " + e.getMessage());
			}
		}
		
		// Fall back to hard-coded list
		return getDefaultModels();
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://platform.openai.com/docs/models/";
	}

	/**
	 * Get the default hard-coded list of models.
	 */
	private List<String> getDefaultModels() {
		return Arrays.asList(
			"gpt-4o",
			"gpt-4o-mini",
			"gpt-4-turbo",
			"gpt-4",
			"gpt-3.5-turbo"
		);
	}

	/**
	 * Query the OpenAI API for available models.
	 */
	private List<String> queryAvailableModels(final String apiKey) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("https://api.openai.com/v1/models"))
			.header("Authorization", "Bearer " + apiKey)
			.GET()
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new IOException("API request failed with status: " + response.statusCode());
		}

		// Parse the JSON response to extract model IDs
		// We'll look for "id" fields that start with "gpt"
		List<String> models = new ArrayList<>();
		Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*\"(gpt[^\"]+)\"");
		Matcher matcher = pattern.matcher(response.body());
		
		while (matcher.find()) {
			String modelId = matcher.group(1);
			// Filter to main chat models (exclude fine-tuned, instruct, etc.)
			if (isMainChatModel(modelId) && !models.contains(modelId)) {
				models.add(modelId);
			}
		}
		
		return models;
	}

	/**
	 * Check if a model ID represents a main chat model we want to show.
	 */
	private boolean isMainChatModel(final String modelId) {
		// Include main GPT models
		if (modelId.matches("gpt-4o(-mini)?")) return true;
		if (modelId.matches("gpt-4(-turbo)?")) return true;
		return modelId.matches("gpt-3\\.5-turbo");
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
			.timeout(Duration.ofSeconds(60))
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		return OpenAiStreamingChatModel.builder()
			.apiKey(apiKey)
			.modelName(modelName)
			.timeout(Duration.ofSeconds(60))
			.build();
	}
}
