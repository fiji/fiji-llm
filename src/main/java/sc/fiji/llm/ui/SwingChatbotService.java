package sc.fiji.llm.ui;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import sc.fiji.llm.assistant.FijiAssistant;

/**
 * Swing implementation of ChatbotService that launches a SimpleChatWindow.
 */
@Plugin(type = Service.class)
public class SwingChatbotService extends AbstractService implements ChatbotService {
    @Override
    public String usageNotes() {
        return  "Your output is displayed in plain text (no markdown). ";
    }

    @Override
    public void launchChat(FijiAssistant assistant, String title) {
        SwingUtilities.invokeLater(() -> {
            FijiAssistantChat chatWindow = new FijiAssistantChat(getContext(), assistant, title);
            chatWindow.show();
        });
    }
}
