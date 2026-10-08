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
	public static final String AUTO_RUN = "sc.fiji.chat.autoRunChat";
	private static final String NO_MODELS_AVAILABLE =
		"<No Models Available For This Service>";
	private static final String WIDTH = "300";
	private static final String HOSTED_SERVICE_MESSAGE =
		"<html><body style='width: " + WIDTH + "px'>" +
		"<b>Cloud AI service.</b> Powerful remotely hosted models.<br />" +
		"Data handling and retention are subject to provider policies.</body></html>";
	private static final String LOCAL_SERVICE_MESSAGE =
		"<html><body style='width: " + WIDTH + "px'>" +
		"<b>Local AI service.</b> Models run on your hardware.<br />" +
		"All data and messages stay on your computer." +
		"</body></html>";
	private static final double MEDIUM_COST_THRESHOLD = 10.0;
	private static final double HIGH_COST_THRESHOLD = 20.0;
	private static final double MEDIUM_DEMAND_THRESHOLD = 10.0;
	private static final double HIGH_DEMAND_THRESHOLD = 18.0;
	private static final int DEMAND_REFERENCE_CONTEXT_TOKENS = 32 * 1024;

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
		"px'>" + "<h1 style='text-align: center'>Welcome to Fiji Chat!</h1>" +
		"<p>For detailed documentation, see <a href=\"https://github.com/fiji/fiji-llm\" " +
		"style='color: #1a5fb4; text-decoration: underline;'>the README</a>.<br />" +
		"AI may be inaccurate. Always verify important results.<br /><br />" +
		"This feature is under active development.<br />" +
		"Please <a href=\"https://forum.image.sc/tag/llm\" " +
		"style='color: #1a5fb4; text-decoration: underline;'>contact us on the forum</a> " +
		"with issues or requests." +
		"</p></body></html>";

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String providerMessage = "<html><div style='width: " + WIDTH +
		"px;'><hr style='border: none; border-top: 2px solid #cccccc; margin: 0;'></div>" +
		"<body style='width: " + WIDTH + "px'>" +
		"<h2 style='text-align: center'>Getting Started</h2>" +
		"<p>1. Select an <b>AI Service</b> (e.g. ChatGPT or Claude).<br />" +
		"2. Select a specific <b>Chat Model</b> to talk with." +
		"</p></body></html>";

	@Parameter(label = "AI Service →", callback = "providerChanged",
		persist = false)
	private String provider;

	@Parameter(label = "Model Docs →",
		visibility = org.scijava.ItemVisibility.MESSAGE, persist = false,
		required = false)
	private String modelDocLink = "";

	@Parameter(label = "Chat Model →", choices = {}, callback = "modelChanged",
		persist = false)
	private String model;

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String serviceInfoMessage = "";

	@Parameter(label = "", visibility = org.scijava.ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String modelTierMessage = "";

	@Override
	public void initialize() {
		// Get available providers and populate the provider choices
		final List<LLMProvider> providers = providerService.getInstances();
		final List<String> providerLabels = providers.stream().map(
			LLMProvider::getName).toList();

		final MutableModuleItem<String> providerItem = getInfo().getMutableInput(
			"provider", String.class);
		providerItem.setChoices(providerLabels);

		// Set default provider if available
		if (!providerLabels.isEmpty()) {
			String defaultProvider = prefService.get(Fiji_Chat.class,
				LAST_CHAT_PROVIDER, "");
			if (!providerItem.getChoices().contains(defaultProvider)) {
				defaultProvider = providerLabels.get(0);
			}
			providerItem.setValue(this, defaultProvider);
			providerChanged();
		}

		// Show the config dialog when the previous run requested auto-start.
		if (prefService.getBoolean(Fiji_Chat.class, AUTO_RUN, false)) {
			for (final var input : getInfo().inputs()) {
				resolveInput(input.getName());
			}
		}
	}

	/** @return the name of the provider chosen in the service chooser */
	private String providerName() {
		return provider;
	}

	/**
	 * Callback triggered when the provider selection changes. Updates the model
	 * choices.
	 */
	protected void providerChanged() {
		if (provider == null || provider.isEmpty()) {
			serviceInfoMessage = "";
			return;
		}

		final LLMProvider selectedProvider = providerService.getProvider(
			providerName());
		if (selectedProvider == null) {
			serviceInfoMessage = "";
			return;
		}
		serviceInfoMessage = formatServiceInfo(selectedProvider);

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
		modelChanged();
	}

	static String formatServiceInfo(final LLMProvider provider) {
		return provider.requiresApiKey() ? HOSTED_SERVICE_MESSAGE :
			LOCAL_SERVICE_MESSAGE;
	}

	/**
	 * Callback triggered when the model selection changes.
	 */
	protected void modelChanged() {
		if (provider == null || provider.isEmpty() || model == null) {
			modelTierMessage = "";
			return;
		}

		final LLMProvider selectedProvider = providerService.getProvider(
			providerName());
		if (selectedProvider == null) {
			modelTierMessage = "";
			return;
		}

		modelTierMessage = selectedProvider.getCost(model).map(Fiji_Chat::formatCost)
			.orElseGet(() -> selectedProvider.getDemand(model).map(
				Fiji_Chat::formatDemand).orElse(""));
	}

	private enum ModelTier {
		LOW(1, "Low", "#2e7d32"), MEDIUM(2, "Medium", "#c58a00"), HIGH(3,
			"High", "#c62828");

		private final int filledSegments;
		private final String label;
		private final String color;

		ModelTier(final int filledSegments, final String label, final String color) {
			this.filledSegments = filledSegments;
			this.label = label;
			this.color = color;
		}

		private String meterCellsHtml() {
			final StringBuilder meter = new StringBuilder();
			for (int segmentIndex = 0; segmentIndex < 3; segmentIndex++) {
				final boolean filled = segmentIndex < filledSegments;
				final String cellLabel = segmentIndex == filledSegments - 1 ? label : "";
				meter.append("<td bgcolor=\"").append(filled ? color :
					"#d6d6d6").append("\" width='72' height='30' align='center'>")
					.append("<font color=\"").append(filled ? "#ffffff" : "#555555")
					.append("\"><b>").append(cellLabel).append(
						"</b></font></td>");
			}
			return meter.toString();
		}
	}

	private static ModelTier costTier(final LLMProvider.ModelCost cost) {
		final double totalCost = cost.inputPerMillionTokens() + cost
			.outputPerMillionTokens();
		if (totalCost <= MEDIUM_COST_THRESHOLD) return ModelTier.LOW;
		if (totalCost <= HIGH_COST_THRESHOLD) return ModelTier.MEDIUM;
		return ModelTier.HIGH;
	}

	private static ModelTier demandTier(final LLMProvider.ModelDemand demand) {
		final double estimatedDemand = demand.estimateGiB(
			DEMAND_REFERENCE_CONTEXT_TOKENS);
		if (estimatedDemand <= MEDIUM_DEMAND_THRESHOLD) return ModelTier.LOW;
		if (estimatedDemand <= HIGH_DEMAND_THRESHOLD) return ModelTier.MEDIUM;
		return ModelTier.HIGH;
	}

	private static String formatTier(final String label, final ModelTier tier) {
		return String.format(Locale.ROOT,
			"<html><body style='width: %s px'><table border='0' cellpadding='0' " +
			"cellspacing='0'><tr><td valign='middle'><b>%s →</b>" +
			"&nbsp;</td>%s</tr></table></body></html>", WIDTH, label,
			tier.meterCellsHtml());
	}

	static String formatCost(final LLMProvider.ModelCost cost) {
		return formatTier("Relative API Cost", costTier(cost));
	}

	static String formatDemand(final LLMProvider.ModelDemand demand) {
		return formatTier("Relative Local Demand", demandTier(demand));
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
