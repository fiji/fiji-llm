package sc.fiji.llm.ui;

import org.scijava.service.SciJavaService;

import sc.fiji.llm.assistant.FijiAssistant;

/**
 * Service interface for launching chatbot UIs.
 */
public interface ChatbotService extends SciJavaService {
    void launchChat(FijiAssistant assistant, String title);
}
