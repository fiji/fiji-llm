/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package sc.fiji.llm.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class for managing the Ollama server process and CLI operations.
 * Uses subprocess calls to the ollama CLI rather than a client library.
 */
public class OllamaProcessManager {

	private Process ollamaProcess;
	private List<String> cachedInstalledModels;

	/**
	 * Checks if the Ollama server is running by attempting to list models.
	 * The ollama list command succeeds only if the server is running.
	 *
	 * @return true if the server is running and reachable, false otherwise
	 */
	public boolean isServerRunning() {
		try {
			ProcessBuilder pb = new ProcessBuilder("ollama", "list");
			Process process = pb.start();
			int exitCode = process.waitFor();
			return exitCode == 0;
		}
		catch (Exception e) {
			// Command failed or ollama not found
			return false;
		}
	}

	/**
	 * Attempts to start the Ollama server using the ollama serve command.
	 *
	 * @return true if the server was started successfully, false otherwise
	 */
	public boolean startServer() {
		try {
			ProcessBuilder pb = new ProcessBuilder("ollama", "serve");

			// Ensure environment is correct
			Map<String, String> env = pb.environment();
			env.putIfAbsent("HOME", System.getProperty("user.home"));

			File devNull;
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				devNull = new File("NUL");
			}
			else {
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
				if (isServerRunning()) {
					return true;
				}
				Thread.sleep(pollIntervalMs);
			}
		}
		catch (Exception e) {
			// Ollama may not be installed and that's OK
		}
		return false;
	}

	/**
	 * Gets the list of installed models from the local Ollama server.
	 * Results are cached until a new model is pulled.
	 *
	 * @return list of installed model names, or empty list if server is not running
	 */
	public List<String> getInstalledModels() {
		// Return cached if available and server is still running
		if (cachedInstalledModels != null && isServerRunning()) {
			return cachedInstalledModels;
		}

		// Clear cache and return empty if server isn't running
		if (!isServerRunning()) {
			cachedInstalledModels = null;
			return Collections.emptyList();
		}

		try {
			ProcessBuilder pb = new ProcessBuilder("ollama", "list");
			Process process = pb.start();

			// Parse output: skip header, read model names from first column
			List<String> models = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream())))
			{
				String line;
				boolean firstLine = true;
				while ((line = reader.readLine()) != null) {
					if (firstLine) {
						firstLine = false; // Skip header
						continue;
					}
					String[] parts = line.split("\\s+");
					if (parts.length > 0 && !parts[0].isEmpty()) {
						models.add(parts[0]);
					}
				}
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				cachedInstalledModels = models;
				return models;
			}
		}
		catch (Exception e) {
			// Failed to list models
		}

		return Collections.emptyList();
	}

	/**
	 * Pulls (downloads) a model from the Ollama registry.
	 *
	 * @param modelName the name of the model to pull
	 * @throws Exception if the pull operation fails
	 */
	public void pullModel(String modelName) throws Exception {
		ProcessBuilder pb = new ProcessBuilder("ollama", "pull", modelName);
		pb.inheritIO(); // Show output to user
		Process process = pb.start();
		int exitCode = process.waitFor();
		if (exitCode == 0) {
			// Clear cache so it gets refreshed on next call
			cachedInstalledModels = null;
		}
		else {
			throw new RuntimeException("Failed to pull model: " + modelName);
		}
	}

	/**
	 * Shuts down the Ollama process. Only stops the process if it was started by
	 * this manager. If the user has Ollama running independently, this manager
	 * will not kill it.
	 */
	public void shutdown() {
		if (ollamaProcess != null && ollamaProcess.isAlive()) {
			try {
				if (System.getProperty("os.name").toLowerCase().contains("win")) {
					// Windows: use taskkill by PID
					new ProcessBuilder("taskkill", "/PID", String.valueOf(ollamaProcess
						.pid()), "/T", "/F").start();
				}
				else {
					// macOS/Linux: send SIGINT instead of SIGTERM
					new ProcessBuilder("kill", "-2", String.valueOf(ollamaProcess.pid()))
						.start();
				}

				// Wait up to 5s for exit
				for (int i = 0; i < 10; i++) {
					if (!ollamaProcess.isAlive()) {
						return;
					}
					Thread.sleep(500);
				}
			}
			catch (Exception e) {
				// Ignore exceptions during shutdown
			}
		}
	}
}
