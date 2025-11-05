package sc.fiji.llm.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.scijava.command.CommandService;
import org.scijava.prefs.PrefService;
import org.scijava.thread.ThreadService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.request.ChatRequest;
import net.miginfocom.swing.MigLayout;
import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.chat.ContextItem;
import sc.fiji.llm.chat.ContextItemService;
import sc.fiji.llm.chat.ContextItemSupplier;
import sc.fiji.llm.chat.Conversation;
import sc.fiji.llm.chat.ConversationBuilder;
import sc.fiji.llm.commands.Fiji_Chat;
import sc.fiji.llm.tools.AiToolService;

/**
 * Swing-based chat window for chatting with LLMs in Fiji.
 */
public class FijiAssistantChat {
    public static final float CHAT_FONT_SIZE = 16f;
    private static final int INPUT_PANEL_PADDING = 8;
    private static final String PLACEHOLDER_TEXT = "Type your message here...";
    private static enum Sender {USER, ASSISTANT, SYSTEM, ERROR};

    private static final String SYSTEM_PROMPT = "You are an assistant chatbot running as an integrated plugin within the Fiji (ImageJ) application for scientific image analysis. " +
        "Your mission is to help users develop reproducible workflows (e.g. via scripts), answer their image analysis questions, and select the best tools for their data and goals. " +
        "Key elements of your persona: positive, validating, patient, encouraging, understanding.";

    // -- Contextual fields --
    private final CommandService commandService;
    private final PrefService prefService;
    private final ContextItemService contextItemSupplierService;
    private final ThreadService threadService;

    // -- Non-Contextual fields --
    private final FijiAssistant assistant;
    private final JFrame frame;
    private final JPanel chatPanel;
    private final JScrollPane chatScrollPane;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JPanel contextTagsPanel;
    private final JScrollPane contextTagsScrollPane;
    private final JButton clearAllButton;
    private final java.util.Map<ContextItem, JButton> contextItemButtons;
    private final Conversation conversation;

    public FijiAssistantChat(final FijiAssistant assistant, final String title, CommandService commandService, PrefService prefService, AiToolService aiToolService, ContextItemService contextItemService, ThreadService threadService, ChatbotService chatService) {
        this.assistant = assistant;
        this.commandService = commandService;
        this.prefService = prefService;
        this.threadService = threadService;
        this.contextItemSupplierService = contextItemService;

        this.contextItemButtons = new HashMap<>();

        this.conversation = new ConversationBuilder()
            .withBaseSystemMessage(SYSTEM_PROMPT)
            .withMessageFormatHint(chatService.messageFormatHint())
            .withTools(aiToolService.getInstances())
            .build();

        // Create the frame
        frame = new JFrame("Fiji Chat - " + title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top navigation bar with model selection
        final JPanel topNavBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        final JButton changeModelButton;
        final URL gearIconUrl = getClass().getResource("/icons/gear-noun-32.png");
        if (gearIconUrl != null) {
            changeModelButton = new JButton(new ImageIcon(gearIconUrl));
            changeModelButton.setPreferredSize(new Dimension(36, 36));
            changeModelButton.setToolTipText("Change Model");
        } else {
            changeModelButton = new JButton("Change Model");
        }
        changeModelButton.setFocusPainted(false);
        changeModelButton.addActionListener(e -> changeModel());
        topNavBar.add(changeModelButton);

        // Chat display area - MigLayout for proper resizing with messages at bottom
        chatPanel = new JPanel(new MigLayout(
            "fillx, wrap, insets 0",  // Fill horizontally, wrap each component to new row
            "[grow,fill]",             // Column grows and fills
            "[grow][][]"               // First row grows (pushes content down), then message rows
        ));
        chatPanel.setBackground(Color.WHITE);

        // Add a glue panel that will push messages to bottom
        final JPanel glue = new JPanel();
        glue.setOpaque(false);
        chatPanel.add(glue, "pushy, growy"); // This row grows vertically, pushing messages down

        // Add a bottom spacer for 8px padding at the end of messages
        final JPanel bottomSpacer = new JPanel();
        bottomSpacer.setOpaque(false);
        bottomSpacer.setPreferredSize(new Dimension(1, 8));
        bottomSpacer.setMinimumSize(new Dimension(1, 8));
        bottomSpacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        chatPanel.add(bottomSpacer, "growx, height 8!");

        chatScrollPane = new JScrollPane(chatPanel);
        chatScrollPane.setPreferredSize(new Dimension(600, 400));
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Button bar with context buttons - wrapped in outer panel with space reserved on right (matching contextTagsPanel structure)
        final JScrollPane suppliersScrollPane = createContextSelectorPanel();
        final JPanel buttonBar = new JPanel(new MigLayout("insets 0, fillx", "[grow,fill][shrink]", "[grow,fill]"));
        buttonBar.setOpaque(false);
        buttonBar.add(suppliersScrollPane, "growx, growy, pushy");

        // Add spacer on the right to match the width of the Clear All button (28px)
        final JPanel spacerPanel = new JPanel();
        spacerPanel.setOpaque(false);
        spacerPanel.setPreferredSize(new Dimension(28, 1));
        buttonBar.add(spacerPanel, "width 28!, aligny center");

        // Context tags panel (shows active context items as removable tags)
        // Create inner panel for tags that will wrap
        final JPanel tagsContainer = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                if (getParent() instanceof javax.swing.JViewport) {
                    int w = ((javax.swing.JViewport) getParent()).getWidth();
                    
                    // Calculate wrapped height manually
                    FlowLayout layout = (FlowLayout) getLayout();
                    int hgap = layout.getHgap();
                    int vgap = layout.getVgap();
                    
                    int maxWidth = w - (hgap * 2); // Account for horizontal gaps
                    int currentWidth = hgap;
                    int currentHeight = vgap;
                    int rowHeight = 0;
                    
                    for (java.awt.Component comp : getComponents()) {
                        Dimension d = comp.getPreferredSize();
                        
                        if (currentWidth + d.width > maxWidth && currentWidth > hgap) {
                            // Wrap to new row
                            currentHeight += rowHeight + vgap;
                            currentWidth = hgap;
                            rowHeight = 0;
                        }
                        
                        currentWidth += d.width + hgap;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                    
                    currentHeight += rowHeight + vgap; // Add final row height
                    
                    return new Dimension(w, currentHeight);
                }
                return super.getPreferredSize();
            }
        };
        tagsContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
        tagsContainer.setOpaque(false);
        tagsContainer.setToolTipText("Attached items will be included as context with your message");

        // Create scrollable container for tags
        contextTagsScrollPane = new JScrollPane(tagsContainer);
        contextTagsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contextTagsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contextTagsScrollPane.setPreferredSize(new Dimension(600, 36));
        contextTagsScrollPane.setMinimumSize(new Dimension(36, 36));
        contextTagsScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        
        // Apply light blue background to the scrollpane itself
        contextTagsScrollPane.setBackground(new java.awt.Color(240, 248, 255)); // Light blue background
        contextTagsScrollPane.getViewport().setBackground(new java.awt.Color(240, 248, 255));
        contextTagsScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(200, 220, 240), 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0) // No internal padding
        ));

        // Create the outer panel that always shows
        contextTagsPanel = new JPanel(new MigLayout("insets 0, fillx", "[grow,fill][shrink]", "[grow,fill]"));
        contextTagsPanel.setOpaque(false);

        // Add scrollable tags area in the center
        contextTagsPanel.add(contextTagsScrollPane, "growx, growy, pushy");

        // Add "Clear All" button on the right (icon button, 28x28 to match send button)
        final URL closeIconUrl = getClass().getResource("/icons/close-20.png");
        if (closeIconUrl != null) {
            clearAllButton = new JButton(new ImageIcon(closeIconUrl));
        } else {
            clearAllButton = new JButton("✕");
        }

        clearAllButton.setToolTipText("Clear all context items");
        clearAllButton.setFocusPainted(false);
        clearAllButton.setEnabled(false); // Disabled until context items are added
        clearAllButton.setForeground(java.awt.Color.RED);
        clearAllButton.setFont(clearAllButton.getFont().deriveFont(14f));
        clearAllButton.addActionListener(e -> clearAllContext());
        contextTagsPanel.add(clearAllButton, "aligny center, height 28!");

        // Input area - scrollable and resizable
        inputArea = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setColor(Color.GRAY);
                    g.setFont(new Font("Dialog", Font.PLAIN, 14));
                    g.drawString(PLACEHOLDER_TEXT, 5, 20);
                }
            }
        };

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(inputArea.getFont().deriveFont(CHAT_FONT_SIZE));
        final JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Add KeyListener to handle Enter key (Shift+Enter for newline)
        inputArea.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        // Send button - styled to look integrated
        final URL iconUrl = getClass().getResource("/icons/send-20.png");
        if (iconUrl != null) {
            sendButton = new JButton(new ImageIcon(iconUrl));
        } else {
            sendButton = new JButton("Send");
        }
        sendButton.setFocusPainted(false);
        sendButton.setToolTipText("Send message");

        // Input panel with button bar and input area
        final JPanel inputPanel = new JPanel(new MigLayout("insets 0, fillx, filly", "[grow,fill][shrink]", "[grow,fill]"));
        inputPanel.add(inputScrollPane, "growx, growy, pushy");
        inputPanel.add(sendButton, "aligny bottom, height 28!");

        // Bottom panel combining context tags, button bar, and input
        final JPanel bottomPanel = new JPanel(new MigLayout("fillx, wrap, insets 0 0 " + INPUT_PANEL_PADDING + " " + INPUT_PANEL_PADDING + ", gapy " + INPUT_PANEL_PADDING, "[grow,fill]", "[][][grow,fill]"));
        bottomPanel.add(buttonBar, "growx, wrap");
        bottomPanel.add(contextTagsPanel, "growx, wrap");
        bottomPanel.add(inputPanel, "growx, growy, pushy, grow");

        // Create a split pane with vertical divider between chat and input
        final JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            chatScrollPane,
            bottomPanel
        );
        splitPane.setDividerLocation(0.7); // 70% for chat, 30% for input initially
        splitPane.setResizeWeight(1.0); // Extra space goes to the top (chat area)
        splitPane.setContinuousLayout(true); // Smooth resizing

        // Add components to frame
        frame.add(topNavBar, BorderLayout.NORTH);
        frame.add(splitPane, BorderLayout.CENTER);

        // Set up event handlers
        sendButton.addActionListener(e -> sendMessage());

        // Finalize frame
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
        inputArea.requestFocus();
    }

    /**
     * Generic selector panel that builds a button + dropdown for each
     * registered ContextItemSupplier. Creates a wrappable, scrollable
     * panel similar to the context tags panel.
     */
    private JScrollPane createContextSelectorPanel() {
        // Create wrapping container similar to context tags
        final JPanel container = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                if (getParent() instanceof javax.swing.JViewport) {
                    int w = ((javax.swing.JViewport) getParent()).getWidth();
                    
                    // Calculate wrapped height manually
                    FlowLayout layout = (FlowLayout) getLayout();
                    int hgap = layout.getHgap();
                    int vgap = layout.getVgap();
                    
                    int maxWidth = w - (hgap * 2);
                    int currentWidth = hgap;
                    int currentHeight = vgap;
                    int rowHeight = 0;
                    
                    for (java.awt.Component comp : getComponents()) {
                        Dimension d = comp.getPreferredSize();
                        
                        if (currentWidth + d.width > maxWidth && currentWidth > hgap) {
                            // Wrap to new row
                            currentHeight += rowHeight + vgap;
                            currentWidth = hgap;
                            rowHeight = 0;
                        }
                        
                        currentWidth += d.width + hgap;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                    
                    currentHeight += rowHeight + vgap;
                    
                    return new Dimension(w, currentHeight);
                }
                return super.getPreferredSize();
            }
        };
        container.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
        container.setOpaque(false);

        try {
            final List<ContextItemSupplier> suppliers = contextItemSupplierService.getInstances();

            if (suppliers == null || suppliers.isEmpty()) {
                // No suppliers available - return empty scrollpane
                final JScrollPane scrollPane = new JScrollPane(container);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setPreferredSize(new Dimension(600, 45));
                scrollPane.setMinimumSize(new Dimension(45, 45));
                scrollPane.getVerticalScrollBar().setUnitIncrement(5);
                scrollPane.setBorder(null);
                scrollPane.setOpaque(false);
                scrollPane.getViewport().setOpaque(false);

                return scrollPane;
            }

            for (final ContextItemSupplier supplier : suppliers) {
                final String displayName = supplier.getDisplayName();

                // Create a unit panel for this supplier (button + dropdown + label)
                final JPanel unitPanel = new JPanel(new BorderLayout(0, 1));
                unitPanel.setOpaque(false);

                // Top part: buttons (main + dropdown)
                final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                buttonsPanel.setOpaque(false);

                // Main button (36x36 with icon or text)
                final JButton contextItemButton;
                final ImageIcon supplierIcon = supplier.getIcon();
                if (supplierIcon != null) {
                    contextItemButton = new JButton(supplierIcon);
                    contextItemButton.setPreferredSize(new Dimension(36, 36));
                    contextItemButton.setToolTipText("Attach active " + displayName);
                    contextItemButton.setFocusPainted(false);
                } else {
                    final String iconText = displayName.length() > 0 ? displayName.substring(0, 1) : "?";
                    contextItemButton = new JButton(iconText);
                    contextItemButton.setPreferredSize(new Dimension(36, 36));
                    contextItemButton.setToolTipText("Attach active " + displayName);
                    contextItemButton.setFont(contextItemButton.getFont().deriveFont(14f));
                    contextItemButton.setFocusPainted(false);
                }
                
                // Add darker outer border and lighter right divider
                final Color darkBorder = Color.GRAY;
                final Color lightDivider = new Color(200, 200, 200);
                contextItemButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, darkBorder),
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, lightDivider),
                        BorderFactory.createEmptyBorder(2, 2, 2, 1)
                    )
                ));

                // Dropdown button (smaller, 20x36 to match height)
                final JButton dropdownButton = new JButton("▼");
                dropdownButton.setPreferredSize(new Dimension(20, 36));
                dropdownButton.setToolTipText("Select " + displayName + " to attach as context");
                dropdownButton.setFont(dropdownButton.getFont().deriveFont(12f));
                dropdownButton.setFocusPainted(false);
                
                // Add darker outer border and lighter left divider
                dropdownButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 1, 1, darkBorder),
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0, lightDivider),
                        BorderFactory.createEmptyBorder(2, 1, 2, 2)
                    )
                ));

                // Main action: create active context item via supplier
                contextItemButton.addActionListener(e -> {
                    threadService.run(() -> {
                        try {
                            final ContextItem item = supplier.createActiveContextItem();
                            if (item != null) {
                                addContextItem(item);
                            } else {
                                appendToChat(Sender.ERROR, "No active " + displayName + " available");
                            }
                        } catch (Exception ex) {
                            appendToChat(Sender.ERROR, "Failed to create active " + displayName + ": " + ex.getMessage());
                        }
                    });
                });

                // Dropdown: list available items from supplier
                dropdownButton.addActionListener(e -> {
                    final JPopupMenu menu = new JPopupMenu();
                    try {
                        final List<ContextItem> available = supplier.listAvailable();
                        if (available == null || available.isEmpty()) {
                            final JMenuItem none = new JMenuItem("(none)");
                            none.setEnabled(false);
                            menu.add(none);
                        } else {
                            for (final ContextItem it : available) {
                                final JMenuItem mi = new JMenuItem(it.getLabel());
                                mi.addActionListener(ae -> addContextItem(it));
                                menu.add(mi);
                            }
                        }
                    } catch (Exception ex) {
                        final JMenuItem err = new JMenuItem("(not available)");
                        err.setEnabled(false);
                        menu.add(err);
                    }

                    menu.show(dropdownButton, 0, dropdownButton.getHeight());
                });

                buttonsPanel.add(contextItemButton);
                buttonsPanel.add(dropdownButton);

                // Bottom part: label
                final JLabel label = new JLabel(displayName + "s", SwingConstants.CENTER);
                label.setFont(label.getFont().deriveFont(11f));

                unitPanel.add(buttonsPanel, BorderLayout.NORTH);
                unitPanel.add(label, BorderLayout.SOUTH);

                container.add(unitPanel);
            }

        } catch (Exception e) {
            // If the supplier service fails, show nothing
        }

        // Wrap in scrollpane
        final JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(600, 60)); // Taller to accommodate buttons + labels
        scrollPane.setMinimumSize(new Dimension(60, 60)); // Taller to accommodate buttons + labels
        scrollPane.getVerticalScrollBar().setUnitIncrement(5);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        return scrollPane;
    }    private void sendMessage() {
        final String userMessage = inputArea.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        // Add message to conversation synchronously on EDT before spawning background thread
        conversation.addUserMessage(userMessage);

        // Display user message (this will use invokeLater but message is already in conversation)
        appendToChat(Sender.USER, userMessage);

        inputArea.setText("");
        inputArea.setEnabled(false);
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
                    inputArea.setEnabled(true);
                    sendButton.setEnabled(true);
                    inputArea.requestFocus();
                });
            }
        }).start();
    }

    private void appendToChat(final Sender sender, final String message) {
        if (message == null) return;

        // Always use invokeLater since this can be called from both EDT and background threads
        SwingUtilities.invokeLater(() -> {
            // Convert Sender enum to MessageType
            final ChatMessagePanel.MessageType messageType = switch (sender) {
                case USER -> ChatMessagePanel.MessageType.USER;
                case ASSISTANT -> ChatMessagePanel.MessageType.ASSISTANT;
                case SYSTEM -> ChatMessagePanel.MessageType.SYSTEM;
                case ERROR -> ChatMessagePanel.MessageType.ERROR;
            };

            // Create and add message panel
            final ChatMessagePanel messagePanel = new ChatMessagePanel(messageType, message, CHAT_FONT_SIZE);
            
            // Remove the glue and bottom spacer, add message, re-add glue and spacer to keep messages at bottom
            final int componentCount = chatPanel.getComponentCount();
            if (componentCount >= 2) {
                final java.awt.Component glue = chatPanel.getComponent(0);
                final java.awt.Component bottomSpacer = chatPanel.getComponent(componentCount - 1);
                chatPanel.remove(0); // Remove glue
                chatPanel.remove(componentCount - 2); // Remove bottom spacer (index shifts after first removal)
                chatPanel.add(messagePanel, "growx"); // Grow horizontally only
                chatPanel.add(glue, "pushy, growy", 0); // Re-add glue at top (index 0)
                chatPanel.add(bottomSpacer, "growx, height 8!"); // Re-add bottom spacer at end
            } else {
                chatPanel.add(messagePanel, "growx");
            }
            
            chatPanel.revalidate();
            chatPanel.repaint();

            // Scroll to bottom
            SwingUtilities.invokeLater(() -> {
                final javax.swing.JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

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
        prefService.remove(Fiji_Chat.class, Fiji_Chat.LAST_CHAT_MODEL);

        // Close this chat window
        frame.dispose();

        // Re-invoke the Fiji_Chat command to show the selection dialog
        commandService.run(Fiji_Chat.class, true);
    }

    private void addContextItem(final ContextItem item) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addContextItem(item));
            return;
        }
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

        // Truncate label to max length
        final int maxLabelLength = 20;
        String displayLabel = item.getLabel();
        if (displayLabel.length() > maxLabelLength) {
            displayLabel = displayLabel.substring(0, maxLabelLength - 1) + "…";
        }
        displayLabel = "[" + item.getType() + "] " + displayLabel;

        // Create a removable tag button with truncated label and X
        final JButton tagButton = new JButton(displayLabel + " ✕");

        // Build tooltip
        String tooltipText = item.getLabel() + " - Click to remove";
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

        // Get the tags container from the scrollpane's viewport
        JPanel tagsContainer = (JPanel) contextTagsScrollPane.getViewport().getView();
        tagsContainer.add(tagButton);

        // Enable the Clear All button now that we have context items
        clearAllButton.setEnabled(true);
        contextTagsPanel.revalidate();
        contextTagsPanel.repaint();
    }

    private void removeContextItem(final ContextItem item, final JButton tagButton) {
        conversation.removeContextItem(item);
        contextItemButtons.remove(item);
        final JPanel tagsContainer = (JPanel) contextTagsScrollPane.getViewport().getView();
        tagsContainer.remove(tagButton);

        // Disable the Clear All button if no more context items
        if (conversation.getContextItems().isEmpty()) {
            clearAllButton.setEnabled(false);
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
        final JPanel tagsContainer = (JPanel) contextTagsScrollPane.getViewport().getView();
        tagsContainer.removeAll();
        clearAllButton.setEnabled(false);
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
