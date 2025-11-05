package sc.fiji.llm.ui;

import javax.swing.SwingUtilities;

import org.scijava.command.CommandService;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.thread.ThreadService;

import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.chat.ContextItemService;
import sc.fiji.llm.tools.AiToolService;

/**
 * Swing implementation of ChatbotService that launches a SimpleChatWindow.
 */
@Plugin(type = Service.class)
public class SwingChatbotService extends AbstractService implements ChatbotService {

    @Parameter
    private CommandService commandService;

    @Parameter
    private PrefService prefService;

    @Parameter
    private PlatformService platformService;

    @Parameter
	private AiToolService aiToolService;

    @Parameter
    private ContextItemService contextItemService;

    @Parameter
    private ThreadService threadService;

    @Override
    public String messageFormatHint() {
        return  "Your messages are displayed as plain text (no markdown support)";
    }

    @Override
    public void launchChat(FijiAssistant assistant, String title) {
        SwingUtilities.invokeLater(() -> {
            FijiAssistantChat chatWindow = new FijiAssistantChat(assistant, title, commandService, prefService, platformService, aiToolService, contextItemService, threadService, this);
            chatWindow.show();
        });
    }
}
