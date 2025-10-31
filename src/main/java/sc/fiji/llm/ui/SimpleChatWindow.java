package sc.fiji.llm.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
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

    public SimpleChatWindow(final FijiAssistant assistant, final LLMContextService contextService, 
                            final CommandService commandService, final PrefService prefService, final String title) {
        this.assistant = assistant;
        this.contextService = contextService;
        this.commandService = commandService;
        this.prefService = prefService;

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

        // Button bar above input
        final JPanel buttonBar = new JPanel(new BorderLayout());
        
        // Left side - context buttons (placeholder for future)
        final JPanel leftButtons = new JPanel();
        leftButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        // Right side - action buttons
        final JPanel rightButtons = new JPanel();
        rightButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        
        final JButton changeModelButton = new JButton("Change Model");
        changeModelButton.addActionListener(e -> changeModel());
        rightButtons.add(changeModelButton);
        
        buttonBar.add(leftButtons, BorderLayout.WEST);
        buttonBar.add(rightButtons, BorderLayout.EAST);

        // Input panel
        final JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // Combined bottom panel with button bar and input
        final JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonBar, BorderLayout.NORTH);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);

        // Add components to frame
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Set up event handlers
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Finalize frame
        frame.pack();
        frame.setLocationRelativeTo(null);

        // Welcome message
        appendToChat("System", "Chat session started. Type your message and press Enter or click Send.");
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
                final String context = contextService.buildPluginContext();
                final String response = assistant.chat(context, userMessage);

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
}
