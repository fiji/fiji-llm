package sc.fiji.llm.plugins;

import java.util.List;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import dev.langchain4j.model.chat.ChatModel;
import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.provider.LLMProviderPlugin;
import sc.fiji.llm.service.APIKeyService;
import sc.fiji.llm.service.LLMService;
import sc.fiji.llm.ui.ChatbotService;

/**
 * Interactive chat interface for the Fiji AI Assistant.
 * Provides a conversational interface to get help with image analysis,
 * scripting, and general Fiji/ImageJ questions.
 */
	@Plugin(type = Command.class,
		label = "Fiji Chat",
		description = "Chat with an AI assistant to get help with your image analysis needs",
		menu = {
			@Menu(label = "Plugins"),
			@Menu(label = "Assistants"),
			@Menu(label = "Fiji Chat", accelerator = "ctrl shift 0")
		})
public class Fiji_Chat extends DynamicCommand {
	private static final String LAST_CHAT_MODEL = "sc.fiji.chat.lastModel";
	private static final String LAST_CHAT_PROVIDER = "sc.fiji.chat.lastProvider";

	@Parameter
	private LLMService llmService;

	@Parameter
	private APIKeyService apiKeyService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private ChatbotService chatbotService;

	@Parameter(label = "",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String welcomeMessage = "<html><body style='width: 425px'>" +
			"<h2 style='text-align: center'>Welcome to Fiji Chat!</h2>" +
			"<p>Chat with an AI assistant to get help with your image analysis needs.</p>" +
			"<p><b>Setup:</b></p>" +
			"<ol>" +
			"<li><b>Choose an AI service</b> - Select from available providers (OpenAI, Anthropic, Google, etc.)</li>" +
			"<li><b>Enter your authentication (API) key</b> - Get this from your provider's website (link below)</li>" +
			"<li><b>Select a chat model</b> - Different models have varying capabilities and costs (see documentation)</li>" +
			"</ol>" +
			"</body></html>";

	@Parameter(label = "",
        visibility = org.scijava.ItemVisibility.MESSAGE,
        persist = false,
        required = false)
	private String welcomeSeparator = "<html><div style='width: 500px; margin: 15px 0;'><hr style='border: none; border-top: 2px solid #cccccc; margin: 0;'></div></html>";

	@Parameter(label = "AI Service",
			callback = "providerChanged",
			persist = false)
	private String provider;

    @Parameter( label = " ", style = "separator", persist = false, required = false, visibility = ItemVisibility.MESSAGE )
	private String apiSpacer = "";

	@Parameter(label = "Get Authentication Key →",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String apiKeyLink = "";

	@Parameter(label = "Authentication Key",
			description = "API key for the selected provider",
			persist = false,
			required = false)
	private String apiKey = "";

    @Parameter( label = " ", style = "separator", persist = false, required = false, visibility = ItemVisibility.MESSAGE )
	private String modelSpacer = "";

	@Parameter(label = "View Model Info →",
			visibility = org.scijava.ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String modelDocLink = "";

	@Parameter(label = "Chat Model",
			choices = {},
			callback = "modelChanged",
			persist = false)
	private String model;

	private FijiAssistant assistant;

	@Override
	public void initialize() {
		// Get available providers and populate the provider choices
		final List<LLMProviderPlugin> providers = llmService.getAvailableProviders();
		final String[] providerNames = providers.stream()
				.map(LLMProviderPlugin::getName)
				.toArray(String[]::new);

		final MutableModuleItem<String> providerItem = getInfo().getMutableInput("provider", String.class);
		providerItem.setChoices(List.of(providerNames));
		
		// Set default provider if available
		if (providerNames.length > 0) {
			String defaultProvider = prefService.get(Fiji_Chat.class, LAST_CHAT_PROVIDER, provider);
			if (!providerItem.getChoices().contains(defaultProvider)) {
				defaultProvider = providerNames[0];
			}
			providerItem.setValue(this, defaultProvider);
			providerChanged();
		}
	}

	/**
	 * Callback triggered when the provider selection changes.
	 * Updates the model choices and checks for existing API key.
	 */
	protected void providerChanged() {
		if (provider == null || provider.isEmpty()) {
			return;
		}

		final LLMProviderPlugin selectedProvider = llmService.getProvider(provider);
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
			apiKeyItem.setValue(this, "********"); // Mask the stored key
			apiKeyItem.setDescription("Using stored key (enter new key to replace it)");
		} else {
			apiKeyItem.setValue(this, "");
			apiKeyItem.setDescription("Enter your " + provider + " API key from the link above");
		}

		// Update model documentation link
		final String modelsUrl = selectedProvider.getModelsDocumentationUrl();
		final MutableModuleItem<String> modelDocLinkItem = getInfo().getMutableInput("modelDocLink", String.class);
		modelDocLinkItem.setValue(this, "<html><a href=\"" + modelsUrl + "\">" + modelsUrl + "</a></html>");

		// Update model choices
		final List<String> models = selectedProvider.getAvailableModels();
		final MutableModuleItem<String> modelItem = getInfo().getMutableInput("model", String.class);
		modelItem.setChoices(models);
		
		// Set default model
		if (!models.isEmpty()) {
			String defaultModel = prefService.get(Fiji_Chat.class, LAST_CHAT_MODEL, model);
			if (!modelItem.getChoices().contains(defaultModel)) {
				defaultModel = models.get(0);
			}
			modelItem.setValue(this, defaultModel);
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
		// Determine which API key to use
		String keyToUse = apiKey;
		
		// If the input is masked (********) or empty, use the stored key
		if (keyToUse == null || keyToUse.isEmpty() || keyToUse.equals("********")) {
			keyToUse = apiKeyService.getApiKey(provider);
		} else {
			// User entered a new key - store it
			apiKeyService.setApiKey(provider, keyToUse);
		}

		if (keyToUse == null || keyToUse.isEmpty()) {
			cancel("No API key available for " + provider);
			return;
		}

		// Create the chat model
		try {
			prefService.put(Fiji_Chat.class, LAST_CHAT_PROVIDER, provider);
			prefService.put(Fiji_Chat.class, LAST_CHAT_MODEL, model);
			final ChatModel chatModel = llmService.createChatModel(provider, model);
			assistant = llmService.createAssistant(FijiAssistant.class, chatModel);
		} catch (Exception e) {
			cancel("Failed to create chat model: " + e.getMessage());
			return;
		}

		// Launch the chat window using ChatbotService
		chatbotService.launchChat(assistant, provider + " - " + model);
	}
}
