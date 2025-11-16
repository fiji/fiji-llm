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

import sc.fiji.llm.assistant.AssistantService;
import sc.fiji.llm.chat.ContextItemService;
import sc.fiji.llm.chat.ConversationService;
import sc.fiji.llm.provider.ProviderService;
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

    @Parameter
    private AssistantService assistantService;

    @Parameter
    private ProviderService providerService;

    @Parameter
    private ConversationService conversationService;

    @Override
    public void launchChat(String title, String providerName, String modelName) {
        SwingUtilities.invokeLater(() -> {
            FijiAssistantChat chatWindow = new FijiAssistantChat(title, commandService, prefService, platformService, aiToolService, contextItemService, threadService, this, assistantService, providerService, conversationService, providerName, modelName);
            chatWindow.show();
        });
    }
}
