package sc.fiji.llm.ui;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.service.LLMContextService;

/**
 * Swing implementation of ChatbotService that launches a SimpleChatWindow.
 */
@Plugin(type = Service.class)
public class SwingChatbotService extends AbstractService implements ChatbotService {

    @Parameter
    public LLMContextService llmContextService;

    @Override
    public void launchChat(FijiAssistant assistant, String title) {
        SwingUtilities.invokeLater(() -> {
            SimpleChatWindow chatWindow = new SimpleChatWindow(assistant, llmContextService, title);
            chatWindow.show();
        });
    }
}
