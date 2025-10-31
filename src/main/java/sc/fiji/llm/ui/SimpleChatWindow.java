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

import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.service.LLMContextService;

/**
 * Simple Swing-based chat window for LLM chat.
 */
public class SimpleChatWindow {
    private final FijiAssistant assistant;
    private final LLMContextService contextService;
    private final JFrame frame;
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;

    public SimpleChatWindow(final FijiAssistant assistant, final LLMContextService contextService, final String title) {
        this.assistant = assistant;
        this.contextService = contextService;

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

        // Input panel
        final JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Add components to frame
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

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
}
