package sc.fiji.llm.provider;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;

/**
 * LLM provider plugin for Ollama (locally run models).
 */
@Plugin(type = LLMProvider.class, name = "Ollama")
public class OllamaProvider implements LLMProvider {

	private static final String MODEL_URL = "https://ollama.com/search?c=tools&c=thinking";
	private static final String TAG_BASE_URL = "https://ollama.com/library/";
	private static final String LOCAL_SERVER_URL = "http://localhost:11434";
	private static final String REMOTE_STRING = "* (remote)";

	private Process ollamaProcess;
	private Ollama cachedOllamaClient;
	private Set<String> cachedRemoteTags;

	@Parameter
	private LogService logService;

	@Parameter
	private UIService uIService;

	@Parameter
	private StatusService statusService;

	@Override
	public String getName() {
		return "Ollama";
	}

	@Override
	public String getDescription() {
		return "Local Ollama models";
	}

	@Override
	public List<String> getAvailableModels() {
		// Get basic available remote models
		Set<String> remoteTags = fetchRemoteTags();
		// Get actual list of installed models from Ollama
		List<String> models;
		try {
			 models = ollamaClient().listModels().stream()
				.map(model -> model.getName())
				.collect(Collectors.toList());
		} catch (OllamaException e) {
			return List.copyOf(remoteTags);
		}
		remoteTags.removeAll(models);
		remoteTags.stream().map(m -> m + REMOTE_STRING).forEach(models::add);

		return models;
	}

	@Override
	public String validateModel(String modelToValidate) {
		if (modelToValidate.endsWith(REMOTE_STRING)) {
			if (uIService.showDialog("The selected LLM model will be downloaded. This could take some time.\nProceed?", MessageType.WARNING_MESSAGE, OptionType.OK_CANCEL_OPTION).equals(Result.OK_OPTION)) {

				String modelName = modelToValidate.substring(0, modelToValidate.length() - REMOTE_STRING.length());
				statusService.showStatus(-1, -1, "Downloading Ollama model: " + modelName);
				try {
					ollamaClient().pullModel(modelName);
				} catch (OllamaException e) {
					statusService.clearStatus();
					statusService.showStatus("Download failed: " + modelName);
					// e.printStackTrace();
					// Failed to pull
					return modelToValidate;
				}
				statusService.clearStatus();
				statusService.showStatus("Download complete: " + modelName);
				// Pull successful
				return modelName;
			} else {
				return LLMProvider.VALIDATION_FAILED;
			}
		}
		// Not a remote model
		return modelToValidate;
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://ollama.com/download";
	}

	@Override
	public boolean requiresApiKey() {
		return false;
	}

	@Override
	public String getApiKeyUrl() {
		return "";
	}

	@Override
	public ChatModel createChatModel(final String apiKey, final String modelName) {
		return OllamaChatModel.builder()
			.baseUrl(LOCAL_SERVER_URL)
			.modelName(modelName)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		return OllamaStreamingChatModel.builder()
			.baseUrl(LOCAL_SERVER_URL)
			.modelName(modelName)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}

	@Override
	public void initialize() {
		if (isOllamaServerRunning()) {
			return;
		}

		// Try to start Ollama server
		startOllamaServer();
	}

	@Override
	public void dispose() {
		// Only shut down Ollama if we started it ourselves.
		// If the user has Ollama running independently, we shouldn't kill it.
		if (ollamaProcess != null && ollamaProcess.isAlive()) {
			try {
				if (System.getProperty("os.name").toLowerCase().contains("win")) {
					// Windows: use taskkill by PID
					new ProcessBuilder("taskkill", "/PID", String.valueOf(ollamaProcess.pid()), "/T", "/F").start();
				} else {
					// macOS/Linux: send SIGINT instead of SIGTERM
					new ProcessBuilder("kill", "-2", String.valueOf(ollamaProcess.pid())).start();
				}

				// Wait up to 5s for exit
				for (int i = 0; i < 10; i++) {
					if (!ollamaProcess.isAlive()) {
						return;
					}
					Thread.sleep(500);
				}
		    } catch (Exception e) {

			}
		}
	}

	/**
	 * Attempts to start the Ollama server using the ollama serve command.
	 *
	 * @return true if the server was started successfully, false otherwise
	 */
	private boolean startOllamaServer() {
		try {
			ProcessBuilder pb = new ProcessBuilder("ollama", "serve");

			// Ensure environment is correct
			Map<String, String> env = pb.environment();
			env.putIfAbsent("HOME", System.getProperty("user.home"));
			// If you’ve installed Ollama in a non-standard location, add it here:
			// env.put("PATH", env.get("PATH") + ":/usr/local/bin");

			File devNull;
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				devNull = new File("NUL");
			} else {
				devNull = new File("/dev/null");
			}
			// Ignore output from the server
			pb.redirectOutput(devNull);

			// Redirect output so we can see startup logs
			pb.redirectErrorStream(true);

			ollamaProcess = pb.start();

			// Poll for readiness
			int maxAttempts = 10;
			int pollIntervalMs = 1000;
			for (int i = 0; i < maxAttempts; i++) {
				if (isOllamaServerRunning()) {
					return true;
				}
				Thread.sleep(pollIntervalMs);
			}
		} catch (Exception e) {
			// Ollama may not be installed and that's OK
		}
		logService.error("Unnable to connect to local ollama server");
		return false;
	}

	/**
	 * Gets or creates the cached Ollama client.
	 *
	 * @return the Ollama client instance
	 */
	private Ollama ollamaClient() {
		if (cachedOllamaClient == null) {
			cachedOllamaClient = new Ollama(LOCAL_SERVER_URL);
		}
		return cachedOllamaClient;
	}

	/**
	 * Checks if the Ollama server is running by attempting to ping it.
	 * If successful, caches the client. If unsuccessful, clears any cached client.
	 *
	 * @return true if the server is running and reachable, false otherwise
	 */
	private boolean isOllamaServerRunning() {
		try {
			Ollama client = ollamaClient();
			if (client.ping()) {
				return true;
			}
		} catch (OllamaException e) {
			// This isn't necessarily a problem
		}
		cachedOllamaClient = null;
		return false;
	}

	/**
	 * See https://github.com/ollama/ollama/issues/8241
	 */
	private Set<String> fetchRemoteTags() {
		if (cachedRemoteTags != null) return cachedRemoteTags;

		Set<String> remoteTags = new LinkedHashSet<>();
        // Fetch and parse the page
		try {
			Document doc = Jsoup.connect(MODEL_URL)
					.userAgent("Mozilla/5.0 (compatible; Java Jsoup)")
					.get();

			// Step 1: get tool names
			Elements models = doc.select("span[x-test-search-response-title]");
			for (Element model : models) {
				String modelName = model.text();

				// Step 2: fetch tags for this tool
				String tagsUrl = TAG_BASE_URL + modelName;
				Document tagsDoc = Jsoup.connect(tagsUrl)
						.userAgent("Mozilla/5.0 (compatible; Java Jsoup)")
						.get();

				// Select <a> elements that contain the tags. Prefer extracting the
				// canonical tag from the href (e.g. /library/qwen3:8b -> qwen3:8b).
				// Some anchors contain additional UI labels (like a separate
				// "latest" span) so using .text() yields e.g. "qwen3:8b latest"
				// which previously caused us to filter out valid tags. Use the
				// href attribute and the hidden input.command value as fallback.
				Elements tagLinks = tagsDoc.select("a[href^='/library/" + modelName + "']");
				for (Element tagLink : tagLinks) {
					String href = tagLink.attr("href"); // e.g. /library/qwen3:8b
					String tag = null;

					if (href != null && href.startsWith("/library/")) {
						tag = href.substring("/library/".length());
						// strip query or trailing slash if any
						int q = tag.indexOf('?');
						if (q != -1) tag = tag.substring(0, q);
						if (tag.endsWith("/")) tag = tag.substring(0, tag.length() - 1);
					}

					// fallback: some desktop rows include an <input class="command" value="qwen3:8b" />
					if ((tag == null || tag.isEmpty()) ) {
						Element input = tagLink.selectFirst("input.command[value]");
						if (input != null) {
							tag = input.attr("value");
						}
					}

					if (tag == null || tag.isEmpty()) continue;

					String lower = tag.toLowerCase();
					// Skip aliases like `qwen3:latest` (we prefer explicit version tags)
					if (!lower.contains(":") || lower.endsWith(":latest") || lower.equals("latest") || lower.contains("cloud")) {
						continue;
					}

					remoteTags.add(tag);
				}
			}
		} catch (IOException e) {
			// Remote tags not available
		}
		cachedRemoteTags = remoteTags;
		return remoteTags;
	}
}
