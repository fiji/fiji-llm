package sc.fiji.llm.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;

import dev.langchain4j.model.chat.ChatModel;
import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.provider.LLMProviderPlugin;
import sc.fiji.llm.service.APIKeyService;
import sc.fiji.llm.service.LLMContextService;
import sc.fiji.llm.service.LLMService;

/**
 * Interactive chat interface for the Fiji AI Assistant.
 * Provides a conversational interface to get help with image analysis,
 * scripting, and general Fiji/ImageJ questions.
 */
@Plugin(type = Command.class, menuPath = "Plugins>Assistants>Fiji Chat")
public class Fiji_Chat extends DynamicCommand {

	@Parameter
	private LLMService llmService;

	@Parameter
	private APIKeyService apiKeyService;

	@Parameter
	private LLMContextService contextService;

	@Parameter(label = "Provider", 
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			callback = "providerChanged",
			persist = false)
	private String provider;

	@Parameter(label = "API Key", 
			description = "API key for the selected provider",
			persist = false,
			required = false)
	private String apiKey = "";

	@Parameter(label = "Model",
			choices = {},
			callback = "modelChanged",
			persist = false)
	private String model;

	private FijiAssistant assistant;
	private SimpleChatWindow chatWindow;

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
			providerItem.setValue(this, providerNames[0]);
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

		// Check if we have a stored API key for this provider
		final String storedKey = apiKeyService.getApiKey(provider);
		final MutableModuleItem<String> apiKeyItem = getInfo().getMutableInput("apiKey", String.class);
		
		if (storedKey != null && !storedKey.isEmpty()) {
			apiKeyItem.setValue(this, "********"); // Mask the stored key
			apiKeyItem.setDescription("Using stored API key (enter new key to override)");
		} else {
			apiKeyItem.setValue(this, "");
			apiKeyItem.setDescription("No stored key found - please enter API key");
		}

		// Update model choices
		final List<String> models = selectedProvider.getAvailableModels();
		final MutableModuleItem<String> modelItem = getInfo().getMutableInput("model", String.class);
		modelItem.setChoices(models);
		
		// Set default model
		if (!models.isEmpty()) {
			modelItem.setValue(this, models.get(0));
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
			final ChatModel chatModel = llmService.createChatModel(provider, model);
			assistant = llmService.createAssistant(FijiAssistant.class, chatModel);
		} catch (Exception e) {
			cancel("Failed to create chat model: " + e.getMessage());
			return;
		}

		// Launch the chat window
		SwingUtilities.invokeLater(() -> {
			chatWindow = new SimpleChatWindow(assistant, contextService, provider + " - " + model);
			chatWindow.show();
		});
	}

	/**
	 * Simple Swing-based chat window.
	 */
	private static class SimpleChatWindow {
		private final FijiAssistant assistant;
		private final LLMContextService contextService;
		private final JFrame frame;
		private final JTextArea chatArea;
		private final JTextField inputField;
		private final JButton sendButton;

		public SimpleChatWindow(final FijiAssistant assistant, final LLMContextService contextService, final String title) {
			this.assistant = assistant;
			this.contextService = contextService;
			
			// Create the frame
			frame = new JFrame("Fiji Chat - " + title);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setLayout(new BorderLayout());
			
			// Chat display area
			chatArea = new JTextArea();
			chatArea.setEditable(false);
			chatArea.setLineWrap(true);
			chatArea.setWrapStyleWord(true);
			final JScrollPane scrollPane = new JScrollPane(chatArea);
			scrollPane.setPreferredSize(new Dimension(600, 400));
			
			// Input panel
			final JPanel inputPanel = new JPanel(new BorderLayout());
			inputField = new JTextField();
			sendButton = new JButton("Send");
			
			inputPanel.add(inputField, BorderLayout.CENTER);
			inputPanel.add(sendButton, BorderLayout.EAST);
			
			// Add components to frame
			frame.add(scrollPane, BorderLayout.CENTER);
			frame.add(inputPanel, BorderLayout.SOUTH);
			
			// Set up event handlers
			sendButton.addActionListener(e -> sendMessage());
			inputField.addActionListener(e -> sendMessage());
			
			// Finalize frame
			frame.pack();
			frame.setLocationRelativeTo(null);
			
			// Welcome message
			appendToChat("System", "Chat session started. Type your message and press Enter or click Send.");
		}

		public void show() {
			frame.setVisible(true);
			inputField.requestFocus();
		}

		private void sendMessage() {
			final String userMessage = inputField.getText().trim();
			if (userMessage.isEmpty()) {
				return;
			}
			
			// Display user message
			appendToChat("You", userMessage);
			inputField.setText("");
			inputField.setEnabled(false);
			sendButton.setEnabled(false);
			
			// Process in background thread
			new Thread(() -> {
				try {
					final String context = contextService.buildPluginContext();
					final String response = assistant.chat(context, userMessage);
					
					SwingUtilities.invokeLater(() -> {
						appendToChat("Assistant", response);
						inputField.setEnabled(true);
						sendButton.setEnabled(true);
						inputField.requestFocus();
					});
				} catch (Exception e) {
					SwingUtilities.invokeLater(() -> {
						appendToChat("Error", "Failed to get response: " + e.getMessage());
						inputField.setEnabled(true);
						sendButton.setEnabled(true);
						inputField.requestFocus();
					});
				}
			}).start();
		}

		private void appendToChat(final String sender, final String message) {
			chatArea.append(sender + ": " + message + "\n\n");
			chatArea.setCaretPosition(chatArea.getDocument().getLength());
		}
	}
}
