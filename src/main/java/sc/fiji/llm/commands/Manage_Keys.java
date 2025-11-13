package sc.fiji.llm.commands;

import java.util.List;

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
	menu = {
		@Menu(label = "Help"),
		@Menu(label = "Assistants"),
		@Menu(label = "Manage API Keys...")
	})
public class Manage_Keys extends DynamicCommand {
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
	private String welcomeMessage = "<html><body style='width: 425px'>" +
			"<h2 style='text-align: center'>Manage API Keys</h2>" +
			"<p>Add, update, or remove API keys for your AI service providers.</p>" +
			"<p><b>Instructions:</b></p>" +
			"<ol>" +
			"<li><b>Select a provider</b> - Choose an AI service</li>" +
			"<li><b>Enter the key</b> - Paste your API key in the field below</li>" +
			"</ol>" +
			"</body></html>";

	@Parameter(label = "",
		visibility = ItemVisibility.MESSAGE,
		persist = false,
		required = false)
	private String welcomeSeparator = "<html><div style='width: 500px; margin: 15px 0;'><hr style='border: none; border-top: 2px solid #cccccc; margin: 0;'></div></html>";

	@Parameter(label = "AI Service",
			callback = "providerChanged",
			persist = false)
	private String provider;

	@Parameter(label = "Get Authentication Key →",
			visibility = ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String apiKeyLink = "";

	@Parameter(label = "Authentication Key",
			description = "API key for the selected provider",
			persist = false)
	private String apiKey = "";

    @Parameter(visibility = ItemVisibility.INVISIBLE,
            required = false,
            persist = false)
    private Boolean startChatbot = false;

	@Override
	public void initialize() {
        if (!startChatbot) {
            // This is never a required input - just a flag
            resolveInput("startChatbot");
        }

		final MutableModuleItem<String> providerItem = getInfo().getMutableInput("provider", String.class);

		// Get available providers and populate the provider choices
		final List<LLMProvider> providers = providerService.getInstances();
		final String[] providerNames = providers.stream()
                .filter(LLMProvider::requiresApiKey)
				.map(LLMProvider::getName)
				.toArray(String[]::new);

		providerItem.setChoices(List.of(providerNames));
		
		// Set default provider if available
		if (providerNames.length > 0) {
			String defaultProvider = prefService.get(Fiji_Chat.class, Fiji_Chat.LAST_CHAT_PROVIDER, "");
			if (!providerItem.getChoices().contains(defaultProvider)) {
				defaultProvider = providerNames[0];
			}
			providerItem.setValue(this, defaultProvider);
		    providerChanged();
		}

		if (startChatbot && prefService.getBoolean(Fiji_Chat.class, Fiji_Chat.SKIP_INPUTS, false)) {
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
		apiKeyLinkItem.setValue(this, "<html><a href=\"" + apiKeyUrl + "\">" + apiKeyUrl + "</a></html>");

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
		
		// If the input is masked (********) or empty, use the stored key
		if (keyToUse == null || keyToUse.isEmpty() || keyToUse.equals(MASK)) {
			// No action if user didn't enter a new key
		} else {
            // Store the new key
            apiKeyService.setApiKey(provider, keyToUse);
        }

        if (startChatbot && apiKeyService.hasApiKey(provider)) {
            // Create the assistant
        	String model = prefService.get(Fiji_Chat.class, Fiji_Chat.LAST_CHAT_MODEL);
            try {
				prefService.put(Fiji_Chat.class, Fiji_Chat.SKIP_INPUTS, "true");
                // Launch the chat window with provider and model info so it can recreate the assistant with memory
                chatbotService.launchChat(provider + " - " + model, provider, model);
            } catch (Exception e) {
                cancel("Failed to create chat model: " + e.getMessage());
            }
        }
	}
}
