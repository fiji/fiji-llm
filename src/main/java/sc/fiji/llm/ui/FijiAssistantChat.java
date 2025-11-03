package sc.fiji.llm.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.request.ChatRequest;
import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.chat.ContextItem;
import sc.fiji.llm.chat.Conversation;
import sc.fiji.llm.chat.ConversationBuilder;
import sc.fiji.llm.chat.ScriptContextItem;
import sc.fiji.llm.tools.AiToolService;

/**
 * Swing-based chat window for chatting with LLMs in Fiji.
 */
public class FijiAssistantChat {
    private static enum Sender {USER, ASSISTANT, SYSTEM, ERROR};

    private static final String SYSTEM_PROMPT = "You are an assistant chatbot running as an integrated plugin within the Fiji (ImageJ) application for scientific image analysis. " +
        "Your mission is to help users develop reproducible workflows (e.g. via scripts), answer their image analysis questions, and select the best tools for their data and goals. " +
        "Key elements of your persona: positive, validating, patient, encouraging, understanding.";

    @Parameter
    private CommandService commandService;

    @Parameter
    private PrefService prefService;

	@Parameter
	private AiToolService aiToolService;

    private final FijiAssistant assistant;
    private final JFrame frame;
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JPanel contextTagsPanel;
    private final java.util.Map<ContextItem, JButton> contextItemButtons;
    private final Conversation conversation;

    public FijiAssistantChat(final Context context, final FijiAssistant assistant, final String title) {
        context.inject(this);
        this.assistant = assistant;
        this.contextItemButtons = new java.util.HashMap<>();
        this.conversation = new ConversationBuilder()
            .withBaseSystemMessage(SYSTEM_PROMPT)
            .withTools(aiToolService.getInstances())
            .build();

        // Create the frame
        frame = new JFrame("Fiji Chat - " + title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top navigation bar with model selection
        final JPanel topNavBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        final JButton changeModelButton = new JButton("Change Model");
        changeModelButton.addActionListener(e -> changeModel());
        topNavBar.add(changeModelButton);

        // Chat display area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        final JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // Button bar with context buttons on left
        final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

        // Create a square button with dropdown for script selection
        final JPanel scriptButtonPanel = createScriptSelectorPanel();

        buttonBar.add(scriptButtonPanel);
        // Add more context type buttons here in the future

        // Context tags panel (shows active context items as removable tags)
        contextTagsPanel = new JPanel();
        contextTagsPanel.setLayout(new BorderLayout());
        contextTagsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(200, 220, 240), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        contextTagsPanel.setBackground(new java.awt.Color(240, 248, 255)); // Light blue background
        contextTagsPanel.setVisible(false); // Hidden until context items are added

        // Input panel container
        final JPanel inputPanelContainer = new JPanel();
        inputPanelContainer.setLayout(new BoxLayout(inputPanelContainer, BoxLayout.Y_AXIS));

		// Input panel
		final JPanel inputPanel = new JPanel(new BorderLayout());
		inputField = new PlaceholderTextField("Type your message here...");
		sendButton = new JButton("Send");
		inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        inputPanelContainer.add(buttonBar);
        inputPanelContainer.add(contextTagsPanel);
        inputPanelContainer.add(inputPanel);

        // Add components to frame
        frame.add(topNavBar, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanelContainer, BorderLayout.SOUTH);

        // Set up event handlers
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Finalize frame
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
        inputField.requestFocus();
    }

    /**
     * Creates and configures a square icon button with consistent styling.
     * Used for large icon buttons in the button bar (e.g., script selector, change model).
     */
    private JButton createIconButton(final String icon, final String tooltip, final float fontSize) {
        final JButton button = new JButton(icon);
        button.setPreferredSize(new Dimension(50, 50));
        button.setToolTipText(tooltip);
        button.setFont(button.getFont().deriveFont(fontSize));
        button.setFocusPainted(false);
        return button;
    }

    private JPanel createScriptSelectorPanel() {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        panel.setOpaque(false);

        // Main square button with script icon
        final JButton mainButton = createIconButton("📜", "Attach the active script as context", 24f);

        // Dropdown button (small arrow button)
        final JButton dropdownButton = new JButton("▼");
        dropdownButton.setPreferredSize(new Dimension(30, 50));
        dropdownButton.setToolTipText("Select a script to attach as context");
        dropdownButton.setFont(dropdownButton.getFont().deriveFont(10f));
        dropdownButton.setFocusPainted(false);

        // When main button clicked, add active script
        mainButton.addActionListener(e -> addActiveScriptContext());

        // When dropdown clicked, show script selection menu
        dropdownButton.addActionListener(e -> {
            final JPopupMenu menu = buildScriptSelectionMenu();
            menu.show(dropdownButton, 0, dropdownButton.getHeight());
        });

        panel.add(mainButton);
        panel.add(dropdownButton);

        return panel;
    }

    private JPopupMenu buildScriptSelectionMenu() {
        final JPopupMenu menu = new JPopupMenu();

        try {
            final java.util.List<TextEditor> instances = TextEditor.instances;

            if (instances == null || instances.isEmpty()) {
                // No script editor open
                final JMenuItem openEditorItem = new JMenuItem("Open Script Editor...");
                openEditorItem.addActionListener(e ->
                    commandService.run(org.scijava.ui.swing.script.ScriptEditor.class, true));
                menu.add(openEditorItem);
            } else {
                // Add menu item for the active script
                final JMenuItem activeItem = new JMenuItem("Active Script");
                activeItem.addActionListener(e -> addActiveScriptContext());
                menu.add(activeItem);

                // Add menu items for all open scripts if there are multiple
                if (instances.size() > 1 || hasMultipleTabs(TextEditorUtils.getMostRecentVisibleEditor())) {
                    menu.addSeparator();

                    for (final TextEditor textEditor : instances) {
                        addScriptMenuItems(menu, textEditor);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback menu
            final JMenuItem errorItem = new JMenuItem("(Script editor not available)");
            errorItem.setEnabled(false);
            menu.add(errorItem);
        }

        return menu;
    }

    private void sendMessage() {
        final String userMessage = inputField.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        // Add message to conversation synchronously on EDT before spawning background thread
        conversation.addUserMessage(userMessage);

        // Display user message (this will use invokeLater but message is already in conversation)
        appendToChat(Sender.USER, userMessage);

        inputField.setText("");
        inputField.setEnabled(false);
        sendButton.setEnabled(false);

        // Process in background thread (LLM calls happen OFF the EDT)
        new Thread(() -> {
            try {
                // Create and send ChatRequest (happens on background thread)
                final ChatRequest chatRequest = conversation.buildChatRequest();

                final AiMessage message = assistant.chat(chatRequest);

                appendToChat(Sender.ASSISTANT, message.text());
            } catch (RateLimitException e) {
                // If this was a rate-limit / quota error show a short system message
                appendToChat(Sender.SYSTEM, "Rate limit reached. Please wait before retrying, or select a different model.");
            } catch (Exception e) {
                // Fall back to a short message including the exception summary
                final String msg = e.getMessage() != null ? e.getMessage().replaceAll("\\n", " ").replaceAll("\\s+", " ") : "(no message)";
                if (msg.length() > 300) {
                    appendToChat(Sender.SYSTEM, "Error: " + msg.substring(0, 300) + "…");
                } else {
                    appendToChat(Sender.SYSTEM, "Error: " + msg);
                }
            } finally {
                // Re-enable inputs so the user can change model or try again manually
                SwingUtilities.invokeLater(() -> {
                    inputField.setEnabled(true);
                    sendButton.setEnabled(true);
                    inputField.requestFocus();
                });
            }
        }).start();
    }

    private void appendToChat(final Sender sender, final String message) {
        if (message == null) return;

        // Always use invokeLater since this can be called from both EDT and background threads
        SwingUtilities.invokeLater(() -> {
            chatArea.append(sender + ": " + message + "\n\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());

            // Track messages for API calls
            switch (sender) {
                case USER -> {
                    // User message already added to conversation synchronously in sendMessage()
                    // Just clear context UI
                    clearAllContextButtons();
                }
                case ASSISTANT -> conversation.addAssistantMessage(message);
                case SYSTEM, ERROR -> {} // System and error messages are not tracked in conversation
            }
        });
    }

    private void changeModel() {
        // Clear the last chat model preference to force model selection dialog
        prefService.remove(sc.fiji.llm.plugins.Fiji_Chat.class, "sc.fiji.chat.lastModel");

        // Close this chat window
        frame.dispose();

        // Re-invoke the Fiji_Chat command to show the selection dialog
        commandService.run(sc.fiji.llm.plugins.Fiji_Chat.class, true);
    }

    private void addActiveScriptContext() {
        // Try to get the active script from the script editor
        try {
            // Access the static list of open TextEditor instances and pick
            // the most-recent one that is visible. TextEditor.instances
            // appends instances on creation, so the last element is the
            // newest. If none are visible, open the ScriptEditor.
            final TextEditor textEditor = TextEditorUtils.getMostRecentVisibleEditor();
            if (textEditor == null) {
                // No visible editor instance found - open the Script Editor
                commandService.run(org.scijava.ui.swing.script.ScriptEditor.class, true);
                return;
            }

            // Try to obtain the currently selected tab. If a no-arg getTab() exists, use it; otherwise fall back
            TextEditorTab tab = null;
            try {
                // Some versions expose a no-arg getTab() for the active tab
                tab = textEditor.getTab();
            } catch (Throwable t) {
                // Fall back to the first tab if no no-arg getTab() is available
                try {
                    tab = textEditor.getTab(0);
                } catch (Throwable t2) {
                    // Give up if we can't access a tab
                    throw new RuntimeException("Unable to access script tab", t2);
                }
            }

            if (tab == null) {
                appendToChat(Sender.ERROR, "No active script tab available");
                return;
            }

            // Get the tab index
            final int tabIndex = findTabIndex(textEditor, tab);
            if (tabIndex < 0) {
                appendToChat(Sender.ERROR, "Failed to locate tab index");
                return;
            }

            // Build and add the script context
            final ScriptContextItem scriptItem = buildScriptContextItem(textEditor, tab, tabIndex);
            addContextItem(scriptItem);
        } catch (Exception e) {
            // If we can't access the script editor, show an error
            appendToChat(Sender.ERROR, "Failed to access script editor: " + e.getMessage());
        }
    }

    private String getErrorOutput(final TextEditor textEditor) {
        try {
            final javax.swing.JTextArea errorScreen = textEditor.getErrorScreen();
            if (errorScreen != null) {
                final String text = errorScreen.getText();
                return text != null ? text.trim() : "";
            }
        } catch (Exception e) {
            // If we can't access error output, just return empty string
        }
        return "";
    }

    private boolean hasMultipleTabs(final TextEditor textEditor) {
        if (textEditor == null) {
            return false;
        }
        try {
            int count = 0;
            while (true) {
                try {
                    TextEditorTab tab = textEditor.getTab(count);
                    if (tab == null) break;
                    count++;
                    if (count > 1) return true;
                } catch (Exception e) {
                    break;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void addScriptMenuItems(final JPopupMenu menu, final TextEditor textEditor) {
        try {
            int i = 0;
            while (true) {
                TextEditorTab tab = textEditor.getTab(i);
                if (tab == null) break;

                final String title = tab.getTitle();
                final int tabIndex = i;
                final JMenuItem item = new JMenuItem(title);
                item.addActionListener(e -> addScriptFromTab(textEditor, tabIndex));
                menu.add(item);

                i++;
            }
        } catch (Exception e) {
            // Skip this editor if we can't access its tabs
        }
    }

    private void addScriptFromTab(final TextEditor textEditor, final int tabIndex) {
        try {
            final TextEditorTab tab = textEditor.getTab(tabIndex);
            if (tab == null) return;

            // Build and add the script context
            final ScriptContextItem scriptItem = buildScriptContextItem(textEditor, tab, tabIndex);
            addContextItem(scriptItem);

        } catch (Exception e) {
            appendToChat(Sender.ERROR, "Failed to add script from tab: " + e.getMessage());
        }
    }

    /**
     * Builds a ScriptContextItem from a TextEditor and tab.
     * Extracts script content, error output, and selection information.
     */
    private ScriptContextItem buildScriptContextItem(final TextEditor textEditor, final TextEditorTab tab, final int tabIndex) {
        // Get the title and strip leading asterisks (which indicate unsaved changes)
        final String scriptName = stripLeadingAsterisks(tab.getTitle());

        // Get the editor pane
        final EditorPane editorPane = (EditorPane) tab.getEditorPane();

        // Get the text content with line numbers
        final String scriptContent = TextEditorUtils.addLineNumbers(editorPane.getText());

        // Get error output from TextEditor
        final String errorOutput = getErrorOutput(textEditor);

        // Get instance index
        final int instanceIndex = TextEditor.instances.indexOf(textEditor);

        // Get selection line numbers
        final int[] selectionLines = getSelectionLineNumbers(editorPane);

        return new ScriptContextItem(scriptName, scriptContent, instanceIndex, tabIndex, errorOutput, selectionLines[0], selectionLines[1]);
    }

    /**
     * Strips leading asterisks from a script name (asterisks indicate unsaved changes).
     */
    private String stripLeadingAsterisks(final String scriptName) {
        if (scriptName == null) {
            return null;
        }
        return scriptName.replaceAll("^\\*+", "");
    }

    /**
     * Finds the tab index of a given tab within a TextEditor.
     * Returns -1 if the tab is not found.
     */
    private int findTabIndex(final TextEditor textEditor, final TextEditorTab targetTab) {
        for (int i = 0; ; i++) {
            try {
                TextEditorTab currentTab = textEditor.getTab(i);
                if (currentTab == null) break;
                if (currentTab == targetTab) {
                    return i;
                }
            } catch (Exception e) {
                break;
            }
        }
        return -1;
    }

    /**
     * Extracts selection start and end line numbers from an EditorPane.
     * Returns an array [startLine, endLine]. Both default to NO_SELECTION if there is no actual highlighted text.
     * A true selection is indicated by either different start/end lines, or same line with actual selected text.
     */
    private int[] getSelectionLineNumbers(final EditorPane editorPane) {
        int selectionStartLine = ScriptContextItem.NO_SELECTION;
        int selectionEndLine = ScriptContextItem.NO_SELECTION;

        try {
            final String selectedText = editorPane.getSelectedText();

            // Only report selection if there's actual highlighted text
            if (selectedText != null && !selectedText.isEmpty()) {
                final int selectionStart = editorPane.getSelectionStart();
                final int selectionEnd = editorPane.getSelectionEnd();
                selectionStartLine = editorPane.getLineOfOffset(selectionStart) + 1; // Lines are 1-indexed
                selectionEndLine = editorPane.getLineOfOffset(selectionEnd) + 1;
            }
        } catch (Exception e) {
            // If we can't get selection info, just use NO_SELECTION
        }

        return new int[]{selectionStartLine, selectionEndLine};
    }

    private void addContextItem(final ContextItem item) {
        // Check for duplicates using equals() - don't add the same item twice
        if (conversation.getContextItems().contains(item)) {
            // Flash the existing tag to indicate it's already added
            final JButton existingButton = contextItemButtons.get(item);
            if (existingButton != null) {
                flashButton(existingButton);
            }
            return;
        }

        // Add to conversation
        conversation.addContextItem(item);

        // Truncate label to max length (not counting selection info)
        final int maxLabelLength = 15;
        String displayLabel = item.getLabel();
        if (displayLabel.length() > maxLabelLength) {
            displayLabel = displayLabel.substring(0, maxLabelLength - 1) + "…";
        }

        // Append selection info if this is a script context item with a selection
        String selectionInfo = "";
        if (item instanceof ScriptContextItem) {
            final ScriptContextItem scriptItem = (ScriptContextItem) item;
            if (scriptItem.hasSelection()) {
                selectionInfo = " (" + scriptItem.getSelectionStartLine() + "-" + scriptItem.getSelectionEndLine() + ")";
            }
        }

        // Create a removable tag button with truncated label, selection info, and X
        final JButton tagButton = new JButton(displayLabel + selectionInfo + " ✕");

        // Build tooltip with script name and selection info
        String tooltipText = item.getLabel() + " - Click to remove";
        if (item instanceof ScriptContextItem) {
            final ScriptContextItem scriptItem = (ScriptContextItem) item;
            tooltipText = scriptItem.getScriptName();
            if (scriptItem.hasSelection()) {
                tooltipText += " (lines " + scriptItem.getSelectionStartLine() + "-" + scriptItem.getSelectionEndLine() + ")";
            }
            tooltipText += " - Click to remove";
        }
        tagButton.setToolTipText(tooltipText);
        tagButton.addActionListener(e -> removeContextItem(item, tagButton));

        // Store the button reference (item -> button)
        contextItemButtons.put(item, tagButton);

        // Style the button to look like a flat tag
        tagButton.setFocusPainted(false);
        tagButton.setContentAreaFilled(false);
        tagButton.setOpaque(true);
        tagButton.setBackground(java.awt.Color.WHITE);
        tagButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));

        // Add hover effect
        tagButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                tagButton.setBackground(new java.awt.Color(230, 240, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tagButton.setBackground(java.awt.Color.WHITE);
            }
        });

        // Get or create the tags container panel
        JPanel tagsContainer;
        if (contextTagsPanel.getComponentCount() == 0) {
            tagsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
            tagsContainer.setOpaque(false);
            contextTagsPanel.add(tagsContainer, BorderLayout.CENTER);

            // Add "Clear All" button on the right
            final JButton clearAllButton = new JButton("Clear All");
            clearAllButton.setToolTipText("Remove all context items");
            clearAllButton.setFocusPainted(false);
            clearAllButton.addActionListener(e -> clearAllContext());
            final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 3));
            rightPanel.setOpaque(false);
            rightPanel.add(clearAllButton);
            contextTagsPanel.add(rightPanel, BorderLayout.EAST);
        } else {
            tagsContainer = (JPanel) contextTagsPanel.getComponent(0);
        }

        tagsContainer.add(tagButton);
        contextTagsPanel.setVisible(true);
        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
    }

    private void removeContextItem(final ContextItem item, final JButton tagButton) {
        conversation.removeContextItem(item);
        contextItemButtons.remove(item);
        final JPanel tagsContainer = (JPanel) contextTagsPanel.getComponent(0);
        tagsContainer.remove(tagButton);

        if (conversation.getContextItems().isEmpty()) {
            contextTagsPanel.setVisible(false);
        }

        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
    }

    private void clearAllContextButtons() {
        // Remove all context items from the conversation
        for (final ContextItem item : new ArrayList<>(conversation.getContextItems())) {
            conversation.removeContextItem(item);
        }

        contextItemButtons.clear();
        contextTagsPanel.removeAll();
        contextTagsPanel.setVisible(false);
        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
    }

    private void clearAllContext() {
        clearAllContextButtons();
    }

    private void flashButton(final JButton button) {
        // Flash the button orange to indicate duplicate
        final java.awt.Color originalBg = button.getBackground();
        final java.awt.Color flashColor = new java.awt.Color(255, 165, 0); // Orange

        // Create a timer to flash 3 times
        final javax.swing.Timer timer = new javax.swing.Timer(150, null);
        final int[] flashCount = {0};

        timer.addActionListener(e -> {
            if (flashCount[0] % 2 == 0) {
                button.setBackground(flashColor);
            } else {
                button.setBackground(originalBg);
            }
            flashCount[0]++;

            if (flashCount[0] >= 6) { // 3 flashes (on-off-on-off-on-off)
                timer.stop();
                button.setBackground(originalBg);
            }
        });

        timer.start();
    }
}
