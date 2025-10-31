package sc.fiji.llm.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

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

import org.scijava.command.CommandService;
import org.scijava.prefs.PrefService;

import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.service.LLMContextService;

/**
 * Simple Swing-based chat window for LLM chat.
 */
public class SimpleChatWindow {
    private final FijiAssistant assistant;
    private final LLMContextService contextService;
    private final CommandService commandService;
    private final PrefService prefService;
    private final JFrame frame;
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JPanel contextTagsPanel;
    private final List<ContextItem> contextItems;

    public SimpleChatWindow(final FijiAssistant assistant, final LLMContextService contextService,
                            final CommandService commandService, final PrefService prefService, final String title) {
        this.assistant = assistant;
        this.contextService = contextService;
        this.commandService = commandService;
        this.prefService = prefService;
        this.contextItems = new ArrayList<>();

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
        contextTagsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
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

        // Display user message
        appendToChat("You", userMessage);
        inputField.setText("");
        inputField.setEnabled(false);
        sendButton.setEnabled(false);

        // Process in background thread
        new Thread(() -> {
            try {
                // Build context with both plugin context and attached items
                final StringBuilder fullContext = new StringBuilder();
                fullContext.append(contextService.buildPluginContext());

                // Add attached context items
                if (!contextItems.isEmpty()) {
                    fullContext.append("\n\n=== Attached Context ===\n");
                    for (final ContextItem item : contextItems) {
                        fullContext.append("\n--- ").append(item.getType()).append(": ").append(item.getLabel()).append(" ---\n");
                        fullContext.append(item.getContent()).append("\n");
                    }
                }

                final String response = assistant.chat(fullContext.toString(), userMessage);

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
            // Access the static list of open TextEditor instances
            final Class<?> textEditorClass = Class.forName("org.scijava.ui.swing.script.TextEditor");
            final java.lang.reflect.Field instancesField = textEditorClass.getField("instances");
            @SuppressWarnings("unchecked")
            final java.util.List<Object> instances = (java.util.List<Object>) instancesField.get(null);

            if (instances.isEmpty()) {
                // No script editor open - open it
                commandService.run(org.scijava.ui.swing.script.ScriptEditor.class, true);
                return;
            }

            // Get the first (most recently active) editor instance
            final Object textEditor = instances.get(0);

            // Get the current tab
            final java.lang.reflect.Method getTabMethod = textEditorClass.getMethod("getTab");
            final Object tab = getTabMethod.invoke(textEditor);

            // Get the title from the tab
            final java.lang.reflect.Method getTitleMethod = tab.getClass().getMethod("getTitle");
            final String scriptName = (String) getTitleMethod.invoke(tab);

            // Get the editor pane from the tab
            final java.lang.reflect.Field editorPaneField = tab.getClass().getDeclaredField("editorPane");
            editorPaneField.setAccessible(true);
            final Object editorPane = editorPaneField.get(tab);

            // Get the text content
            final java.lang.reflect.Method getTextMethod = editorPane.getClass().getMethod("getText");
            final String scriptContent = (String) getTextMethod.invoke(editorPane);

            // Get the language
            final java.lang.reflect.Method getLanguageMethod = editorPane.getClass().getMethod("getCurrentLanguage");
            final Object language = getLanguageMethod.invoke(editorPane);
            final String languageName = language != null ? language.toString().toLowerCase() : "unknown";

            // Add the script context
            final ScriptContextItem scriptItem = new ScriptContextItem(scriptName, scriptContent, languageName);
            addContextItem(scriptItem);

        } catch (Exception e) {
            // If we can't access the script editor, show an error
            appendToChat("Error", "Failed to access script editor: " + e.getMessage());
        }
    }

    private void showScriptSelectionMenu(final JButton button, final int x, final int y) {
        final JPopupMenu menu = new JPopupMenu();

        try {
            // Access the static list of open TextEditor instances
            final Class<?> textEditorClass = Class.forName("org.scijava.ui.swing.script.TextEditor");
            final java.lang.reflect.Field instancesField = textEditorClass.getField("instances");
            @SuppressWarnings("unchecked")
            final java.util.List<Object> instances = (java.util.List<Object>) instancesField.get(null);

            if (instances.isEmpty()) {
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
                if (instances.size() > 1 || hasMultipleTabs(instances.get(0), textEditorClass)) {
                    menu.addSeparator();

                    for (final Object textEditor : instances) {
                        addScriptMenuItems(menu, textEditor, textEditorClass);
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

    private boolean hasMultipleTabs(final Object textEditor, final Class<?> textEditorClass) {
        try {
            final java.lang.reflect.Field tabbedField = textEditorClass.getDeclaredField("tabbed");
            tabbedField.setAccessible(true);
            final javax.swing.JTabbedPane tabbed = (javax.swing.JTabbedPane) tabbedField.get(textEditor);
            return tabbed.getTabCount() > 1;
        } catch (Exception e) {
            return false;
        }
    }

    private void addScriptMenuItems(final JPopupMenu menu, final Object textEditor, final Class<?> textEditorClass) {
        try {
            // Get the tabbed pane
            final java.lang.reflect.Field tabbedField = textEditorClass.getDeclaredField("tabbed");
            tabbedField.setAccessible(true);
            final javax.swing.JTabbedPane tabbed = (javax.swing.JTabbedPane) tabbedField.get(textEditor);
            final int tabCount = tabbed.getTabCount();

            // Get method to retrieve individual tabs
            final java.lang.reflect.Method getTabMethod = textEditorClass.getMethod("getTab", int.class);

            for (int i = 0; i < tabCount; i++) {
                final Object tab = getTabMethod.invoke(textEditor, i);
                final java.lang.reflect.Method getTitleMethod = tab.getClass().getMethod("getTitle");
                final String title = (String) getTitleMethod.invoke(tab);

                final int tabIndex = i;
                final JMenuItem item = new JMenuItem(title);
                item.addActionListener(e -> addScriptFromTab(textEditor, tabIndex, textEditorClass));
                menu.add(item);
            }
        } catch (Exception e) {
            // Skip this editor if we can't access its tabs
        }
    }

    private void addScriptFromTab(final Object textEditor, final int tabIndex, final Class<?> textEditorClass) {
        try {
            // Get the specific tab
            final java.lang.reflect.Method getTabMethod = textEditorClass.getMethod("getTab", int.class);
            final Object tab = getTabMethod.invoke(textEditor, tabIndex);

            // Get the title
            final java.lang.reflect.Method getTitleMethod = tab.getClass().getMethod("getTitle");
            final String scriptName = (String) getTitleMethod.invoke(tab);

            // Get the editor pane
            final java.lang.reflect.Field editorPaneField = tab.getClass().getDeclaredField("editorPane");
            editorPaneField.setAccessible(true);
            final Object editorPane = editorPaneField.get(tab);

            // Get the text content
            final java.lang.reflect.Method getTextMethod = editorPane.getClass().getMethod("getText");
            final String scriptContent = (String) getTextMethod.invoke(editorPane);

            // Get the language
            final java.lang.reflect.Method getLanguageMethod = editorPane.getClass().getMethod("getCurrentLanguage");
            final Object language = getLanguageMethod.invoke(editorPane);
            final String languageName = language != null ? language.toString().toLowerCase() : "unknown";

            // Add the script context
            final ScriptContextItem scriptItem = new ScriptContextItem(scriptName, scriptContent, languageName);
            addContextItem(scriptItem);

        } catch (Exception e) {
            appendToChat("Error", "Failed to add script from tab: " + e.getMessage());
        }
    }

    private void addContextItem(final ContextItem item) {
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

        contextTagsPanel.add(tagButton);
        contextTagsPanel.setVisible(true);
        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
    }

    private void removeContextItem(final ContextItem item, final JButton tagButton) {
        contextItems.remove(item);
        contextTagsPanel.remove(tagButton);

        if (contextItems.isEmpty()) {
            contextTagsPanel.setVisible(false);
        }

        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
    }
}
