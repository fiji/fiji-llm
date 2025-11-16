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

import java.util.List;
import java.util.stream.Collectors;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import sc.fiji.llm.auth.APIKeyService;
import sc.fiji.llm.provider.LLMProvider;
import sc.fiji.llm.provider.ProviderService;
import sc.fiji.llm.ui.ChatbotService;

/**
 * Manage API keys for different AI providers.
 * Provides a user interface to add, update, remove, and validate API keys.
 */
@Plugin(type = Command.class,
	description = "Manage API keys for AI providers",
	iconPath = "/icons/robot-icon-32.png",
	menu = {
		@Menu(label = "Help"),
		@Menu(label = "Assistants"),
		@Menu(label = "Manage API Keys...")
	})
public class Manage_Keys extends DynamicCommand {
	public static final String AUTO_RUN = "sc.fiji.chat.autoRunKeys";
	private static final String WIDTH = "260";
    private static final String MASK = "********";

	@Parameter
	private ProviderService providerService;

	@Parameter
	private APIKeyService apiKeyService;

    @Parameter
    private PrefService prefService;

    @Parameter
    private ChatbotService chatbotService;

	@Parameter(label = "",
			visibility = ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String welcomeMessage = "";

	@Parameter(label = "",
			visibility = ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String providerMessage = "";

	@Parameter(label = "AI Service →",
			callback = "providerChanged",
			persist = false)
	private String provider;

	@Parameter(label = "",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String apiKeyMessage = "";

	@Parameter(label = "Get API Key →",
			visibility = ItemVisibility.MESSAGE,
			description = "Where to get an API key for this provider",
			persist = false,
			required = false)
	private String apiKeyLink = "";

	@Parameter(label = "Enter API Key →",
			description = "New API key for this provider",
			persist = false)
	private String apiKey = "";

    @Parameter(visibility = ItemVisibility.INVISIBLE,
            required = false,
            persist = false)
    private Boolean startChatbot = false;

	@Override
	public void initialize() {
		// Get available providers
		final List<LLMProvider> providers = providerService.getInstances();
		final List<String> providerNames = providers.stream()
                .filter(LLMProvider::requiresApiKey)
				.map(LLMProvider::getName)
				.collect(Collectors.toList());

		boolean singleProvider = false;

		// If we're given a provider, make sure it actually requires keys
		if (provider != null && !provider.isEmpty()) {
			if (!providerNames.contains(provider)) {
				provider = null;
			} else {
				singleProvider = true;
			}
		}

        if (!startChatbot) {
            // This is never a required input - just a flag
            resolveInput("startChatbot");
        }

		StringBuilder welcomeMsg = new StringBuilder();
		welcomeMsg.append("<body style='width: " + WIDTH + "px'>");
		welcomeMsg.append("<h2 style='text-align: center'>Manage ");
		welcomeMsg.append(singleProvider ? provider + " " : "");
		welcomeMsg.append("API Key");
		welcomeMsg.append(singleProvider ? "" : "s");
		welcomeMsg.append("</h2>");
		welcomeMsg.append("<p><b>API keys</b> allow applications (like Fiji) to access cloud-based AI services.<br />");
		welcomeMsg.append("They also serve as <i>authentication for you</i>, as most services charge for this functionality.");
		welcomeMsg.append("Here, you can add, update, or remove the API key for ");
		welcomeMsg.append(singleProvider ? provider : "a selected AI service provider");
		welcomeMsg.append(".</p></body>");
		welcomeMessage = welcomeMsg.toString();

		if (!singleProvider) {
			StringBuilder providerMsg = new StringBuilder();
			providerMsg.append("<div style='width: " + WIDTH + "px;'><hr style='border: none; border-top: 2px solid #cccccc; margin: 0;'></div>");
			providerMsg.append("<body style='width: " + WIDTH + "px'>");
			providerMsg.append("<p>First, select an <b>AI Service</b>.<br />");
			providerMsg.append("This is the <i>general</i> service provider you want to edit the key for.</p></body>");
			providerMessage = providerMsg.toString();
		} else {
			providerMessage = null;
			resolveInput("provider");
			resolveInput("providerMessage");
		}

		StringBuilder apiKeyMsg = new StringBuilder();
		apiKeyMsg.append(singleProvider ? "<div style='width: " + WIDTH + "px;'><hr style='border: none; border-top: 2px solid #cccccc; margin: 0;'></div>" : "");
		apiKeyMsg.append("<body style='width: " + WIDTH + "px'><p>");
		apiKeyMsg.append(singleProvider ? "Please" : "Next,");
		apiKeyMsg.append(" enter your API Key for this AI service.<br />");
		apiKeyMsg.append("If you're not sure where to get one, click the <b>Get API Key</b> link.</p></body>");
		apiKeyMessage = apiKeyMsg.toString();

		final MutableModuleItem<String> providerItem = getInfo().getMutableInput("provider", String.class);

		providerItem.setChoices(providerNames);
		
		// Set default provider if available
		if (!providerNames.isEmpty()) {
			String defaultProvider = prefService.get(Fiji_Chat.class, Fiji_Chat.LAST_CHAT_PROVIDER, "");
			if (!providerItem.getChoices().contains(defaultProvider)) {
				defaultProvider = providerNames.get(0);
			}
			providerItem.setValue(this, defaultProvider);
		    providerChanged();
		}

		if (singleProvider && prefService.getBoolean(Manage_Keys.class, autoRunKey(provider), false)) {
			// Just re-start the chat without gathering input
			for (final var input : getInfo().inputs()) {
				resolveInput(input.getName());
			}
		}
	}

	/**
	 * Callback triggered when the provider selection changes.
	 * Updates the API key link and shows existing key status.
	 */
	protected void providerChanged() {
		if (provider == null || provider.isEmpty()) {
			return;
		}

		final LLMProvider selectedProvider = providerService.getProvider(provider);
		if (selectedProvider == null) {
			return;
		}

		// Update API key link
		final String apiKeyUrl = selectedProvider.getApiKeyUrl();
		final MutableModuleItem<String> apiKeyLinkItem = getInfo().getMutableInput("apiKeyLink", String.class);
		apiKeyLinkItem.setValue(this, "<a href=\"" + apiKeyUrl + "\">" + apiKeyUrl + "</a>");

		// Check if we have a stored API key for this provider
		final String storedKey = apiKeyService.getApiKey(provider);
		final MutableModuleItem<String> apiKeyItem = getInfo().getMutableInput("apiKey", String.class);
		
		if (storedKey != null && !storedKey.isEmpty()) {
			apiKeyItem.setValue(this, MASK); // Mask the stored key
			apiKeyItem.setDescription("Key is configured (enter new key to replace it)");
		} else {
			apiKeyItem.setValue(this, "");
			apiKeyItem.setDescription("Enter your " + provider + " API key from the link above");
		}
	}

	@Override
	public void run() {
		// Determine which API key to use
		String keyToUse = apiKey;
		
		if (keyToUse == null || keyToUse.isEmpty()) {
			apiKeyService.removeApiKey(provider);
		}
		else if ( keyToUse.matches("\\*+")) {
			// No action if user didn't enter a new key
		} else {
            // Store the new key
            apiKeyService.setApiKey(provider, keyToUse);
        }

        if (apiKeyService.hasApiKey(provider)) {
			prefService.put(Manage_Keys.class, autoRunKey(provider), true);
			if (startChatbot) {
				// Create the assistant
				String model = prefService.get(Fiji_Chat.class, Fiji_Chat.LAST_CHAT_MODEL);
				try {
					// Launch the chat window with provider and model info so it can recreate the assistant with memory
					chatbotService.launchChat(provider + " - " + model, provider, model);
					prefService.put(Fiji_Chat.class, Fiji_Chat.AUTO_RUN, true);
				} catch (Exception e) {
					cancel("Failed to create chat model: " + e.getMessage());
				}
			}
        }
	}

	/**
	 * @param provider
	 * @return A key to use with the {@link PrefService} for this particular provider
	 */
	public static String autoRunKey(String provider) {
		return AUTO_RUN + ":" + provider;
	}
}
