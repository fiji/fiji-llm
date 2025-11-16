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
package sc.fiji.llm.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;

import sc.fiji.llm.provider.LLMProvider;
import sc.fiji.llm.provider.ProviderService;
import sc.fiji.llm.ui.ChatbotService;

/**
 * Interactive chat interface for the Fiji AI Assistant.
 * Provides a conversational interface to get help with image analysis,
 * scripting, and general Fiji/ImageJ questions.
 */
@Plugin(type = Command.class,
	description = "Chat with an AI assistant to get help with your image analysis needs",
	iconPath = "/icons/robot-icon-32.png",
	menu = {
		@Menu(label = "Help"),
		@Menu(label = "Assistants"),
		@Menu(label = "Fiji Chat...", accelerator = "CTRL 0")
	})
public class Fiji_Chat extends DynamicCommand {
	public static final String LAST_CHAT_MODEL = "sc.fiji.chat.lastModel";
	public static final String LAST_CHAT_PROVIDER = "sc.fiji.chat.lastProvider";
	public static final String NO_MODELS_AVAILABLE = "<No Models Available For This Service>";
	public static final String AUTO_RUN = "sc.fiji.chat.autoRunChat";
	private static final String WIDTH = "400";

	@Parameter
	private ProviderService providerService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private ChatbotService chatbotService;

	@Parameter
	private UIService uiService;

	@Parameter(label = "",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String welcomeMessage = "<html><body style='width: " + WIDTH + "px'>" +
			"<h2 style='text-align: center'>Welcome to Fiji Chat!</h2>" +
			"<p>Chat with an AI assistant for help, including:</p>" +
			"<ul>" +
			"<li>Writing and debugging macros and scripts</li>" +
			"<li>Recommended commands for image analysis tasks</li>" +
			"<li>General Fiji support</li>" +
			"</ul>" +
			"<p><b>Important:</b> This feature connects to external AI services with their own terms and conditions.<br />" +
			"Your queries may not be private/confidential.<br />" +
			"For detailed documentation, see <a href=\"https://github.com/fiji/fiji-llm\">the README</a>.</p>" +
			"<p><b>NOTE:</b> This feature is in active development. <br />"+
			"The AI may provide incorrect information and make mistakes - always verify generative content.<br />" +
			"Help out by <a href=\"https://forum.image.sc/tag/llm\">contacting us on the forum</a> with issues or feature requests.<br />" +
			"</body></html>";

	@Parameter(label = "",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String providerMessage = "<html><div style='width: " + WIDTH + "px;'><hr style='border: none; border-top: 2px solid #cccccc; margin: 0;'></div>" +
			"<body style='width: " + WIDTH + "px'>" +
			"<p>First, select an <b>AI Service</b>.<br />" +
			"This is the <i>general</i> service provider you want to use (e.g. if you subscribe to ChatGPT or Claude).</p>" +
			"</body></html>";

	@Parameter(label = "AI Service →",
			callback = "providerChanged",
			persist = false)
	private String provider;

	@Parameter(label = "",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String modelMessage = "<html><body style='width: " + WIDTH + "px'>" +
			"<p>Next, choose a <b>Chat Model</b>. This is the <i>specific</i> model that you will chat with.<br />" +
			"The <b>Service Info</b> page can help you decide, as usage rates and capabilities can vary.</p>" +
			"</body></html>";

	@Parameter(label = "Service Info →",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String modelDocLink = "";

	@Parameter(label = "Chat Model →",
			choices = {},
			callback = "modelChanged",
			persist = false)
	private String model;

	@Parameter(label = "",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String nextStepsMessage = "<html><body style='width: " + WIDTH + "px'>" +
			"<p>Click <b>OK</b> when you're ready to proceed. If needed, you can set an <b>API key</b> next.</p>" +
			"</body></html>";

	@Override
	public void initialize() {
		// Get available providers and populate the provider choices
		final List<LLMProvider> providers = providerService.getInstances();
		final String[] providerNames = providers.stream()
				.map(LLMProvider::getName)
				.toArray(String[]::new);

		final MutableModuleItem<String> providerItem = getInfo().getMutableInput("provider", String.class);
		providerItem.setChoices(List.of(providerNames));
		
		// Set default provider if available
		if (providerNames.length > 0) {
			String defaultProvider = prefService.get(Fiji_Chat.class, LAST_CHAT_PROVIDER, "");
			if (!providerItem.getChoices().contains(defaultProvider)) {
				defaultProvider = providerNames[0];
			}
			providerItem.setValue(this, defaultProvider);
			providerChanged();
		}

		if (prefService.getBoolean(Fiji_Chat.class, AUTO_RUN, false)) {
			for (final var input : getInfo().inputs()) {
				resolveInput(input.getName());
			}
		}
	}

	/**
	 * Callback triggered when the provider selection changes.
	 * Updates the model choices.
	 */
	protected void providerChanged() {
		if (provider == null || provider.isEmpty()) {
			return;
		}

		final LLMProvider selectedProvider = providerService.getProvider(provider);
		if (selectedProvider == null) {
			return;
		}

		// Update model documentation link
		final String modelsUrl = selectedProvider.getModelsDocumentationUrl();
		final MutableModuleItem<String> modelDocLinkItem = getInfo().getMutableInput("modelDocLink", String.class);
		modelDocLinkItem.setValue(this, "<html><a href=\"" + modelsUrl + "\">" + modelsUrl + "</a></html>");

		// Update model choices
		final List<String> models = selectedProvider.getAvailableModels();
		final MutableModuleItem<String> modelItem = getInfo().getMutableInput("model", String.class);
		
		// Set default model
		if (!models.isEmpty()) {
			modelItem.setChoices(models);
			String defaultModel = prefService.get(Fiji_Chat.class, LAST_CHAT_MODEL, "");
			if (!modelItem.getChoices().contains(defaultModel)) {
				defaultModel = models.get(0);
			}
			modelItem.setValue(this, defaultModel);
		} else {
			modelItem.setChoices(List.of(NO_MODELS_AVAILABLE));
			modelItem.setValue(this, NO_MODELS_AVAILABLE);
		}
	}

	/**
	 * Callback triggered when the model selection changes.
	 */
	protected void modelChanged() {
		// This is called when model changes - currently just a placeholder
		// Could be used to display model-specific info
	}

	@Override
	public void run() {
		if (NO_MODELS_AVAILABLE.equals(model)) {
			uiService.showDialog("No models available for service: " + provider + "\nPlease select a different service.");
			commandService.run(Fiji_Chat.class, true);
			return;
		}
		prefService.put(Fiji_Chat.class, LAST_CHAT_PROVIDER, provider);

		final LLMProvider selectedProvider = providerService.getProvider(provider);
		String validatedModel = selectedProvider.validateModel(model);
		if (LLMProvider.VALIDATION_FAILED.equals(validatedModel)) {
			cancel("Model validation failed");
			return;
		}

		prefService.put(Fiji_Chat.class, LAST_CHAT_MODEL, validatedModel);
		if (selectedProvider.requiresApiKey()) {
			Map<String, Object> params = new HashMap<>();
			params.put("startChatbot", true);
			params.put("provider", provider);

			commandService.run(Manage_Keys.class, true, params);
		} else {
			// Create the assistant
			try {
				// Launch the chat window with provider and model info so it can recreate the assistant with memory
				chatbotService.launchChat(provider + " - " + validatedModel, provider, validatedModel);
				prefService.put(Fiji_Chat.class, Fiji_Chat.AUTO_RUN, true);
			} catch (Exception e) {
				cancel("Failed to create chat model: " + e.getMessage());
			}
		}
	}
}
