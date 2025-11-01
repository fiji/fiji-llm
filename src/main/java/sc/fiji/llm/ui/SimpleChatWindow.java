package sc.fiji.llm.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.chat.ContextItem;
import sc.fiji.llm.chat.Conversation;
import sc.fiji.llm.chat.ScriptContextItem;

/**
 * Simple Swing-based chat window for LLM chat.
 */
public class SimpleChatWindow {
    private static enum Sender {USER, ASSISTANT, SYSTEM, ERROR};

    @Parameter
    private CommandService commandService;

    @Parameter
    private PrefService prefService;

    private final FijiAssistant assistant;
    private final JFrame frame;
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JPanel contextTagsPanel;
    private final List<ContextItem> contextItems;
    private final java.util.Map<ContextItem, JButton> contextItemButtons;
    private final Conversation conversation;

    public SimpleChatWindow(final Context context, final FijiAssistant assistant, final String title) {
        context.inject(this);
        this.assistant = assistant;
        this.contextItems = new ArrayList<>();
        this.contextItemButtons = new java.util.HashMap<>();
        final String systemPrompt = "You are an expert Fiji/ImageJ assistant. You help users with image analysis, " +
            "processing, and scripting in the Fiji/ImageJ environment.\n\n" +
            "When you generate or modify scripts for the user, always use the createOrUpdateScript tool.";
        this.conversation = new Conversation(systemPrompt);

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

        // Button bar with context buttons on left, action buttons on right
        final JPanel buttonBar = new JPanel(new BorderLayout());

        // Left side - context buttons
        final JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

        final JButton scriptButton = new JButton("📜 Add Script");
        scriptButton.setToolTipText("Add script context (right-click for options)");

        // Left-click: add active script
        scriptButton.addActionListener(e -> addActiveScriptContext());

        // Right-click: show menu of available scripts
        scriptButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    showScriptSelectionMenu(scriptButton, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    showScriptSelectionMenu(scriptButton, e.getX(), e.getY());
                }
            }
        });

        leftButtons.add(scriptButton);
        // Add more context type buttons here in the future

        // Right side - action buttons
        final JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));

        final JButton changeModelButton = new JButton("Change Model");
        changeModelButton.addActionListener(e -> changeModel());
        rightButtons.add(changeModelButton);

        buttonBar.add(leftButtons, BorderLayout.WEST);
        buttonBar.add(rightButtons, BorderLayout.EAST);

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
        inputField = new JTextField();
        sendButton = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        inputPanelContainer.add(buttonBar);
        inputPanelContainer.add(contextTagsPanel);
        inputPanelContainer.add(inputPanel);

        // Add components to frame
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

    private void sendMessage() {
        final String userMessage = inputField.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        // Build context with attached items
        final StringBuilder contextMsg = new StringBuilder();

        if (!contextItems.isEmpty()) {
            contextMsg.append("===Start of Context Items===\n");
            for (final ContextItem item : contextItems) {
                contextMsg.append("\n--- ").append(item.getType()).append(": ").append(item.getLabel()).append(" ---\n");
                contextMsg.append(item.getContent()).append("\n");
            }
            contextMsg.append("===End of Context Items===\n");
            conversation.addUserMessage(contextMsg.toString());
        }

        // Display user message
        appendToChat(Sender.USER, userMessage);

        inputField.setText("");
        inputField.setEnabled(false);
        sendButton.setEnabled(false);

        // Process in background thread
        new Thread(() -> {
            try {
                // Create and send ChatRequest
                final ChatRequest chatRequest = conversation.buildChatRequest();

                final ChatResponse response = assistant.chat(chatRequest);
                final String assistantMessage = response.aiMessage().text();

                SwingUtilities.invokeLater(() -> {
                    appendToChat(Sender.ASSISTANT, assistantMessage);
                    inputField.setEnabled(true);
                    sendButton.setEnabled(true);
                    inputField.requestFocus();
                });
            } catch (Exception e) {
                // If this was a rate-limit / quota error, parse a retry delay and show a short system message.
                final Long retrySecs = parseRetrySeconds(e);

                SwingUtilities.invokeLater(() -> {
                    if (retrySecs != null && retrySecs > 0) {
                        appendToChat(Sender.SYSTEM, "Rate limit reached. Please wait ~" + retrySecs + "s before retrying, or select a different model.");
                    } else {
                        // Fall back to a short message including the exception summary
                        final String msg = e.getMessage() != null ? e.getMessage().replaceAll("\\n", " ").replaceAll("\\s+", " ") : "(no message)";
                        if (msg.length() > 300) {
                            appendToChat(Sender.SYSTEM, "Error: " + msg.substring(0, 300) + "…");
                        } else {
                            appendToChat(Sender.SYSTEM, "Error: " + msg);
                        }
                    }

                    // Re-enable inputs so the user can change model or try again manually
                    inputField.setEnabled(true);
                    sendButton.setEnabled(true);
                    inputField.requestFocus();
                });
            }
        }).start();
    }

    /**
     * Parse a retry delay in seconds from the exception message chain.
     * Recognizes patterns like "Please retry in 25.3s" or JSON fields like "retryDelay":"26s".
     */
    private Long parseRetrySeconds(final Throwable t) {
        if (t == null) return null;

        final Pattern p1 = Pattern.compile("Please retry in\\s*([0-9]+(?:\\.[0-9]+)?)s", Pattern.CASE_INSENSITIVE);
        final Pattern p2 = Pattern.compile("retryDelay.*?(\\d+)s", Pattern.CASE_INSENSITIVE);

        Throwable cur = t;
        while (cur != null) {
            final String msg = cur.getMessage();
            if (msg != null) {
                Matcher m1 = p1.matcher(msg);
                if (m1.find()) {
                    try {
                        double secs = Double.parseDouble(m1.group(1));
                        return (long) Math.ceil(secs);
                    } catch (NumberFormatException ignored) {
                    }
                }

                Matcher m2 = p2.matcher(msg);
                if (m2.find()) {
                    try {
                        long secs = Long.parseLong(m2.group(1));
                        return secs;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            cur = cur.getCause();
        }

        return null;
    }

    private void appendToChat(final Sender sender, final String message) {
        chatArea.append(sender + ": " + message + "\n\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());

        // Track messages for API calls
        switch (sender) {
            case USER -> conversation.addUserMessage(message);
            case ASSISTANT -> conversation.addAssistantMessage(message);
        }
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

            // Get the title and editor pane
            final String scriptName = tab.getTitle();
            final EditorPane editorPane = (EditorPane) tab.getEditorPane();

            // Get the text content and language
            final String scriptContent = editorPane.getText();
            final Object language = editorPane.getCurrentLanguage();
            final String languageName = language != null ? language.toString().toLowerCase() : "unknown";

            // Add the script context
            final ScriptContextItem scriptItem = new ScriptContextItem(scriptName, scriptContent, languageName);
            addContextItem(scriptItem);
        } catch (Exception e) {
            // If we can't access the script editor, show an error
            appendToChat(Sender.ERROR, "Failed to access script editor: " + e.getMessage());
        }
    }

    private void showScriptSelectionMenu(final JButton button, final int x, final int y) {
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

        menu.show(button, x, y);
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

            // Get the title
            final String scriptName = tab.getTitle();

            // Get the editor pane
            final EditorPane editorPane = (EditorPane) tab.getEditorPane();

            // Get the text content
            final String scriptContent = editorPane.getText();

            // Get the language
            final Object language = editorPane.getCurrentLanguage();
            final String languageName = language != null ? language.toString().toLowerCase() : "unknown";

            // Add the script context
            final ScriptContextItem scriptItem = new ScriptContextItem(scriptName, scriptContent, languageName);
            addContextItem(scriptItem);

        } catch (Exception e) {
            appendToChat(Sender.ERROR, "Failed to add script from tab: " + e.getMessage());
        }
    }

    private void addContextItem(final ContextItem item) {
        // Check for duplicates - don't add the same script twice
        for (final ContextItem existing : contextItems) {
            if (existing.getLabel().equals(item.getLabel()) &&
                existing.getContent().equals(item.getContent())) {
                // Flash the existing tag to indicate it's already added
                final JButton existingButton = contextItemButtons.get(existing);
                if (existingButton != null) {
                    flashButton(existingButton);
                }
                return;
            }
        }

        contextItems.add(item);

        // Truncate label to max length
        final int maxLabelLength = 15;
        String displayLabel = item.getLabel();
        if (displayLabel.length() > maxLabelLength) {
            displayLabel = displayLabel.substring(0, maxLabelLength - 1) + "…";
        }

        // Create a removable tag button with truncated label and X
        final JButton tagButton = new JButton(displayLabel + " ✕");
        tagButton.setToolTipText(item.getLabel() + " - Click to remove");
        tagButton.addActionListener(e -> removeContextItem(item, tagButton));

        // Store the button reference
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
        contextItems.remove(item);
        contextItemButtons.remove(item);
        final JPanel tagsContainer = (JPanel) contextTagsPanel.getComponent(0);
        tagsContainer.remove(tagButton);

        if (contextItems.isEmpty()) {
            contextTagsPanel.setVisible(false);
        }

        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
    }

    private void clearAllContext() {
        contextItems.clear();
        contextItemButtons.clear();
        contextTagsPanel.removeAll();
        contextTagsPanel.setVisible(false);
        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
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
