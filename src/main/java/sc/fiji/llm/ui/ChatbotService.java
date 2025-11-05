package sc.fiji.llm.ui;

import org.scijava.service.SciJavaService;

/**
 * Service interface for launching chatbot UIs.
 */
public interface ChatbotService extends SciJavaService {
    String messageFormatHint();

    /**
     * Launch a chat window with the given assistant.
     * 
     * @param assistant the assistant instance (will be recreated with memory)
     * @param title the window title
     * @param providerName the name of the LLM provider (e.g., "OpenAI")
     * @param modelName the name of the model (e.g., "gpt-4")
     */
    void launchChat(String title, String providerName, String modelName);
}
