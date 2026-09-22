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

package sc.fiji.llm.log;

import org.scijava.console.ConsoleService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogMessage;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.Tool;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/** AI tools for reading ImageJ and SciJava diagnostic logs. */
@Plugin(type = AiToolPlugin.class)
public class LogToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private LogService logService;

	@Parameter
	private ConsoleService consoleService;

	@Parameter
	private LegacyService legacyService;

	private final Object captureStateLock = new Object();
	private SciJavaLogUtils.LogCapture scijavaCapture;

	public LogToolPlugin() {
		super(LogToolPlugin.class);
	}

	@Override
	public String getName() {
		return "Log Tools";
	}

	@Tool(value = { "Read the current contents of ImageJ's Log window, including whether the window is open" }, name = "fiji_log_imagej_read")
	public String readImageJLog() {
		try {
			final ImageJLogUtils.ImageJLog log = ImageJLogUtils.getLog(legacyService);
			final JsonObject result = new JsonObject();
			result.addProperty("source", "imagej");
			result.addProperty("log_window_open", log.isOpen());
			result.addProperty("text", log.getText());
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_log_imagej_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Start capturing structured SciJava log messages. Call this before running an operation whose SciJava messages should be inspected" }, name = "fiji_log_scijava_start_capture")
	public String startSciJavaCapture() {
		try {
			synchronized (captureStateLock) {
				if (scijavaCapture != null) {
					return jsonError("A SciJava log capture is already active",
						"fiji_log_scijava_read");
				}
				scijavaCapture = SciJavaLogUtils.capture(logService, consoleService);
			}
			final JsonObject result = new JsonObject();
			result.addProperty("capture_started", true);
			result.addProperty("source", "scijava");
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_log_scijava_start_capture: " + e
				.getMessage());
		}
	}

	@Tool(value = { "Read the SciJava log messages captured since fiji_log_scijava_start_capture" }, name = "fiji_log_scijava_read")
	public String readSciJavaLog() {
		try {
			final SciJavaLogUtils.LogCapture capture;
			synchronized (captureStateLock) {
				capture = scijavaCapture;
			}
			if (capture == null) {
				return jsonError("No SciJava log capture is active",
					"fiji_log_scijava_start_capture");
			}
			return sciJavaLogResult(capture.getLogs(), true).toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_log_scijava_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Stop the active SciJava log capture and return its final messages" }, name = "fiji_log_scijava_stop_capture")
	public String stopSciJavaCapture() {
		try {
			final SciJavaLogUtils.LogCapture capture;
			synchronized (captureStateLock) {
				if (scijavaCapture == null) {
					return jsonError("No SciJava log capture is active",
						"fiji_log_scijava_start_capture");
				}
				capture = scijavaCapture;
				scijavaCapture = null;
			}
			final SciJavaLogUtils.LogMessages logs;
			try {
				logs = capture.getLogs();
			}
			finally {
				capture.close();
			}
			return sciJavaLogResult(logs, false).toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_log_scijava_stop_capture: " + e
				.getMessage());
		}
	}

	private JsonObject sciJavaLogResult(final SciJavaLogUtils.LogMessages logs,
		final boolean captureActive)
	{
		final JsonArray messages = new JsonArray();
		for (final LogMessage message : logs.getMessages()) {
			final JsonObject messageJson = new JsonObject();
			messageJson.addProperty("level", LogLevel.prefix(message.level()));
			messageJson.addProperty("source", message.source().toString());
			messageJson.addProperty("message", message.text() == null ? "" : message
				.text());
			messageJson.addProperty("timestamp", message.time().getTime());
			if (message.throwable() != null) {
				messageJson.addProperty("exception", message.throwable().toString());
			}
			messages.add(messageJson);
		}

		final JsonObject result = new JsonObject();
		result.addProperty("source", "scijava");
		result.addProperty("capture_active", captureActive);
		result.addProperty("text", logs.getText());
		result.addProperty("console_stdout", logs.getStdout());
		result.addProperty("console_stderr", logs.getStderr());
		result.add("messages", messages);
		return result;
	}
}
