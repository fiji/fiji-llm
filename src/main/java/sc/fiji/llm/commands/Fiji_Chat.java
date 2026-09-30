/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
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

package sc.fiji.llm.commands;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * Interactive chat interface for the Fiji AI Assistant. Provides a
 * conversational interface to get help with image analysis, scripting, and
 * general Fiji/ImageJ questions.
 */
@Plugin(type = Command.class,
	description = "Chat with an AI assistant to get help with your image analysis needs",
	iconPath = "/icons/robot-icon-32.png", menu = { @Menu(label = "Help"), @Menu(
		label = "Assistants"), @Menu(label = "Fiji Chat...",
			accelerator = "CTRL 0") })
public class Fiji_Chat extends DynamicCommand {

	public static final String LAST_CHAT_MODEL = "sc.fiji.chat.lastModel";
	public static final String LAST_CHAT_PROVIDER = "sc.fiji.chat.lastProvider";
	private static final String CURATED_MARKER = "*";
	public static final String AUTO_RUN = "sc.fiji.chat.autoRunChat";
	private static final String NO_MODELS_AVAILABLE =
		"<No Models Available For This Service>";
	private static final String WIDTH = "400";
	private static final String MULTIPLE_MODEL_MESSAGE = "<html><body style='width: " + WIDTH + "px'>" +
		"<p>Next, choose a <b>Chat Model</b>. This is the <i>specific</i> model that you will chat with.<br />" +
		"The <b>Service Info</b> page can help you decide, as usage rates and capabilities can vary.</p>" +
		"</body></html>";
	private static final String SINGLE_MODEL_MESSAGE = "<html><body style='width: " + WIDTH + "px'>" +
		"<p>See the <b>Service Info</b> page for more information about this model provider.</p>" +
		"</body></html>";

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

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String welcomeMessage = "<html><body style='width: " + WIDTH +
		"px'>" + "<h2 style='text-align: center'>Welcome to Fiji Chat!</h2>" +
		"<p>Chat with an AI assistant for help, including:</p>" + "<ul>" +
		"<li>Recommended commands for image analysis tasks</li>" +
		"<li>Writing and debugging macros and scripts</li>" +
		"<li>General Fiji support</li>" + "</ul>" +
		"<p><b>Important:</b> This feature connects to external AI services with their own terms and conditions. " +
		"Your queries may not be private/confidential. " +
		"For detailed documentation, see <a href=\"https://github.com/fiji/fiji-llm\">the README</a>.</p>" +
		"<p><b>NOTE:</b> This feature is in active development. " +
		"The AI may provide incorrect information and make mistakes - always verify generative content." +
		"Help out by <a href=\"https://forum.image.sc/tag/llm\">contacting us on the forum</a> with issues or feature requests.</p>" +
		"</body></html>";

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String providerMessage = "<html><div style='width: " + WIDTH +
		"px;'><hr style='border: none; border-top: 2px solid #cccccc; margin: 0;'></div>" +
		"<body style='width: " + WIDTH + "px'>" +
		"<p>First, select an <b>AI Service</b>.</p><ul>" +
		"<li>This is typically the <i>general</i> model provider you want to use (e.g. ChatGPT or Claude).</li>" +
		"<li>Model selection can be overwhelming! We recommend starting with a curated (<b>*</b>) local model.</li>" +
		"<li>In general, local model services (e.g. Ollama) provide control, reproducibility, and security.</li>" +
		"<li>However, they are limited by your local hardware, and have reduced scope compared to frontier models.</li>" +
		"</ul></body></html>";

	@Parameter(label = "AI Service →", callback = "providerChanged",
		persist = false)
	private String provider;

	// Maps each service choice label to its provider name.
	private final Map<String, String> providerNamesByLabel = new LinkedHashMap<>();

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String modelMessage = MULTIPLE_MODEL_MESSAGE;

	@Parameter(label = "Service Info →",
		visibility = org.scijava.ItemVisibility.MESSAGE, persist = false,
		required = false)
	private String modelDocLink = "";

	@Parameter(label = "Chat Model →", choices = {}, callback = "modelChanged",
		persist = false)
	private String model;

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String modelCostMessage = "";

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String nextStepsMessage = "<html><body style='width: " + WIDTH +
		"px'>" +
		"<p>Click <b>OK</b> after you've made your selection.<br />" +
		"Note that some AI services will require additional configuration.</p>" +
		"</body></html>";

	@Override
	public void initialize() {
		// Get available providers and populate the provider choices
		final List<LLMProvider> providers = providerService.getInstances();
		for (final LLMProvider p : providers) {
			providerNamesByLabel.put(choiceLabel(p), p.getName());
		}
		final List<String> providerLabels = List.copyOf(providerNamesByLabel
			.keySet());

		final MutableModuleItem<String> providerItem = getInfo().getMutableInput(
			"provider", String.class);
		providerItem.setChoices(providerLabels);

		// Set default provider if available
		String recommendedModel = "";
		if (!providerLabels.isEmpty()) {
			String defaultProvider = labelForName(prefService.get(Fiji_Chat.class,
				LAST_CHAT_PROVIDER, ""));
			if (defaultProvider.isEmpty()) {
				final var recommended = providers.stream()
					.filter(p -> p.getRecommendedModel().isPresent())
					.findFirst();
				if (recommended.isPresent()) {
					defaultProvider = choiceLabel(recommended.get());
					recommendedModel = recommended.get().getRecommendedModel().get();
				}
			}
			if (!providerItem.getChoices().contains(defaultProvider)) {
				defaultProvider = providerLabels.get(0);
			}
			providerItem.setValue(this, defaultProvider);
			providerChanged();
			if (!recommendedModel.isEmpty()) {
				final MutableModuleItem<String> modelItem = getInfo().getMutableInput(
					"model", String.class);
				if (modelItem.getChoices().contains(recommendedModel)) {
					modelItem.setValue(this, recommendedModel);
				}
			}
		}

		// A recommended default was applied, so show the config dialog regardless
		if (recommendedModel.isEmpty() && prefService.getBoolean(Fiji_Chat.class, AUTO_RUN, false)) {
			for (final var input : getInfo().inputs()) {
				resolveInput(input.getName());
			}
		}
	}

	/**
	 * @return the label for a provider in the service chooser, which marks
	 *         curated providers
	 */
	static String choiceLabel(final LLMProvider p) {
		return p.isCurated() ? CURATED_MARKER + p.getName() : p.getName();
	}

	/** @return the name of the provider chosen in the service chooser */
	private String providerName() {
		return providerNamesByLabel.getOrDefault(provider, provider);
	}

	/**
	 * @return the chooser label for a provider name, or an empty string if none
	 */
	private String labelForName(final String name) {
		// Note: curated names once included the marker, so strip it.
		final String bare = name.startsWith(CURATED_MARKER) ? name.substring(
			CURATED_MARKER.length()) : name;
		return providerNamesByLabel.entrySet().stream().filter(e -> e.getValue()
			.equals(bare)).map(Map.Entry::getKey).findFirst().orElse("");
	}

	/**
	 * Callback triggered when the provider selection changes. Updates the model
	 * choices.
	 */
	protected void providerChanged() {
		if (provider == null || provider.isEmpty()) {
			return;
		}

		final LLMProvider selectedProvider = providerService.getProvider(
			providerName());
		if (selectedProvider == null) {
			return;
		}

		// Update model documentation link
		final String modelsUrl = selectedProvider.getModelsDocumentationUrl();
		final MutableModuleItem<String> modelDocLinkItem = getInfo()
			.getMutableInput("modelDocLink", String.class);
		modelDocLinkItem.setValue(this, "<html><a href=\"" + modelsUrl + "\">" +
			modelsUrl + "</a></html>");

		// Update model choices
		final List<String> models = selectedProvider.getAvailableModels();
		final MutableModuleItem<String> modelItem = getInfo().getMutableInput(
			"model", String.class);

		// Set default model
		if (!models.isEmpty()) {
			modelItem.setChoices(models);
			String defaultModel = prefService.get(Fiji_Chat.class, LAST_CHAT_MODEL,
				"");
			if (!modelItem.getChoices().contains(defaultModel)) {
				defaultModel = models.get(0);
			}
			modelItem.setValue(this, defaultModel);
		}
		else {
			modelItem.setChoices(List.of(NO_MODELS_AVAILABLE));
			modelItem.setValue(this, NO_MODELS_AVAILABLE);
		}
		modelMessage = models.size() == 1 ? SINGLE_MODEL_MESSAGE : MULTIPLE_MODEL_MESSAGE;
		modelChanged();
	}

	/**
	 * Callback triggered when the model selection changes.
	 */
	protected void modelChanged() {
		if (provider == null || provider.isEmpty() || model == null) {
			modelCostMessage = "";
			return;
		}

		final LLMProvider selectedProvider = providerService.getProvider(
			providerName());
		if (selectedProvider == null) {
			modelCostMessage = "";
			return;
		}

		modelCostMessage = selectedProvider.getCost(model).map(
			Fiji_Chat::formatCost).orElse("");
	}

	static String formatCost(final LLMProvider.ModelCost cost) {
		return String.format(Locale.ROOT,
			"<html><body style='width: %s px'><p><b>Approximate API Cost →</b> " +
				"<b>$%.2f</b> input / " +
				"<b>$%.2f</b> output per 1M tokens.</p></body></html>", WIDTH,
			cost.inputPerMillionTokens(), cost.outputPerMillionTokens());
	}

	@Override
	public void run() {
		if (NO_MODELS_AVAILABLE.equals(model)) {
			uiService.showDialog("No models available for service: " +
				providerName() +
				"\nPlease select a different service.");
			commandService.run(Fiji_Chat.class, true);
			return;
		}
		final String providerName = providerName();
		prefService.put(Fiji_Chat.class, LAST_CHAT_PROVIDER, providerName);
		prefService.remove(Fiji_Chat.class, LAST_CHAT_MODEL);
		prefService.remove(Fiji_Chat.class, Fiji_Chat.AUTO_RUN);

		final LLMProvider selectedProvider = providerService.getProvider(providerName);
		String validatedModel = selectedProvider.validateModel(model);
		if (LLMProvider.VALIDATION_FAILED.equals(validatedModel)) {
			cancel("Model validation failed");
			return;
		}

		prefService.put(Fiji_Chat.class, LAST_CHAT_MODEL, model);
		if (selectedProvider.requiresApiKey()) {
			Map<String, Object> params = new HashMap<>();
			params.put("startChatbot", true);
			params.put("provider", providerName);

			commandService.run(Manage_Keys.class, true, params);
		}
		else {
			// Create the assistant
			try {
				// Launch the chat window with provider and model info so it can
				// recreate the assistant with memory
				chatbotService.launchChat(providerName + " - " + validatedModel,
					providerName, validatedModel);
				prefService.put(Fiji_Chat.class, Fiji_Chat.AUTO_RUN, true);
			}
			catch (Exception e) {
				cancel("Failed to create chat model: " + e.getMessage());
			}
		}
	}
}
