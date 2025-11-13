package sc.fiji.llm.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.border.AbstractBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;

/**
 * A custom panel for displaying a single chat message with icon and styled bubble.
 */
public class ChatMessagePanel extends JPanel {

	private static final int ICON_SIZE = 32;
	private static final int ICON_GAP = 4;
	private static final int MARGIN = 10;
	private static final int BUBBLE_PADDING = 8;
	private static final int BUBBLE_HORIZONTAL_PADDING = 12;
	private static final int BUBBLE_BORDER_RADIUS = 12;
	private static final int BORDER_WIDTH = 2;
	private static final int RESERVED_WIDTH_BUFFER = (2 * ICON_SIZE) + BUBBLE_PADDING + BUBBLE_HORIZONTAL_PADDING;
	private static final int MIN_AVAILABLE_WIDTH = 200;
	private static final int DEFAULT_AVAILABLE_WIDTH = 600;
	private static final int THINKING_STAGES = 4;
	private final float textFontSize;
	private JTextPane textPane;
	private int thinkingStage = -1;

	public enum MessageType {
		USER,
		ASSISTANT,
		SYSTEM,
		ERROR
	}

	public ChatMessagePanel(final MessageType type, final String message) {
		this(type, message, 13f);
	}

	public ChatMessagePanel(final MessageType type, final String message, final float fontSize) {
		this.textFontSize = fontSize;
		setLayout(new MigLayout("insets 0 0 0 0, fillx", "", "[]"));
		setOpaque(false);

		final JPanel bubble = createMessageBubble(type, message);
		final JLabel iconLabel = createIcon(type);

		layoutComponents(type, bubble, iconLabel);
	}

	private void layoutComponents(final MessageType type, final JPanel bubble, final JLabel iconLabel) {
		switch (type) {
			case USER -> layoutUserMessage(bubble, iconLabel);
			case ASSISTANT -> layoutAssistantMessage(bubble, iconLabel);
			case SYSTEM, ERROR -> add(bubble, "align center");
		}
	}

	private void layoutUserMessage(final JPanel bubble, final JLabel iconLabel) {
		final JPanel pusher = createInvisibleSpacer();
		add(pusher, "pushx");
		add(bubble, "aligny bottom");
		add(iconLabel, "aligny bottom, gapleft " + MARGIN + ", gapright " + MARGIN + ", gapbottom " + ICON_GAP);
	}

	private void layoutAssistantMessage(final JPanel bubble, final JLabel iconLabel) {
		final JPanel pusher = createInvisibleSpacer();
		add(iconLabel, "aligny bottom, gapleft " + MARGIN + ", gapright " + MARGIN + ", gapbottom " + ICON_GAP);
		add(bubble, "aligny bottom");
		add(pusher, "pushx");
	}

	private JPanel createInvisibleSpacer() {
		final JPanel pusher = new JPanel();
		pusher.setOpaque(false);
		return pusher;
	}

	private JLabel createIcon(final MessageType type) {
		final String iconPath = getIconPath(type);

		if (iconPath != null) {
			try {
				final URL iconURL = getClass().getResource(iconPath);
				if (iconURL != null) {
					final ImageIcon icon = new ImageIcon(iconURL);
					return createIconLabel(icon);
				}
			} catch (Exception e) {
				// Fall through to return empty label
			}
		}

		return createEmptyIconLabel();
	}

	private String getIconPath(final MessageType type) {
		return switch (type) {
			case USER -> "/icons/user-32.png";
			case ASSISTANT -> "/icons/fiji-32.png";
			case SYSTEM -> "/icons/info.png";
			case ERROR -> "/icons/info.png";
		};
	}

	private JLabel createIconLabel(final ImageIcon icon) {
		final ImageIcon scaledIcon = (icon.getIconWidth() != ICON_SIZE || icon.getIconHeight() != ICON_SIZE)
			? scaleIcon(icon)
			: icon;

		return createLabelWithFixedSize(scaledIcon);
	}

	private ImageIcon scaleIcon(final ImageIcon icon) {
		final java.awt.Image scaledImage = icon.getImage().getScaledInstance(
			ICON_SIZE, ICON_SIZE, java.awt.Image.SCALE_SMOOTH);
		return new ImageIcon(scaledImage);
	}

	private JLabel createLabelWithFixedSize(final ImageIcon icon) {
		final JLabel label = new JLabel(icon);
		setFixedSize(label, ICON_SIZE, ICON_SIZE);
		return label;
	}

	private JLabel createEmptyIconLabel() {
		final JLabel label = new JLabel();
		setFixedSize(label, ICON_SIZE, ICON_SIZE);
		return label;
	}

	private void setFixedSize(final JLabel label, final int width, final int height) {
		final Dimension size = new Dimension(width, height);
		label.setPreferredSize(size);
		label.setMinimumSize(size);
		label.setMaximumSize(size);
	}

	   private JPanel createMessageBubble(final MessageType type, final String message) {
		   final JPanel bubble = new JPanel(new MigLayout(
			   "insets " + BUBBLE_PADDING + " " + BUBBLE_HORIZONTAL_PADDING + " " +
			   BUBBLE_PADDING + " " + BUBBLE_HORIZONTAL_PADDING, "", "")) {

			   private final int RESERVED_WIDTH = (2 * MARGIN) + ICON_SIZE + (2 * MARGIN) + RESERVED_WIDTH_BUFFER;

			   @Override
			   public Dimension getPreferredSize() {
				   final int availableWidth = calculateAvailableWidth();
				   final Dimension pref = super.getPreferredSize();
				   final int largestChildWidth = findLargestChildWidth();

				   pref.width = Math.min(largestChildWidth, availableWidth);
				   return pref;
			   }

			   private int calculateAvailableWidth() {
				   int availableWidth = DEFAULT_AVAILABLE_WIDTH;
				   Container parent = getParent();
				   while (parent != null) {
					   final int parentWidth = parent.getWidth();
					   if (parentWidth > 0) {
						   availableWidth = parentWidth;
						   break;
					   }
					   parent = parent.getParent();
				   }
				   return Math.max(MIN_AVAILABLE_WIDTH, availableWidth - RESERVED_WIDTH);
			   }

			   private int findLargestChildWidth() {
				   int largestChildWidth = 0;
				   for (final Component child : getComponents()) {
					   final Dimension childPref = child.getPreferredSize();
					   if (childPref.width > largestChildWidth) {
						   largestChildWidth = childPref.width;
					   }
				   }
				   return largestChildWidth + (2 * BUBBLE_HORIZONTAL_PADDING) + (2 * BORDER_WIDTH);
			   }
		   };

		   applyBubbleStyle(bubble, type);

		   createTextPane(type, message);

		   bubble.add(textPane);

		   return bubble;
	   }

	private void applyBubbleStyle(final JPanel bubble, final MessageType type) {
		final Color bgColor = getBackgroundColor(type);
		final Color borderColor = getBorderColor(type);

		bubble.setBackground(bgColor);
		bubble.setBorder(new RoundedBorder(borderColor, BUBBLE_BORDER_RADIUS));
	}

	private Color getBackgroundColor(final MessageType type) {
		return switch (type) {
			case USER -> new Color(229, 229, 234);
			case ASSISTANT -> new Color(227, 242, 253);
			case SYSTEM -> new Color(255, 249, 196);
			case ERROR -> new Color(255, 205, 210);
		};
	}

	private Color getBorderColor(final MessageType type) {
		return switch (type) {
			case USER -> new Color(200, 200, 210);
			case ASSISTANT -> new Color(144, 202, 249);
			case SYSTEM -> new Color(255, 235, 59);
			case ERROR -> new Color(239, 83, 80);
		};
	}

	private JTextPane createTextPane(final MessageType type, final String message) {
		textPane = new JTextPane();
		textPane.setText(message);
		textPane.setEditable(false);
		textPane.setFocusable(true); // Allow highlighting and copying
		textPane.setOpaque(false);
		textPane.setFont(textPane.getFont().deriveFont(textFontSize));

		final StyledDocument doc = textPane.getStyledDocument();
		final SimpleAttributeSet attrs = new SimpleAttributeSet();

		final int alignment = switch (type) {
			case USER, ASSISTANT -> StyleConstants.ALIGN_LEFT;
			case SYSTEM, ERROR -> StyleConstants.ALIGN_CENTER;
		};

		StyleConstants.setAlignment(attrs, alignment);
		// Remove extra paragraph indents
		StyleConstants.setLeftIndent(attrs, 0f);
		StyleConstants.setRightIndent(attrs, 0f);
		doc.setParagraphAttributes(0, doc.getLength(), attrs, false);

		// Remove extra JTextPane margin
		textPane.setMargin(new java.awt.Insets(0, 0, 0, 0));

		// Add right-click context menu for copying text
		addContextMenu(textPane);

		return textPane;
	}

	private void addContextMenu(final JTextPane textPane) {
		final JPopupMenu contextMenu = new JPopupMenu();
		final JMenuItem copyItem = new JMenuItem("Copy");

		copyItem.addActionListener(e -> {
			final String selectedText = textPane.getSelectedText();
			if (selectedText != null && !selectedText.isEmpty()) {
				final StringSelection selection = new StringSelection(selectedText);
				java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
			}
		});

		contextMenu.add(copyItem);
		textPane.setComponentPopupMenu(contextMenu);
	}

	public void updateThinking() {
		thinkingStage++;
		if (thinkingStage == THINKING_STAGES) {
			thinkingStage = 0;
		}
		StringBuilder sb = new StringBuilder("Thinking");
		for (int i=0; i<thinkingStage; i++) {
			sb.append(".");
		}
		textPane.setText(sb.toString());
	}

	/**
	 * Appends text to this message panel (for streaming updates).
	 * This method is thread-safe and can be called from any thread.
	 *
	 * @param text the text to append
	 */
	public void appendText(final String text) {
		if (text == null || text.isEmpty()) {
			return;
		}

		if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
			javax.swing.SwingUtilities.invokeLater(() -> appendText(text));
			return;
		}

		if (textPane != null) {
			if (thinkingStage >= 0) {
				thinkingStage = -1;
				textPane.setText("");
			}
			try {
				final StyledDocument doc = textPane.getStyledDocument();
				doc.insertString(doc.getLength(), text, null);
			} catch (Exception e) {
				// Ignore insertion errors
			}
		}
	}

	/**
	 * Custom border for rounded corners on message bubbles.
	 */
	private static class RoundedBorder extends AbstractBorder {
		private final Color color;
		private final int radius;

		public RoundedBorder(final Color color, final int radius) {
			this.color = color;
			this.radius = radius;
		}

		@Override
		public void paintBorder(final Component c, final Graphics g, final int x, final int y,
			final int width, final int height)
		{
			final Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setColor(color);
			g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
			g2d.dispose();
		}

		@Override
		public Insets getBorderInsets(final Component c) {
			return new Insets(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH);
		}

		@Override
		public Insets getBorderInsets(final Component c, final Insets insets) {
			insets.left = insets.right = insets.top = insets.bottom = BORDER_WIDTH;
			return insets;
		}
	}
}
