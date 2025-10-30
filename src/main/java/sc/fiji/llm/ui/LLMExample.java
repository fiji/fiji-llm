package sc.fiji.llm.ui;

import java.util.List;

import org.scijava.plugin.Parameter;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.service.LLMContextService;
import sc.fiji.llm.service.LLMService;

/**
 * Example demonstrating how to use the Fiji LLM services.
 * This would be used as the basis for a chat window or other UI component.
 */
public class LLMExample {

	@Parameter
	private LLMService llmService;

	@Parameter
	private LLMContextService contextService;

	private FijiAssistant assistant;
	private ChatMemory memory;
	private String currentProvider;
	private String currentModel;

	/**
	 * Initialize the assistant with a specific provider and model.
	 *
	 * @param providerName the LLM provider (e.g., "OpenAI", "Anthropic", "Google")
	 * @param modelName the model to use (e.g., "gpt-4o", "claude-3-5-sonnet-20241022")
	 */
	public void initialize(final String providerName, final String modelName) {
		// Create the chat model
		final ChatLanguageModel model = llmService.createChatModel(providerName, modelName);

		// Create conversation memory (remembers last 20 messages)
		this.memory = MessageWindowChatMemory.withMaxMessages(20);

		// Create the assistant
		this.assistant = llmService.createAssistant(FijiAssistant.class, model);

		this.currentProvider = providerName;
		this.currentModel = modelName;
	}

	/**
	 * Send a message to the assistant and get a response.
	 *
	 * @param userMessage the user's message
	 * @return the assistant's response
	 */
	public String chat(final String userMessage) {
		// Build context from the current Fiji/ImageJ environment
		final String context = contextService.buildPluginContext();

		// Send message and get response
		return assistant.chat(context, userMessage);
	}

	/**
	 * Generate a script for a specific task.
	 *
	 * @param language the scripting language
	 * @param task description of what the script should do
	 * @return the generated script code
	 */
	public String generateScript(final String language, final String task) {
		final String context = contextService.buildPluginContext();
		return assistant.generateScript(language, task, context);
	}

	/**
	 * Explain a code snippet.
	 *
	 * @param code the code to explain
	 * @param language the programming language
	 * @return an explanation
	 */
	public String explainCode(final String code, final String language) {
		return assistant.explainCode(code, language);
	}

	/**
	 * Get help debugging an error.
	 *
	 * @param errorMessage the error message
	 * @param stackTrace the stack trace (can be null)
	 * @param code the code that caused the error (can be null)
	 * @return debugging suggestions
	 */
	public String debugError(final String errorMessage, final String stackTrace, final String code) {
		return assistant.debugError(errorMessage, stackTrace, code);
	}

	/**
	 * Get the conversation history.
	 *
	 * @return list of chat messages
	 */
	public List<ChatMessage> getHistory() {
		return memory.messages();
	}

	/**
	 * Clear the conversation history.
	 */
	public void clearHistory() {
		memory.clear();
	}

	/**
	 * Get the current provider name.
	 *
	 * @return the provider name
	 */
	public String getCurrentProvider() {
		return currentProvider;
	}

	/**
	 * Get the current model name.
	 *
	 * @return the model name
	 */
	public String getCurrentModel() {
		return currentModel;
	}
}
