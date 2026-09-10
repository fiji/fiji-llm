/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;
import org.scijava.thread.ThreadService;

import com.google.gson.JsonArray;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import net.miginfocom.swing.MigLayout;
import sc.fiji.llm.assistant.AssistantService;
import sc.fiji.llm.assistant.FijiAssistant;
import sc.fiji.llm.chat.Conversation;
import sc.fiji.llm.chat.ConversationService;
import sc.fiji.llm.commands.Fiji_Chat;
import sc.fiji.llm.commands.Manage_Keys;
import sc.fiji.llm.context.ContextItem;
import sc.fiji.llm.provider.LLMProvider;
import sc.fiji.llm.provider.ProviderService;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.AiToolService;

/**
 * Swing-based chat window for chatting with LLMs in Fiji.
 */
public class FijiAssistantChat {

	public static final float CHAT_FONT_SIZE = 16f;
	private static final int INPUT_PANEL_PADDING = 8;
	private static final int CONFIGURE_CHAT_TEXT_WIDTH = 72;
	private static final int CONFIGURE_CHAT_BUTTON_WIDTH = CONFIGURE_CHAT_TEXT_WIDTH + 42;
	private static final String PLACEHOLDER_TEXT = "Type your message here...";
	private static final String GUIDE_SHOWN_PREF = "guideShown";

	private static enum Sender {
			USER, ASSISTANT, SYSTEM, ERROR
	};

	private static final String SYSTEM_PROMPT =
		"You are a chatbot running in the Fiji (ImageJ) application for scientific image analysis. " +
			"Your role is to help users develop reproducible workflows (e.g. via scripts or macros) and direct them to tools based on their needs. " +
			"You are concise, humble, validating, patient, and understanding. Expect to make mistakes: troubleshoot and iterate.";

	// -- Contextual fields --
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

	@Parameter
	private LogService logService;

	// -- Non-Contextual fields --
	private FijiAssistant assistant;
	private final JFrame frame;
	private final JPanel chatPanel;
	private final JScrollPane chatScrollPane;
	private final JTextArea inputArea;
	private final JButton sendStopButton;
	private final JPanel contextTagsPanel;
	private final JScrollPane contextTagsScrollPane;
	private final JButton attachContextButton;
	private final JButton clearAllButton;
	private final JLabel contextPlaceholderLabel;
	private final java.util.Map<ContextItem, JButton> contextItemButtons;
	private final JButton guideButton;
	private final List<ContextItem> contextItems;
	private JComboBox<String> conversationComboBox;
	private JButton newConversationButton;
	private JButton deleteConversationButton;
	private volatile boolean stopRequested = false;
	private boolean isSendMode = true;
	private boolean modelReady = false;
	private ImageIcon sendIcon;
	private ImageIcon stopIcon;
	private InteractiveGuide guide;
	private ChatMessagePanel preparationPanel;
	private javax.swing.Timer preparationTimer;
	private LLMProvider llmProvider;
	private final String modelName;
	private Conversation currentConversation;
	private final ChatRequestParameters requestParameters;

	public FijiAssistantChat(Context c, String providerName, String modelName)
	{
		c.inject(this);

		this.contextItemButtons = new HashMap<>();
		this.contextItems = new ArrayList<>();
		this.llmProvider = providerService.getProvider(providerName);
		this.modelName = modelName;

		// Create default request parameters
		requestParameters = llmProvider.defaultChatRequestParameters();

		// Create the frame
		frame = new JFrame("Fiji Chat");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		// Top navigation bar with model selection
		final JPanel topNavBar = new JPanel(new BorderLayout());

		// Conversation selector panel (added first so it appears on the left)
		final JPanel conversationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,
			3, 12));
		conversationPanel.setOpaque(false);

		// For difference in hgaps
		final JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		spacer.setPreferredSize(new Dimension(2, 1));
		conversationPanel.add(spacer);

		conversationComboBox = new JComboBox<>();
		int prefHeight = conversationComboBox.getPreferredSize().height;
		conversationComboBox.setPreferredSize(new Dimension(280, prefHeight));
		conversationComboBox.setRenderer(new DefaultListCellRenderer() {

			@Override
			public java.awt.Component getListCellRendererComponent(
				final JList<?> list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus)
			{
				final Object displayValue = index == -1 && value == null ?
					"<no conversation>" : value;
				return super.getListCellRendererComponent(list, displayValue, index,
					isSelected, cellHasFocus);
			}
		});

		conversationComboBox.setToolTipText("Load a previous conversation");
		conversationService.getConversationNames().stream().forEach(
			conversationComboBox::addItem);
		conversationComboBox.setSelectedIndex(-1);
		conversationComboBox.addActionListener(e -> onConversationSelected());
		conversationPanel.add(new JLabel("Conversation:"));
		conversationPanel.add(conversationComboBox);

		newConversationButton = new JButton("+");
		newConversationButton.setPreferredSize(new Dimension(prefHeight,
			prefHeight));
		newConversationButton.setFocusPainted(false);
		newConversationButton.setToolTipText("Start a new conversation");
		newConversationButton.setEnabled(false);
		newConversationButton.addActionListener(e -> clearConversation());
		conversationPanel.add(newConversationButton);

		final URL trashIconUrl = getClass().getResource(
			"/icons/trash-noun-20.png");
		if (trashIconUrl != null) {
			deleteConversationButton = new JButton(new ImageIcon(trashIconUrl));
		} else {
			deleteConversationButton = new JButton("-");
		}
		deleteConversationButton.setPreferredSize(new Dimension(prefHeight,
			prefHeight));
		deleteConversationButton.setFocusPainted(false);
		deleteConversationButton.setToolTipText(
			"Delete the current conversation permanently");
		deleteConversationButton.setEnabled(false);
		deleteConversationButton.setForeground(java.awt.Color.RED);
		deleteConversationButton.addActionListener(
			e -> deleteCurrentConversation());
		conversationPanel.add(deleteConversationButton);

		topNavBar.add(conversationPanel, BorderLayout.WEST);

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5,
			5));
		buttonPanel.setOpaque(false);
		// ImageSC Forum button
		final JButton forumButton;
		final URL forumIconUrl = getClass().getResource(
			"/icons/imagesc-icon-32.png");
		if (forumIconUrl != null) {
			forumButton = new JButton(new ImageIcon(forumIconUrl));
			forumButton.setPreferredSize(new Dimension(36, 36));
		}
		else {
			forumButton = new JButton("Forum");
		}
		forumButton.setFocusPainted(false);
		forumButton.setToolTipText("Get help on the Image.sc forum");
		forumButton.addActionListener(e -> openForumInBrowser());
		buttonPanel.add(forumButton);

		// Change API Keys button
		final JButton configureKeysButton;
		final URL lockIconUrl = getClass().getResource("/icons/key-pao-32.png");
		if (lockIconUrl != null) {
			configureKeysButton = new JButton(new ImageIcon(lockIconUrl));
			configureKeysButton.setPreferredSize(new Dimension(36, 36));
			configureKeysButton.setToolTipText("Configure API Key");
		}
		else {
			configureKeysButton = new JButton("Configure API Key");
		}
		configureKeysButton.setFocusPainted(false);
		configureKeysButton.addActionListener(e -> configureKeys());
		buttonPanel.add(configureKeysButton);

		configureKeysButton.setEnabled(llmProvider.requiresApiKey());

		// Change Model button
		final JButton configureChatButton;
		final URL gearIconUrl = getClass().getResource("/icons/gear-noun-32.png");
		if (gearIconUrl != null) {
			configureChatButton = new JButton(new ImageIcon(gearIconUrl));
		}
		else {
			configureChatButton = new JButton();
		}
		configureChatButton.setText(buildConfigureChatButtonText(providerName,
			modelName, llmProvider.supportsModelSelection(), configureChatButton
			.getFont()));
		configureChatButton.setPreferredSize(new Dimension(
			CONFIGURE_CHAT_BUTTON_WIDTH, 36));
		configureChatButton.setMinimumSize(configureChatButton.getPreferredSize());
		configureChatButton.setMaximumSize(configureChatButton.getPreferredSize());
		configureChatButton.setHorizontalAlignment(SwingConstants.LEFT);
		configureChatButton.setHorizontalTextPosition(SwingConstants.RIGHT);
		configureChatButton.setVerticalTextPosition(SwingConstants.CENTER);
		configureChatButton.setIconTextGap(6);
		configureChatButton.setMargin(new Insets(0, 0, 0, 0));
		configureChatButton.setToolTipText(buildConfigureChatTooltip(providerName,
			modelName, llmProvider.getDescription()));
		configureChatButton.setFocusPainted(false);
		configureChatButton.addActionListener(e -> configureChat());
		buttonPanel.add(configureChatButton);

		// Launch guide button
		final URL questionIconUrl = getClass().getResource(
			"/icons/question-icon-32.png");
		if (questionIconUrl != null) {
			guideButton = new JButton(new ImageIcon(questionIconUrl));
			guideButton.setPreferredSize(new Dimension(36, 36));
		}
		else {
			guideButton = new JButton("Show Guide");
		}
		guideButton.setToolTipText("Explain chat componenets");
		guideButton.setFocusPainted(false);
		guideButton.addActionListener(e -> launchGuide());
		buttonPanel.add(guideButton);

		topNavBar.add(buttonPanel, BorderLayout.EAST);

		// Chat display area - MigLayout for proper resizing with messages at bottom
		// Fill horizontally, wrap each component to new row
		chatPanel = new JPanel(new MigLayout("fillx, wrap, insets 0", "[grow,fill]", // Column
			// grows
			// and
			// fills
			"[grow][][]" // First row grows (pushes content down), then message rows
		));
		chatPanel.setBackground(Color.WHITE);

		// Add a glue panel that will push messages to bottom
		final JPanel glue = new JPanel();
		glue.setOpaque(false);
		// This row grows vertically, pushing messages down
		chatPanel.add(glue, "pushy, growy");

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
		tagsContainer.setToolTipText(
			"Attached items will be included as context with your message");
		contextPlaceholderLabel = new JLabel("No context attached");
		contextPlaceholderLabel.setForeground(Color.GRAY);
		contextPlaceholderLabel.setFont(contextPlaceholderLabel.getFont().deriveFont(
			14f));
		tagsContainer.add(contextPlaceholderLabel);

		// Create scrollable container for tags
		contextTagsScrollPane = new JScrollPane(tagsContainer);
		contextTagsScrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		contextTagsScrollPane.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		contextTagsScrollPane.setPreferredSize(new Dimension(600, 36));
		contextTagsScrollPane.setMinimumSize(new Dimension(36, 36));
		contextTagsScrollPane.getVerticalScrollBar().setUnitIncrement(5);

		// Apply light blue background to the scrollpane itself
		contextTagsScrollPane.setBackground(new java.awt.Color(240, 248, 255)); // Light
		// blue
		// background
		contextTagsScrollPane.getViewport().setBackground(new java.awt.Color(240,
			248, 255));
		contextTagsScrollPane.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new java.awt.Color(200, 220, 240), 1),
			BorderFactory.createEmptyBorder(0, 0, 0, 0) // No internal padding
		));

		// Create the outer panel that always shows
		contextTagsPanel = new JPanel(new MigLayout("insets 0, fillx",
			"[shrink][grow,fill][shrink]", "[grow,fill]"));
		contextTagsPanel.setOpaque(false);

		final URL attachIconUrl = getClass().getResource(
			"/icons/attach-noun-20.png");
		if (attachIconUrl != null) {
			attachContextButton = new JButton(new ImageIcon(attachIconUrl));
		}
		else {
			attachContextButton = new JButton("+");
		}
		attachContextButton.setPreferredSize(new Dimension(28, 28));
		attachContextButton.setToolTipText("Attach context");
		attachContextButton.setFocusPainted(false);
		attachContextButton.setMargin(new Insets(0, 0, 0, 0));
		attachContextButton.addActionListener(e -> showContextMenu(
			attachContextButton));
		contextTagsPanel.add(attachContextButton, "aligny center, width 28!, height 28!");

		// Add scrollable tags area in the center
		contextTagsPanel.add(contextTagsScrollPane, "growx, growy, pushy");

		// Add "Clear All" button on the right (icon button, 28x28 to match send
		// button)
		final URL closeIconUrl = getClass().getResource("/icons/close-20.png");
		if (closeIconUrl != null) {
			clearAllButton = new JButton(new ImageIcon(closeIconUrl));
		}
		else {
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
					((Graphics2D) g).setRenderingHint(
						RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
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
		inputScrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		inputScrollPane.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		// Add KeyListener to handle Enter key (Shift+Enter and Alt+Enter for newline)
		inputArea.addKeyListener(new java.awt.event.KeyAdapter() {

			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
					if (e.isShiftDown() || e.isAltDown()) {
						// Shift+Enter or Alt+Enter: insert newline explicitly
						e.consume();
						inputArea.insert("\n", inputArea.getCaretPosition());
					}
					else {
						// Plain Enter: submit message
						e.consume();
						sendMessage();
					}
				}
			}
		});

		// Send button - styled to look integrated
		final URL iconUrl = getClass().getResource("/icons/send-20.png");
		if (iconUrl != null) {
			sendIcon = new ImageIcon(iconUrl);
		}
		sendIcon = sendIcon != null ? sendIcon : null; // Ensure it's initialized

		// Stop button - styled to look integrated, initially hidden
		final URL stopIconUrl = getClass().getResource("/icons/stop-noun-20.png");
		if (stopIconUrl != null) {
			stopIcon = new ImageIcon(stopIconUrl);
		}
		stopIcon = stopIcon != null ? stopIcon : null; // Ensure it's initialized

		// Single send/stop button that toggles based on mode
		sendStopButton = new JButton();
		sendStopButton.setFocusPainted(false);
		setSendMode(); // Start in send mode
		sendStopButton.addActionListener(e -> {
			if (isSendMode) {
				sendMessage();
			}
			else {
				requestStop();
			}
		});

		// Input panel with button bar and input area
		final JPanel inputPanel = new JPanel(new MigLayout("insets 0, fillx, filly",
			"[grow,fill][shrink]", "[grow,fill]"));
		inputPanel.add(inputScrollPane, "growx, growy, pushy");
		inputPanel.add(sendStopButton, "aligny bottom, height 28!");

		// Bottom panel combining context tags and input
		final JPanel bottomPanel = new JPanel(new MigLayout(
			"fillx, wrap, insets 0 " + INPUT_PANEL_PADDING + " " +
				INPUT_PANEL_PADDING + ", gapy " + INPUT_PANEL_PADDING, "[grow,fill]",
			"[][grow,fill]"));
		bottomPanel.add(contextTagsPanel, "growx, wrap");
		bottomPanel.add(inputPanel, "growx, growy, pushy, grow");

		// Create a split pane with vertical divider between chat and input
		final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
			chatScrollPane, bottomPanel);
		splitPane.setDividerLocation(0.7); // 70% for chat, 30% for input initially
		splitPane.setResizeWeight(1.0); // Extra space goes to the top (chat area)
		splitPane.setContinuousLayout(true); // Smooth resizing

		// Add components to frame
		frame.add(topNavBar, BorderLayout.NORTH);
		frame.add(splitPane, BorderLayout.CENTER);

		// Finalize frame
		frame.pack();
		frame.setLocationRelativeTo(null);

		// Build the interactive guide (items will be displayed in order)
		this.guide = new InteractiveGuide(frame);
		guide.addElement(inputArea, "Chat Input",
			"Type your message here and press 'enter' to chat with the AI assistant.");
		guide.addElement(sendStopButton, "Send / Stop Button",
			"Click to send your message, or to interrupt the assistant while it's responding.");
		guide.addElement(attachContextButton, "Attach Context",
			"Attach the current item or choose an available item as chat context.");
		guide.addElement(contextTagsScrollPane, "Context Items",
			"Currently attached context items are shown here. Click on an item to remove it.");
		guide.addElement(clearAllButton, "Clear Context",
			"Remove all currently attached context items.");
		guide.addElement(conversationComboBox, "Select Conversation",
			"Re-load a previous conversation.");
		guide.addElement(newConversationButton, "New Conversation",
			"Start a new conversation with the current chat model.");
		guide.addElement(deleteConversationButton, "Delete Conversation",
			"Permanently delete the current conversation.");
		guide.addElement(forumButton, "Forum Button",
			"Get help and support on the Image.sc forum.");
		guide.addElement(configureKeysButton, "API Key Button",
			"Configure API credentials for the active AI service.");
		guide.addElement(configureChatButton, "Configure Chat Button",
			"Select a different AI service or model.");

		startModelPreparation();
	}

	public void show() {
		frame.setVisible(true);
		if (modelReady) {
			inputArea.requestFocus();
		}
		if (!prefService.getBoolean(FijiAssistantChat.class, GUIDE_SHOWN_PREF,
			false))
		{
			guide.showSingleton(guideButton, "Welcome to Fiji chat!",
				"Click here for a brief tour.");
			prefService.put(FijiAssistantChat.class, GUIDE_SHOWN_PREF, true);
		}
	}

	private void showContextMenu(final JButton attachButton) {
		final JPopupMenu menu = new JPopupMenu();
		final List<ContextItemSupplier> suppliers;
		try {
			suppliers = contextItemService.getInstances();
		}
		catch (Exception e) {
			final JMenuItem unavailable = new JMenuItem("Context unavailable");
			unavailable.setEnabled(false);
			menu.add(unavailable);
			menu.show(attachButton, 0, attachButton.getHeight());
			return;
		}

		if (suppliers == null || suppliers.isEmpty()) {
			final JMenuItem unavailable = new JMenuItem("No context providers");
			unavailable.setEnabled(false);
			menu.add(unavailable);
		}
		else {
			for (final ContextItemSupplier supplier : suppliers) {
				final String displayName = supplier.getDisplayName();
				final JMenuItem currentItem = new JMenuItem("Attach current " +
					displayName, supplier.getIcon());
				boolean currentItemAvailable = false;
				try {
					currentItemAvailable = supplier.createActiveContextItem() != null;
				}
				catch (Exception e) {
				}
				currentItem.setEnabled(currentItemAvailable);
				if (currentItemAvailable) {
					currentItem.addActionListener(e -> addCurrentContextItem(supplier));
				}
				else {
					currentItem.setToolTipText("No " + displayName +
						" is currently available");
				}
				menu.add(currentItem);
			}

			menu.addSeparator();

			for (final ContextItemSupplier supplier : suppliers) {
				final String displayName = supplier.getDisplayName();
				final JMenu chooseMenu = new JMenu("Choose " + displayName);
				chooseMenu.setIcon(supplier.getIcon());
				try {
					final Set<ContextItem> available = supplier.listAvailable();
					if (available == null || available.isEmpty()) {
						final JMenuItem none = new JMenuItem("(none)");
						none.setEnabled(false);
						chooseMenu.add(none);
					}
					else {
						for (final ContextItem item : available) {
							final JMenuItem itemMenu = new JMenuItem(item.getLabel());
							itemMenu.addActionListener(e -> addContextItem(item,
								supplier));
							chooseMenu.add(itemMenu);
						}
					}
				}
				catch (Exception e) {
					final JMenuItem unavailable = new JMenuItem("(not available)");
					unavailable.setEnabled(false);
					chooseMenu.add(unavailable);
				}
				menu.add(chooseMenu);
			}
		}

		menu.show(attachButton, 0, attachButton.getHeight());
	}

	private void addCurrentContextItem(final ContextItemSupplier supplier) {
		final String displayName = supplier.getDisplayName();
		threadService.run(() -> {
			try {
				final ContextItem item = supplier.createActiveContextItem();
				if (item != null) {
					addContextItem(item, supplier);
				}
			}
			catch (Exception e) {
				appendToChat(Sender.ERROR, "Failed to create active " + displayName +
					": " + e.getMessage());
			}
		});
	}

	/**
	 * Send the current chat contents to the LLM. Must run on EDT
	 */
	private void sendMessage() {
		if (!modelReady) {
			return;
		}

		final String userText = inputArea.getText().trim();
		if (userText.isEmpty()) {
			return;
		}

		inputArea.setText(""); // Clear input immediately

		StringBuilder displayMessage = new StringBuilder(userText);
		List<ContextItem> mergedContextItems = mergeContextItems(contextItems);
		final JsonArray userContextArray = new JsonArray();

		// Add context item notes to the user message
		// Collect merged context items to JsonArray
		if (!mergedContextItems.isEmpty()) {
			displayMessage.append("\n").append("```").append("\n");

			for (final ContextItem item : mergedContextItems) {
				displayMessage.append(item.getLabel()).append("\n");
				userContextArray.add(item.toJson());
			}
			displayMessage.append("```");
		}

		// Add user message panel
		addMessagePanelToChat(ChatMessagePanel.MessageType.USER, displayMessage.toString());
		clearAllContextButtons();

		// Add empty assistant message panel for streaming
		final ChatMessagePanel currentStreamingPanel = createEmptyAssistantMessagePanel();

		// Switch to stop mode
		setStopMode();
		final AtomicBoolean aiMessageStarted = new AtomicBoolean();
		final AtomicBoolean cancelConversation = new AtomicBoolean();
		final int updateDelay = 200;

		// Process chat in background thread (LLM calls happen OFF the EDT)
		Future<?> msgThread = threadService.run(() -> {
			try {
				// If this is the first message in a new conversation, auto-name it
				if (currentConversation == null) {
					createNewConversation(userText, cancelConversation);
				}
				if (currentConversation == null) {
					// Message was canceled before the conversation was created
					aiMessageStarted.set(true);
					removeChatBubble(currentStreamingPanel);
					return;
				}

				final long[] lastScrollTime = {System.currentTimeMillis()};
				// Build user message with context items as attributes
				final UserMessage.Builder msgBuilder = UserMessage.builder()
						.addContent(new TextContent(userText));

				Map<String, Object> attributes = new HashMap<>();

				// Attach user context from user
				if (!mergedContextItems.isEmpty()) {
					attributes.put("context:from_user", userContextArray);
				}


				if (!attributes.isEmpty()) {
					msgBuilder.attributes(attributes);
				}

				final UserMessage userMsg = msgBuilder.build();

				// Save user message to conversation history
				currentConversation.addMessage(displayMessage.toString(), userMsg);

				// Build a chat request for the LLM
				final ChatRequest chatRequest = ChatRequest.builder()
						.messages(userMsg)
						.build();

				// Send user message to the LLM to initiate chat
				assistant.chatStreaming(chatRequest)
					.beforeToolExecution(aiToolService::processToolRequest)
					.onToolExecuted(aiToolService::processToolExecution)
					.onPartialThinkingWithContext((thinking, context) -> {
						if (stopRequested) {
							stopRequested = false;
							aiMessageStarted.set(true);
							context.streamingHandle().cancel();
							removeChatBubble(currentStreamingPanel);
						}
					})
					.onPartialResponseWithContext((partialResponse, context) -> {
						aiMessageStarted.set(true);
						if (stopRequested) {
							stopRequested = false;
							context.streamingHandle().cancel();
							if (currentStreamingPanel.getText().isEmpty()) {
								removeChatBubble(currentStreamingPanel);
							}
						} else {
							SwingUtilities.invokeLater(() -> currentStreamingPanel.appendText(partialResponse.text()));
							// Scroll to bottom periodically (every 200ms) to avoid excessive updates
							final long now = System.currentTimeMillis();
							if (now - lastScrollTime[0] > updateDelay) {
								lastScrollTime[0] = now;
								SwingUtilities.invokeLater(() -> {
									final JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
									vertical.setValue(vertical.getMaximum());
								});
							}
						}
					})
					.onCompleteResponse(response -> {
						aiMessageStarted.set(true);
						try {
							if (currentConversation != null) {
								currentConversation.addMessage(
										currentStreamingPanel.getText(),
										response.aiMessage()
									);
							}

							SwingUtilities.invokeLater(() -> {
								final JScrollBar vertical = chatScrollPane
									.getVerticalScrollBar();
								vertical.setValue(vertical.getMaximum());
								setSendMode();
							});
						}
						catch (Exception e) {
							handleAssistantFailure(e,
								"Unable to save the assistant response", true);
							SwingUtilities.invokeLater(this::setSendMode);
						}
					})
					.onError(error -> {
						handleAssistantFailure(error, currentStreamingPanel,
							aiMessageStarted);
					})
					.start();
			} catch (Exception e) {
				handleAssistantFailure(e, currentStreamingPanel, aiMessageStarted);
			}
		});

		// Start an immediate thinking thread.
		threadService.run(() -> {
			boolean stopped = false;
			while (!aiMessageStarted.get()) {
				if (stopRequested && !stopped) {
					stopped = true;
					cancelConversation.set(true);
					msgThread.cancel(false);
					stopRequested = false;
					SwingUtilities.invokeLater(() -> {
						aiMessageStarted.set(true);
						removeChatBubble(currentStreamingPanel);
					});
				} else {
					SwingUtilities.invokeLater(() -> {
						currentStreamingPanel.updateThinking();
					});
					try {
						Thread.sleep(updateDelay);
					} catch (InterruptedException e) {
						// no-op
					}
				}
			}
		});
	}

	private void removeChatBubble(ChatMessagePanel chatMessagePanel) {
		SwingUtilities.invokeLater(() -> {
				chatPanel.remove(chatMessagePanel);
				chatPanel.repaint();
				JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
				vertical.setValue(vertical.getMaximum());
		});
	}

	private List<ContextItem> mergeContextItems(List<ContextItem> contextItems) {
		final List<ContextItem> result = new ArrayList<>();
		final Map<String, List<ContextItem>> bins = new HashMap<>();

		// Bin items by their merge key
		for (final ContextItem item : contextItems) {
			final String mergeKey = item.getMergeKey();
			if (mergeKey != null) {
				bins.computeIfAbsent(mergeKey, k -> new ArrayList<>()).add(item);
			}
			else {
				// Items without a merge key are added as-is
				result.add(item);
			}
		}

		// Merge items in each bin and add to result
		for (final List<ContextItem> bin : bins.values()) {
			if (bin.size() > 1) {
				final ContextItem merged = bin.get(0).mergeWith(bin.subList(1, bin
					.size()));
				result.add(merged);
			}
			else {
				result.add(bin.get(0));
			}
		}
		return result;
	}

	/**
	 * Requests the current generation to stop.
	 */
	private void requestStop() {
		stopRequested = true;
		setSendMode();
	}

	/**
	 * Switches the send/stop button to send mode.
	 */
	private void setSendMode() {
		isSendMode = true;
		if (sendIcon != null) {
			sendStopButton.setIcon(sendIcon);
		}
		else {
			sendStopButton.setText("Send");
		}
		sendStopButton.setToolTipText("Send message");

		sendStopButton.setEnabled(modelReady);
		inputArea.setEnabled(modelReady);
		if (modelReady) {
			inputArea.requestFocus();
		}
	}

	private void handleAssistantFailure(final Throwable error,
		final ChatMessagePanel streamingPanel,
		final AtomicBoolean aiMessageStarted)
	{
		aiMessageStarted.set(true);
		if (streamingPanel.getText().isEmpty()) {
			removeChatBubble(streamingPanel);
		}
		handleAssistantFailure(error, "Assistant request failed", false);
		SwingUtilities.invokeLater(this::setSendMode);
	}

	private void handleAssistantFailure(final Throwable error,
		final String context, final boolean recovered)
	{
		logService.debug(context, error);
		if (!recovered) {
			appendToChat(Sender.SYSTEM, buildAssistantFailureMessage(error, context));
		}
	}

	private String buildAssistantFailureMessage(final Throwable error,
		final String context)
	{
		if (hasCause(error, RateLimitException.class)) {
			return "Rate limit reached. Please wait before retrying, or select a different model.";
		}

		if (isInvalidApiKey(error)) {
			final String providerName = llmProvider.getName();
			final StringBuilder message = new StringBuilder("The API key for **")
				.append(providerName)
				.append("** was rejected. Please edit the API key in Fiji and try again.");
			final String apiKeyUrl = llmProvider.getApiKeyUrl();
			if (apiKeyUrl != null && !apiKeyUrl.isBlank()) {
				message.append("\n\n[Get a new API key](").append(apiKeyUrl).append(")");
			}
			return message.toString();
		}

		final Throwable cause = rootCause(error);
		final String message = cause != null && cause.getMessage() != null ? cause
			.getMessage().replaceAll("\\s+", " ") : "(no message)";
		final String shortened = message.length() > 300 ? message.substring(0,
			300) + "..." : message;
		return context + ": " + shortened;
	}

	private boolean isInvalidApiKey(final Throwable error) {
		Throwable cause = error;
		while (cause != null) {
			final String message = cause.getMessage();
			if (message != null) {
				final String normalized = message.toLowerCase(Locale.ROOT);
				if (normalized.contains("api_key_invalid") || normalized.contains(
					"api key not valid") || normalized.contains("invalid api key"))
				{
					return true;
				}
			}
			cause = cause.getCause();
		}
		return false;
	}

	private static <T extends Throwable> boolean hasCause(final Throwable error,
		final Class<T> type)
	{
		Throwable cause = error;
		while (cause != null) {
			if (type.isInstance(cause)) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}

	private static Throwable rootCause(final Throwable error) {
		Throwable cause = error;
		while (cause != null && cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause;
	}

	/**
	 * Switches the send/stop button to stop mode.
	 */
	private void setStopMode() {
		isSendMode = false;
		if (stopIcon != null) {
			sendStopButton.setIcon(stopIcon);
		}
		else {
			sendStopButton.setText("Stop");
		}
		sendStopButton.setToolTipText("Interrupt the assistant");
		sendStopButton.setEnabled(modelReady);

		inputArea.setText("");
		inputArea.setEnabled(false);
	}

	private void appendToChat(final Sender sender, final String message) {
		if (message == null) {
			return;
		}

		// Always use invokeLater since this can be called from both EDT and background threads
		SwingUtilities.invokeLater(() -> {
			// Convert Sender enum to MessageType
			final ChatMessagePanel.MessageType messageType = switch (sender) {
				case USER ->
					ChatMessagePanel.MessageType.USER;
				case ASSISTANT ->
					ChatMessagePanel.MessageType.ASSISTANT;
				case SYSTEM ->
					ChatMessagePanel.MessageType.SYSTEM;
				case ERROR ->
					ChatMessagePanel.MessageType.ERROR;
			};

			addMessagePanelToChat(messageType, message);

			// System and error messages are not added to chat memory
			// User and assistant messages are already tracked in chatMemory via sendMessage()
		});
	}

	/**
	 * Adds a message panel to the chat (must be called on EDT).
	 */
	private void addMessagePanelToChat(
		final ChatMessagePanel.MessageType messageType, final String message)
	{
		addMessagePanelToChat(new ChatMessagePanel(messageType, message,
			CHAT_FONT_SIZE));
	}

	private void addMessagePanelToChat(final ChatMessagePanel messagePanel) {

		// Remove the glue and bottom spacer, add message, re-add glue and spacer to
		// keep messages at bottom
		final int componentCount = chatPanel.getComponentCount();
		if (componentCount >= 2) {
			final java.awt.Component glue = chatPanel.getComponent(0);
			final java.awt.Component bottomSpacer = chatPanel.getComponent(
				componentCount - 1);
			chatPanel.remove(0); // Remove glue
			chatPanel.remove(componentCount - 2); // Remove bottom spacer (index
			// shifts after first removal)
			chatPanel.add(messagePanel, "growx"); // Grow horizontally only
			chatPanel.add(glue, "pushy, growy", 0); // Re-add glue at top (index 0)
			chatPanel.add(bottomSpacer, "growx, height 8!"); // Re-add bottom spacer
			// at end
		}
		else {
			chatPanel.add(messagePanel, "growx");
		}

		chatPanel.revalidate();
		chatPanel.repaint();

		// Scroll to bottom
		SwingUtilities.invokeLater(() -> {
			final javax.swing.JScrollBar vertical = chatScrollPane
				.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());
		});
	}

	/**
	 * Creates an empty assistant message panel for streaming. Must be called on
	 * the EDT.
	 */
	private ChatMessagePanel createEmptyAssistantMessagePanel() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("Must be called on EDT");
		}

		// Create an empty assistant message panel
		final ChatMessagePanel messagePanel = new ChatMessagePanel(
			ChatMessagePanel.MessageType.ASSISTANT, "", CHAT_FONT_SIZE);

		// Remove the glue and bottom spacer, add message, re-add glue and spacer to
		// keep messages at bottom
		final int componentCount = chatPanel.getComponentCount();
		if (componentCount >= 2) {
			final java.awt.Component glue = chatPanel.getComponent(0);
			final java.awt.Component bottomSpacer = chatPanel.getComponent(
				componentCount - 1);
			chatPanel.remove(0); // Remove glue
			chatPanel.remove(componentCount - 2); // Remove bottom spacer (index
			// shifts after first removal)
			chatPanel.add(messagePanel, "growx"); // Grow horizontally only
			chatPanel.add(glue, "pushy, growy", 0); // Re-add glue at top (index 0)
			chatPanel.add(bottomSpacer, "growx, height 8!"); // Re-add bottom spacer
			// at end
		}
		else {
			chatPanel.add(messagePanel, "growx");
		}

		chatPanel.revalidate();
		chatPanel.repaint();

		return messagePanel;
	}

	private void launchGuide() {
		guide.start();
	}

	private void startModelPreparation() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("Must be called on EDT");
		}

		setPreparationControlsEnabled(false);
		preparationPanel = new ChatMessagePanel(ChatMessagePanel.MessageType.SYSTEM,
			"*Getting your assistant ready*", CHAT_FONT_SIZE);
		addMessagePanelToChat(preparationPanel);

		preparationTimer = new javax.swing.Timer(200, e -> {
			if (preparationPanel != null) {
				preparationPanel.updateThinking("Getting your assistant ready");
			}
		});
		preparationTimer.start();

		llmProvider.prepare(modelName).whenComplete((ignored, error) ->
			SwingUtilities.invokeLater(() -> finishModelPreparation(error)));
	}

	private void finishModelPreparation(final Throwable error) {
		if (preparationTimer != null) {
			preparationTimer.stop();
			preparationTimer = null;
		}

		final ChatMessagePanel completedPreparationPanel = preparationPanel;
		preparationPanel = null;
		if (completedPreparationPanel != null) {
			removeChatBubble(completedPreparationPanel);
		}

		if (error != null) {
			handleAssistantFailure(error, "Unable to prepare the assistant", false);
			return;
		}

		setPreparationControlsEnabled(true);
		setSendMode();
	}

	private void setPreparationControlsEnabled(final boolean enabled) {
		modelReady = enabled;
		conversationComboBox.setEnabled(enabled);
		inputArea.setEnabled(enabled);
		sendStopButton.setEnabled(enabled);
		if (!enabled) {
			newConversationButton.setEnabled(false);
			deleteConversationButton.setEnabled(false);
		}
	}

	private void configureKeys() {
		// Close this chat window
		frame.dispose();
		prefService.remove(Manage_Keys.class, Manage_Keys.autoRunKey(llmProvider
			.getName()));
		prefService.remove(Fiji_Chat.class, Fiji_Chat.AUTO_RUN);

		Map<String, Object> params = new HashMap<>();
		params.put("startChatbot", true);
		params.put("provider", llmProvider.getName());
		// Re-invoke the Fiji_Chat command to show the selection dialog
		commandService.run(Manage_Keys.class, true, params);
	}

	private void configureChat() {
		// Close this chat window
		frame.dispose();
		prefService.remove(Fiji_Chat.class, Fiji_Chat.AUTO_RUN);

		// Re-invoke the Fiji_Chat command to show the selection dialog
		commandService.run(Fiji_Chat.class, true);
	}

	private static String buildConfigureChatButtonText(final String providerName,
		final String modelName, final boolean supportsModelSelection,
		final Font buttonFont)
	{
		if (!supportsModelSelection) {
			return "<html>" + noWrap(truncateText(providerName, buttonFont,
				CONFIGURE_CHAT_TEXT_WIDTH)) + "</html>";
		}

		final Font modelFont = buttonFont.deriveFont(Font.BOLD);
		final Font providerFont = buttonFont.deriveFont(Math.max(1f, buttonFont
			.getSize2D() - 2f));
		final String modelText = noWrap(truncateText(modelName, modelFont,
			CONFIGURE_CHAT_TEXT_WIDTH));
		final String providerText = noWrap(truncateText(providerName,
			providerFont, CONFIGURE_CHAT_TEXT_WIDTH));
		return "<html><b>" + modelText + "</b><br><font size=\"-1\">" +
			providerText + "</font></html>";
	}

	private static String buildConfigureChatTooltip(final String providerName,
		final String modelName, final String description)
	{
		final String descriptionText = escapeHtml(description).replace("\r\n",
			"<br>").replace("\n", "<br>");
		return "<html>" + escapeHtml(providerName) + "<br>" + descriptionText +
			"<br><br>Model: " + escapeHtml(modelName) +
			"<br>Click to change provider or model</html>";
	}

	private static String truncateText(final String text, final Font font,
		final int maxWidth)
	{
		final String value = text == null ? "" : text;
		final FontMetrics metrics = new JButton().getFontMetrics(font);
		final String ellipsis = "...";
		if (metrics.stringWidth(value) <= maxWidth) return value;

		int end = value.length();
		while (end > 0 && metrics.stringWidth(value.substring(0, end) + ellipsis) >
			maxWidth)
		{
			end--;
		}
		return value.substring(0, end) + ellipsis;
	}

	private static String noWrap(final String text) {
		return escapeHtml(text).replace(" ", "&nbsp;");
	}

	private static String escapeHtml(final String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">",
			"&gt;").replace("\"", "&quot;");
	}

	private void openForumInBrowser() {
		try {
			final URI uri = new URI("https://forum.image.sc/tag/llm");
			platformService.open(uri.toURL());
		}
		catch (Exception e) {
			appendToChat(Sender.ERROR, "Failed to open forum: " + e.getMessage());
		}
	}

	private void addContextItem(final ContextItem item,
		final ContextItemSupplier supplier)
	{
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> addContextItem(item, supplier));
			return;
		}
		// Check for duplicates using equals() - don't add the same item twice
		if (contextItems.contains(item)) {
			// Flash the existing tag to indicate it's already added
			final JButton existingButton = contextItemButtons.get(item);
			if (existingButton != null) {
				flashButton(existingButton);
			}
			return;
		}

		// Add to context items list
		contextItems.add(item);

		// Truncate label to max length
		final int maxLabelLength = 20;
		String displayLabel = item.getLabel();
		if (displayLabel.length() > maxLabelLength) {
			displayLabel = displayLabel.substring(0, maxLabelLength - 1) + "…";
		}

		final JButton tagButton = createContextTagButton(displayLabel, supplier,
			true);

		// Build tooltip
		String tooltipText = item.getLabel() + " - Click to remove";
		tagButton.setToolTipText(tooltipText);
		tagButton.addActionListener(e -> removeContextItem(item, tagButton));

		// Store the button reference (item -> button)
		contextItemButtons.put(item, tagButton);

		// Get the tags container from the scrollpane's viewport
		JPanel tagsContainer = (JPanel) contextTagsScrollPane.getViewport()
			.getView();
		tagsContainer.remove(contextPlaceholderLabel);
		tagsContainer.add(tagButton);

		// Enable the Clear All button now that we have context items
		clearAllButton.setEnabled(true);
		contextTagsPanel.revalidate();
		contextTagsPanel.repaint();
	}

	private JButton createContextTagButton(final String displayLabel,
		final ContextItemSupplier supplier, final boolean removable)
	{
		final String buttonText = removable ? displayLabel + " ✕" : displayLabel;
		final ImageIcon supplierIcon = supplier.getIcon();
		final JButton tagButton;
		if (supplierIcon != null) {
			final Image scaledImage = supplierIcon.getImage().getScaledInstance(16,
				16, Image.SCALE_SMOOTH);
			final ImageIcon scaledIcon = new ImageIcon(scaledImage);
			tagButton = new JButton(buttonText, scaledIcon);
			tagButton.setHorizontalTextPosition(JButton.RIGHT);
			tagButton.setVerticalTextPosition(JButton.CENTER);
		}
		else {
			tagButton = new JButton(buttonText);
		}

		tagButton.setFocusPainted(false);
		tagButton.setContentAreaFilled(false);
		tagButton.setOpaque(true);
		tagButton.setBackground(java.awt.Color.WHITE);
		tagButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory
			.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 6, 3, 6)));

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

		return tagButton;
	}

	private void removeContextItem(final ContextItem item,
		final JButton tagButton)
	{
		contextItems.remove(item);
		contextItemButtons.remove(item);
		final JPanel tagsContainer = (JPanel) contextTagsScrollPane.getViewport()
			.getView();
		tagsContainer.remove(tagButton);
		if (contextItems.isEmpty()) {
			tagsContainer.add(contextPlaceholderLabel);
		}

		// Disable the Clear All button if no more context items
		if (contextItems.isEmpty()) {
			clearAllButton.setEnabled(false);
		}

		contextTagsPanel.revalidate();
		contextTagsPanel.repaint();
	}

	private void clearAllContextButtons() {
		// Remove all context items from the list
		for (final ContextItem item : new ArrayList<>(contextItems)) {
			contextItems.remove(item);
		}

		contextItemButtons.clear();
		final JPanel tagsContainer = (JPanel) contextTagsScrollPane.getViewport()
			.getView();
		tagsContainer.removeAll();
		tagsContainer.add(contextPlaceholderLabel);
		clearAllButton.setEnabled(false);
		contextTagsPanel.revalidate();
		contextTagsPanel.repaint();
	}

	private void clearAllContext() {
		clearAllContextButtons();
	}

	/**
	 * Builds the initial system message
	 */
	private String buildSystemMessage() {
		final StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

		sb.append("\n\n## Context\n");
		sb.append(
			"Contextual information (scripts, images, runtime environment information, etc) is attached as JSON in user messages.\n" +
			"This includes application state, and items focused by the user.\n");

		sb.append("\n\n## Tool Usage\n");
		sb.append(aiToolService.toolEnvironmentMessage());

		final List<AiToolPlugin> tools = aiToolService.getInstances();
		if (!tools.isEmpty()) {
			sb.append("\n\n## Available Tools\n\n");
			for (final AiToolPlugin tool : tools) {
				sb.append("- **").append(tool.getName()).append("**: ");
				final String description = tool.getUsage();
				if (description != null && !description.isEmpty()) {
					sb.append(description);
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	private void flashButton(final JButton button) {
		// Flash the button orange to indicate duplicate
		final java.awt.Color originalBg = button.getBackground();
		final java.awt.Color flashColor = new java.awt.Color(255, 165, 0); // Orange

		// Create a timer to flash 3 times
		final javax.swing.Timer timer = new javax.swing.Timer(150, null);
		final int[] flashCount = { 0 };

		timer.addActionListener(e -> {
			if (flashCount[0] % 2 == 0) {
				button.setBackground(flashColor);
			}
			else {
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

	/**
	 * Called when a conversation is selected from the combo box.
	 */
	private void onConversationSelected() {
		if (!modelReady) {
			return;
		}

		Object selected = conversationComboBox.getSelectedItem();
		if (selected != null && !selected.toString().isEmpty()) {
			String selectedName = selected.toString();
			if (currentConversation == null || !(currentConversation.name().equals(
				selectedName)))
			{
				// Already active
				loadConversation(selected.toString());
			}
			final boolean conversationLoaded = currentConversation != null &&
				currentConversation.name().equals(selectedName);
			deleteConversationButton.setEnabled(conversationLoaded);
			newConversationButton.setEnabled(conversationLoaded);
		}
		else {
			deleteConversationButton.setEnabled(false);
			newConversationButton.setEnabled(false);
		}
	}

	/**
	 * Clears chat history. Conversations auto-start with the first message. Does
	 * nothing if the current conversation is empty (no messages sent yet).
	 */
	private void clearConversation() {
		if (!modelReady) {
			return;
		}

		// Don't allow starting a new conversation if the current one is empty
		// (hasn't been named yet)
		if (currentConversation == null) {
			return;
		}

		currentConversation = null;
		clearChatPanel();
		conversationComboBox.setSelectedIndex(-1);
		deleteConversationButton.setEnabled(false);
		newConversationButton.setEnabled(false);
		inputArea.requestFocus();
	}

	/**
	 * Load a previously saved conversation.
	 */
	private void loadConversation(String conversationName) {
		if (!modelReady) {
			return;
		}

		Conversation conversation = conversationService.getConversation(
			conversationName);
		if (conversation == null) {
			return;
		}

		final Conversation previousConversation = currentConversation;
		final FijiAssistant previousAssistant = assistant;
		try {
			// Reload chat memory with conversation messages before changing the UI.
			final ChatMemory chatMemory = buildAssistant(conversation.systemMessage());
			for (Conversation.Message msg : conversation.messages()) {
				chatMemory.add(msg.memory());
			}

			currentConversation = conversation;
			clearChatPanel();
			for (Conversation.Message msg : conversation.messages()) {
				addMessagePanelToChat(msg
					.memory() instanceof dev.langchain4j.data.message.UserMessage
						? ChatMessagePanel.MessageType.USER
						: ChatMessagePanel.MessageType.ASSISTANT, msg.display());
			}
		}
		catch (Exception e) {
			currentConversation = previousConversation;
			assistant = previousAssistant;
			handleAssistantFailure(e, "Unable to load conversation", false);
			return;
		}

		inputArea.requestFocus();
	}

	private ChatMemory buildAssistant(SystemMessage systemMessage) {
		ChatMemory chatMemory;
		try {
			chatMemory = llmProvider.createTokenChatMemory(modelName);
			chatMemory.add(systemMessage);
		}
		catch (Exception e) {
			handleAssistantFailure(e,
				"Token counting unavailable; using message-count chat memory", true);
			// Fall back to a 20-message window
			chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
			chatMemory.add(systemMessage);
		}

		// Recreate the assistant with the chat memory for proper tool tracking
		assistant = assistantService.createAssistant(FijiAssistant.class,
			llmProvider.getName(), modelName, chatMemory, requestParameters);
		return chatMemory;
	}

	/**
	 * Clear the chat panel (removes all message panels).
	 */
	private void clearChatPanel() {
		// Get the chat panel and remove all message components (keep glue panel)
		synchronized (chatPanel.getTreeLock()) {
			java.awt.Component[] components = chatPanel.getComponents();
			for (java.awt.Component component : components) {
				if (component instanceof ChatMessagePanel) {
					chatPanel.remove(component);
				}
			}
		}
		chatPanel.revalidate();
		chatPanel.repaint();
	}

	/**
	 * Create a new the conversation based on the user's first message. Sends a
	 * separate request to the LLM to summarize the message.
	 */
	private void createNewConversation(String userMessage,
		final AtomicBoolean cancelConversation)
	{
		SystemMessage systemMessage = new SystemMessage(buildSystemMessage());
		ChatMemory chatMemory = buildAssistant(systemMessage);
		String textToTruncate = userMessage;
		String conversationName;
		try {
			// Create a simple request to summarize the user's message into a brief
			// name
			final String namingPrompt =
				"Following is the first message of a new conversation. Respond " +
				"with ONLY a 3-5 word summary of this message, suitable for a " +
				"conversation title: \"" +
					userMessage + "\"";
			final ChatRequest nameRequest = ChatRequest.builder().messages(
				new UserMessage(namingPrompt)).build();

			final AiMessage response = assistant.chat(nameRequest);
			chatMemory.clear();
			textToTruncate = response.text();
		}
		catch (Exception e) {
			handleAssistantFailure(e,
				"Unable to generate a conversation name; using the first message instead",
				true);
		}

		// Check if the message was interrupted while we were waiting for the LLM.
		if (cancelConversation.get()) {
			return;
		}

		// Truncate to the the first few words
		textToTruncate = textToTruncate.trim();
		String[] words = textToTruncate.split("\\s+");
		if (words.length <= 5) {
			conversationName = textToTruncate;
		}
		else {
			conversationName = String.join(" ", Arrays.copyOf(words, 5)) + "...";
		}
		// Truncate conversation name to 30 characters max
		final int maxNameLength = 30;
		if (conversationName.length() > maxNameLength) {
			conversationName = conversationName.substring(0, maxNameLength - 1) +
				"...";
		}

		final String timestampedName = conversationName + new SimpleDateFormat(
			" [dd.MMM.yyyy]").format(new Date());
		// Create the conversation with the auto-generated name
		currentConversation = conversationService.createConversation(
			timestampedName, systemMessage);

		SwingUtilities.invokeLater(() -> {
			conversationComboBox.insertItemAt(timestampedName, 0);
			conversationComboBox.setSelectedItem(timestampedName);
			deleteConversationButton.setEnabled(true);
			newConversationButton.setEnabled(true);
		});
	}

	/**
	 * Delete the currently selected conversation permanently.
	 */
	private void deleteCurrentConversation() {
		if (currentConversation == null) {
			return;
		}

		final String conversationName = currentConversation.name();

		// Confirm deletion with user
		final int response = javax.swing.JOptionPane.showConfirmDialog(frame,
			"Are you sure you want to permanently delete this conversation?\n\n" +
				conversationName, "Delete Conversation",
			javax.swing.JOptionPane.YES_NO_OPTION,
			javax.swing.JOptionPane.WARNING_MESSAGE);

		if (response == javax.swing.JOptionPane.YES_OPTION) {
			conversationService.deleteConversation(conversationName);
			currentConversation = null;
			clearChatPanel();
			conversationComboBox.setSelectedIndex(-1);
			deleteConversationButton.setEnabled(false);
			newConversationButton.setEnabled(false);
			conversationComboBox.removeItem(conversationName);
			inputArea.requestFocus();
		}
	}
}
