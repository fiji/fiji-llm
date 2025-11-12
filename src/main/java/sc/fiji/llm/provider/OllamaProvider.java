package sc.fiji.llm.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

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

	private static final String DEFAULT_BASE_URL = "http://localhost:11434";
	private Process ollamaProcess;
	private Ollama cachedOllamaClient;

	@Parameter
	private LogService logService;

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
		// Get actual list of installed models from Ollama
		try {
			return ollamaClient().listModels().stream()
				.map(model -> model.getName())
				.collect(Collectors.toList());
		} catch (OllamaException e) {
			return new ArrayList<>();
		}
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
			.baseUrl(DEFAULT_BASE_URL)
			.modelName(modelName)
			.timeout(DEFAULT_TIMEOUT)
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String apiKey, final String modelName) {
		return OllamaStreamingChatModel.builder()
			.baseUrl(DEFAULT_BASE_URL)
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
			cachedOllamaClient = new Ollama(DEFAULT_BASE_URL);
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
			Ollama client = new Ollama(DEFAULT_BASE_URL);
			if (client.ping()) {
				cachedOllamaClient = client;
				return true;
			}
		} catch (OllamaException e) {
			// This isn't necessarily a problem
		}
		cachedOllamaClient = null;
		return false;
	}
}
