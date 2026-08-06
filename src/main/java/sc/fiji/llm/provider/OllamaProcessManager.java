/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.provider;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scijava.task.TaskService;

import sc.fiji.llm.ui.TaskProgressFrame;

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
				cachedInstalledModels = Collections.unmodifiableList(models);
				return cachedInstalledModels;
			}
		}
		catch (Exception e) {
			// Failed to list models
		}

		return Collections.emptyList();
	}

	/**
	 * Pulls (downloads) a model from the Ollama registry using the task service
	 * to report progress and allow cancellation.
	 *
	 * @param modelName the name of the model to pull
	 * @param taskService the task service for progress reporting and cancellation
	 * @throws Exception if the pull operation fails or is cancelled
	 */
	public void pullModel(String modelName, TaskService taskService) throws Exception {
		// Create a task for monitoring the download
		var task = taskService.createTask("Fetching Ollama model: " + modelName);

		// Show progress frame if running graphically
		try {
			TaskProgressFrame progressFrame = new TaskProgressFrame(task);
			progressFrame.showFrame();
		}
		catch (HeadlessException e) {
			// Running headless, skip GUI
		}

		ProcessBuilder pb = new ProcessBuilder("ollama", "pull", modelName);
		// Don't inherit I/O so we can capture it for progress
		pb.redirectErrorStream(true);

		Process process = pb.start();

		// Track download state
		Set<String> seenSegments = new HashSet<>();
		int segmentProgress = 0;
		String lastSeenHash = null;
		String failureReason = null;
		boolean wasCancelled = false;

		try {
			// Read output in a thread-safe manner to avoid blocking
			BufferedReader reader = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
			String line;

			task.setProgressMaximum(100);

			while ((line = reader.readLine()) != null) {
				// Check if task was cancelled
				if (task.isCanceled()) {
					process.destroyForcibly();
					reader.close();
					wasCancelled = true;
					break;
				}

				// Extract hash from output (format: "pulling <hash>:")
				String hash = extractSegmentHash(line);
				if (hash != null && !seenSegments.contains(hash)) {
					// New segment encountered - reset progress for this segment
					seenSegments.add(hash);
					segmentProgress = 0;
					task.setProgressValue(0);
					lastSeenHash = hash;
				}

				// Only update progress if it's from the current segment
				if (hash != null && hash.equals(lastSeenHash)) {
					int progress = extractProgressPercentage(line);
					if (progress >= 0) {
						segmentProgress = progress;
						task.setProgressValue(segmentProgress);
					}

					// Update task status with segment info (keep it simple and clean)
					task.setStatusMessage("Downloading segment: " + lastSeenHash.substring(0,
						Math.min(12, lastSeenHash.length())));
				}
			}

			int exitCode = process.waitFor();
			reader.close();

			if (exitCode != 0) {
				failureReason = "Failed to pull model: " + modelName + " (exit code: " +
					exitCode + ")";
			}
			else {
				// Clear cache so it gets refreshed on next call
				cachedInstalledModels = null;
			}
		}
		catch (Exception e) {
			// Wrap other exceptions
			if (process.isAlive()) {
				process.destroyForcibly();
			}
			failureReason = "Error pulling model: " + modelName + " - " + e.getMessage();
		}
		finally {
			// Single throw point: handle all error states
			task.finish();

			if (wasCancelled) {
				throw new RuntimeException("Download cancelled by user for model: " +
					modelName);
			}
			if (failureReason != null) {
				throw new RuntimeException(failureReason);
			}

			// Success
			task.setProgressValue(100);
			task.setStatusMessage("Model downloaded successfully: " + modelName);
		}
	}

	/**
	 * Extracts the segment hash from ollama output lines.
	 * Expected format: "pulling <hash>: N% ▕█████ ... ▏ ..."
	 * The hash is typically a 12-character hex string.
	 *
	 * @param line the output line from ollama
	 * @return the segment hash, or null if no hash found
	 */
	private String extractSegmentHash(String line) {
		// Look for pattern "pulling <hex>:" where hex can have uppercase or lowercase
		// Pattern allows optional whitespace before the colon
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
			"pulling\\s+([a-fA-F0-9]+)\\s*:", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	/**
	 * Extracts the progress percentage from ollama output lines.
	 * Expected format: "pulling 93567e57a8fe:   5% ▕█████ ... ▏ 339 MB/7.0 GB   43 MB/s   2m33s"
	 *
	 * @param line the output line from ollama
	 * @return the progress percentage (0-100), or -1 if no percentage found
	 */
	private int extractProgressPercentage(String line) {
		// Look for pattern like "5%" or "45%" etc
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{1,3})%");
		java.util.regex.Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			try {
				int percent = Integer.parseInt(matcher.group(1));
				// Validate it's a reasonable percentage
				if (percent >= 0 && percent <= 100) {
					return percent;
				}
			}
			catch (NumberFormatException e) {
				// Ignore and return -1
			}
		}

		return -1;
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
