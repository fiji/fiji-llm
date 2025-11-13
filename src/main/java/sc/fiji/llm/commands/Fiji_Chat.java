package sc.fiji.llm.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

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
		menu = {
			@Menu(label = "Help"),
			@Menu(label = "Assistants"),
			@Menu(label = "Fiji Chat...", accelerator = "CTRL 0")
		})
public class Fiji_Chat extends DynamicCommand {
	public static final String LAST_CHAT_MODEL = "sc.fiji.chat.lastModel";
	public static final String LAST_CHAT_PROVIDER = "sc.fiji.chat.lastProvider";
	public static final String SKIP_INPUTS = "sc.fiji.chat.skipInputs";

	@Parameter
	private ProviderService providerService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private CommandService commandService;

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

		if (prefService.getBoolean(Fiji_Chat.class, SKIP_INPUTS, false)) {
			// Just re-start the chat without gathering input
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
		modelItem.setChoices(models);
		
		// Set default model
		if (!models.isEmpty()) {
			String defaultModel = prefService.get(Fiji_Chat.class, LAST_CHAT_MODEL, "");
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
		prefService.put(Fiji_Chat.class, LAST_CHAT_PROVIDER, provider);
		prefService.put(Fiji_Chat.class, LAST_CHAT_MODEL, model);

		final LLMProvider selectedProvider = providerService.getProvider(provider);
		if (selectedProvider.requiresApiKey()) {
			Map<String, Object> params = new HashMap<>();
			params.put("startChatbot", true);
			params.put("provider", provider);

			commandService.run(Manage_Keys.class, true, params);
		} else {
			// Create the assistant
			try {
				prefService.put(Fiji_Chat.class, SKIP_INPUTS, "true");
				// Launch the chat window with provider and model info so it can recreate the assistant with memory
				chatbotService.launchChat(provider + " - " + model, provider, model);
			} catch (Exception e) {
				cancel("Failed to create chat model: " + e.getMessage());
			}
		}
	}
}
