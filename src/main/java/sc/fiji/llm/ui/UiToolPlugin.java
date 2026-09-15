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
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.ui;

import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/** AI tools for inspecting Fiji's visible UI dialogs. */
@Plugin(type = AiToolPlugin.class)
public class UiToolPlugin extends AbstractAiToolPlugin {

	public UiToolPlugin() {
		super(UiToolPlugin.class);
	}

	@Override
	public String getName() {
		return "UI Tools";
	}

	@Override
	public String getUsage() {
		return """
The fiji_ui_* tools provide information about visible UI components.
""";
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
}