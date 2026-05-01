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

import java.io.File;
import java.util.Map;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;

/**
 * Utility class for managing the Ollama server process and client lifecycle.
 */
public class OllamaProcessManager {

	private static final String LOCAL_SERVER_URL = "http://localhost:11434";

	private Process ollamaProcess;
	private Ollama cachedOllamaClient;

	/**
	 * Gets or creates the cached Ollama client.
	 *
	 * @return the Ollama client instance
	 */
	public Ollama getClient() {
		if (cachedOllamaClient == null) {
			cachedOllamaClient = new Ollama();
		}
		return cachedOllamaClient;
	}

	/**
	 * Checks if the Ollama server is running by attempting to ping it. If
	 * successful, caches the client. If unsuccessful, clears any cached client.
	 *
	 * @return true if the server is running and reachable, false otherwise
	 */
	public boolean isServerRunning() {
		try {
			Ollama client = getClient();
			if (client.ping()) {
				return true;
			}
		}
		catch (OllamaException e) {
			// This isn't necessarily a problem
		}
		cachedOllamaClient = null;
		return false;
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
