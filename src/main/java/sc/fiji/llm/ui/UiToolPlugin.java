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

package sc.fiji.llm.ui;

import java.awt.Rectangle;
import java.util.Base64;
import java.util.List;

import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/** AI tools for inspecting Fiji's visible UI. */
@Plugin(type = AiToolPlugin.class)
public class UiToolPlugin extends AbstractAiToolPlugin {

	public UiToolPlugin() {
		super(UiToolPlugin.class);
	}

	@Override
	public String getName() {
		return "UI Tools";
	}

	@Tool(value = { "List visible AWT and Swing windows, including titles, classes, bounds, active state, and modality" }, name = "fiji_ui_windows_read")
	public String readWindows() {
		try {
			final JsonArray windows = new JsonArray();
			for (final AWTDialogUtils.WindowInfo window : AWTDialogUtils
				.getVisibleWindows()) windows.add(windowJson(window));
			final JsonObject result = new JsonObject();
			result.add("windows", windows);
			result.addProperty("count", windows.size());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_ui_windows_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Inspect supported visible controls in one exact window. Provide an exact window title and optionally its exact class name; leave the title blank to inspect the active window" }, name = "fiji_ui_controls_read")
	public String readControls(@P("window_title") final String windowTitle,
		@P("window_class_name") final String windowClassName)
	{
		try {
			final JsonArray controls = new JsonArray();
			for (final AWTDialogUtils.ControlInfo control : AWTDialogUtils
				.getVisibleControls(windowTitle, windowClassName)) controls.add(controlJson(
					control));
			final JsonObject result = new JsonObject();
			result.addProperty("window_title", windowTitle == null ? "" : windowTitle);
			result.add("controls", controls);
			result.addProperty("count", controls.size());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_ui_controls_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Capture a PNG screenshot of one visible window or component using current screen pixels. Provide an exact window_title, using class if needed to differentiate duplicate window titles. The optional component_path from fiji_ui_controls_read allows cropping to that component. Screenshot may include overlapping or occluding windows; set activate_and_restore to true to attempt focusing the target window before capture and restoring prior focus after (behavior not guaranteed)" }, name = "fiji_ui_screenshot")
	public List<Content> screenshot(@P("window_title") final String windowTitle,
		@P("window_class_name") final String windowClassName,
		@P("component_path") final String componentPath,
		@P("activate_and_restore") final boolean activateAndRestore)
	{
		try {
			final AWTDialogUtils.ScreenshotResult screenshot = AWTDialogUtils
				.captureScreenshot(windowTitle, windowClassName, componentPath,
					activateAndRestore);
			final JsonObject metadata = new JsonObject();
			metadata.addProperty("captured", true);
			metadata.add("window", windowJson(screenshot.getWindowInfo()));
			if (screenshot.getControlInfo() != null) metadata.add("control", controlJson(
				screenshot.getControlInfo()));
			metadata.add("capture_bounds", boundsJson(screenshot.getBounds()));
			metadata.addProperty("activation_requested", activateAndRestore);
			metadata.addProperty("focus_restore_requested", activateAndRestore);
			metadata.addProperty("focus_restored", activateAndRestore && screenshot
				.isRestored());
			metadata.addProperty("previous_window_title", screenshot
				.getPreviousWindowTitle());
			metadata.addProperty("previous_window_class_name", screenshot
				.getPreviousWindowClassName());
			metadata.addProperty("previous_focus_owner_class_name", screenshot
				.getPreviousFocusOwnerClassName());
			metadata.addProperty("previous_focus_owner_name", screenshot
				.getPreviousFocusOwnerName());
			return List.of(TextContent.from(metadata.toString()), ImageContent.from(Base64
				.getEncoder().encodeToString(screenshot.getPngBytes()), "image/png"));
		}
		catch (final RuntimeException e) {
			return List.of(TextContent.from(jsonError("Failed to run fiji_ui_screenshot: " + e
				.getMessage())));
		}
	}

	@Tool(value = { "List visible AWT and Swing dialogs, including each dialog's title, messages, buttons, and modal state" }, name = "fiji_ui_dialogs_read")
	public String readDialogs() {
		try {
			final JsonArray dialogs = new JsonArray();
			for (final AWTDialogUtils.DialogInfo dialog : AWTDialogUtils
				.getVisibleDialogs())
			{
				dialogs.add(dialogJson(dialog));
			}
			final JsonObject result = new JsonObject();
			result.add("dialogs", dialogs);
			result.addProperty("count", dialogs.size());
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_ui_dialogs_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Click an exact enabled button in one visible dialog with the exact title. Call fiji_ui_dialogs_read first; this tool rejects missing or ambiguous dialogs and never chooses a button implicitly" }, name = "fiji_ui_dialog_respond")
	public String respondToDialog(@P("dialog_title") final String dialogTitle,
		@P("button_text") final String buttonText)
	{
		if (dialogTitle == null || dialogTitle.isBlank()) {
			return jsonError("dialog_title cannot be null or blank");
		}
		if (buttonText == null || buttonText.isBlank()) {
			return jsonError("button_text cannot be null or blank");
		}

		try {
			final AWTDialogUtils.DialogResponse response = AWTDialogUtils
				.respondToDialog(dialogTitle, buttonText);
			final JsonObject result = new JsonObject();
			result.addProperty("acted", true);
			result.addProperty("dialog_title", response.getDialogTitle());
			result.addProperty("dialog_class_name", response.getDialogClassName());
			result.addProperty("button_text", response.getButtonText());
			result.addProperty("action_command", response.getActionCommand());
			result.addProperty("dialog_visible_after", response
				.isDialogVisibleAfter());
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_ui_dialog_respond: " + e.getMessage());
		}
	}

	private JsonObject dialogJson(final AWTDialogUtils.DialogInfo dialog) {
		final JsonObject result = new JsonObject();
		result.addProperty("title", dialog.getTitle());
		result.addProperty("class_name", dialog.getClassName());
		result.addProperty("visible", dialog.isVisible());
		result.addProperty("active", dialog.isActive());
		result.addProperty("modal", dialog.isModal());
		result.addProperty("modality_type", dialog.getModalityType());
		result.addProperty("owner_name", dialog.getOwnerName());

		final JsonArray messages = new JsonArray();
		for (final String message : dialog.getMessages()) messages.add(message);
		result.add("messages", messages);

		final JsonArray buttons = new JsonArray();
		for (final AWTDialogUtils.ButtonInfo button : dialog.getButtons()) {
			final JsonObject buttonJson = new JsonObject();
			buttonJson.addProperty("text", button.getText());
			buttonJson.addProperty("action_command", button.getActionCommand());
			buttonJson.addProperty("enabled", button.isEnabled());
			buttonJson.addProperty("visible", button.isVisible());
			buttons.add(buttonJson);
		}
		result.add("buttons", buttons);
		return result;
	}

	private JsonObject windowJson(final AWTDialogUtils.WindowInfo window) {
		final JsonObject result = new JsonObject();
		result.addProperty("title", window.getTitle());
		result.addProperty("class_name", window.getClassName());
		result.addProperty("type", window.getType());
		result.addProperty("visible", window.isVisible());
		result.addProperty("showing", window.isShowing());
		result.addProperty("active", window.isActive());
		result.addProperty("focused", window.isFocused());
		result.addProperty("modal", window.isModal());
		result.addProperty("modality_type", window.getModalityType());
		result.addProperty("owner_title", window.getOwnerTitle());
		if (window.getBounds() != null) result.add("bounds", boundsJson(window
			.getBounds()));
		return result;
	}

	private JsonObject controlJson(final AWTDialogUtils.ControlInfo control) {
		final JsonObject result = new JsonObject();
		result.addProperty("path", control.getPath());
		result.addProperty("role", control.getRole());
		result.addProperty("class_name", control.getClassName());
		if (control.getText() != null) result.addProperty("text", control.getText());
		if (control.getValue() != null) result.addProperty("value", control.getValue());
		if (control.isSelected() != null) result.addProperty("selected", control
			.isSelected());
		if (control.isEditable() != null) result.addProperty("editable", control
			.isEditable());
		result.addProperty("value_redacted", control.isValueRedacted());
		result.addProperty("enabled", control.isEnabled());
		result.addProperty("visible", control.isVisible());
		result.addProperty("showing", control.isShowing());
		if (control.getActionCommand() != null) result.addProperty("action_command",
			control.getActionCommand());
		if (control.getBounds() != null) result.add("bounds", boundsJson(control
			.getBounds()));
		return result;
	}

	private JsonObject boundsJson(final Rectangle bounds) {
		final JsonObject result = new JsonObject();
		result.addProperty("x", bounds.x);
		result.addProperty("y", bounds.y);
		result.addProperty("width", bounds.width);
		result.addProperty("height", bounds.height);
		return result;
	}
}
