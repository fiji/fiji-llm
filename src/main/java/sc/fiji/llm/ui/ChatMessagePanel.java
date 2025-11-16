/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package sc.fiji.llm.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.border.AbstractBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

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
	private final StringBuilder rawMarkdown;

	// Flexmark parser/renderer configured for common GFM extensions used by LLMs
	private static final Parser MARKDOWN_PARSER;
	private static final HtmlRenderer MARKDOWN_RENDERER;

	static {
		final MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(
			EmojiExtension.create(),
			TablesExtension.create(),
			StrikethroughExtension.create(),
			TaskListExtension.create(),
			AutolinkExtension.create()
		));
		MARKDOWN_PARSER = Parser.builder(options).build();
		MARKDOWN_RENDERER = HtmlRenderer.builder(options).build();
	}

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
		this.rawMarkdown = new StringBuilder(message == null ? "" : message);
		setLayout(new MigLayout("insets 0 0 0 0, fillx", "", "[]"));
		setOpaque(false);

		final JPanel bubble = createMessageBubble(type);
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

	private JPanel createMessageBubble(final MessageType type) {
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

		   createTextPane(type);

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

	private JTextPane createTextPane(final MessageType type) {
		// create the pane and configure basic properties
		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setFocusable(true); // Allow highlighting and copying
		textPane.setOpaque(false);
		textPane.setFont(textPane.getFont().deriveFont(textFontSize));

		// Use an HTMLEditorKit with a programmatic StyleSheet for predictable styling
		final HTMLEditorKit kit = new HTMLEditorKit();
		final StyleSheet ss = kit.getStyleSheet();
		ss.addRule("body { font-family: Dialog, Arial, sans-serif; font-size: " + (int) textFontSize + "px; color: #222; }");
		ss.addRule("pre { font-family: monospace; background: #f6f8fa; border: 1px solid #ddd; padding: 6px; }");
		ss.addRule("code { font-family: monospace; background: #eee; padding: 2px 4px; border-radius: 3px; }");
		ss.addRule("blockquote { color: #666; margin-left: 8px; padding-left: 8px; border-left: 3px solid #ddd; }");
		ss.addRule("a { color: #1a73e8; text-decoration: none; }");
		ss.addRule("img { max-width: 100%; }");
		ss.addRule("body { margin: 1px; }");
		ss.addRule("div { margin: 1px; }");
		ss.addRule("p { margin-top: 1px; margin-bottom: 1px; }");

		textPane.setEditorKit(kit);
		textPane.setContentType("text/html");

		// Convert Markdown => HTML and sanitize from the tracked raw markdown
		String safeHtml = renderMarkdownToSafeHtml(rawMarkdown.toString());

		// align system/error messages to center by wrapping in a div when needed
		if (type == MessageType.SYSTEM || type == MessageType.ERROR) {
			safeHtml = "<div style=\"text-align:center\">" + safeHtml + "</div>";
		}

		textPane.setText(safeHtml);

		// Remove extra JTextPane margin
		textPane.setMargin(new java.awt.Insets(0, 0, 0, 0));

		// Add hyperlink handling (open in system browser)
		if (textPane instanceof JEditorPane) {
			((JEditorPane) textPane).addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(final HyperlinkEvent e) {
					if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						try {
							if (Desktop.isDesktopSupported()) {
								Desktop.getDesktop().browse(e.getURL().toURI());
							}
						} catch (IOException | URISyntaxException ex) {
							// ignore
						}
					}
				}
			});
		}

		// Try to set paragraph alignment for non-HTML fallback editors. With HTML content
		// alignment is handled above by wrapping or CSS rules.
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
		try {
			doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
		} catch (Exception ignored) {
		}

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
		StringBuilder sb = new StringBuilder("*Thinking");
		for (int i=0; i<thinkingStage; i++) {
			sb.append(".");
		}
		sb.append("*");
 		textPane.setText(renderMarkdownToSafeHtml(sb.toString()));
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
			// If we were showing a transient "thinking" indicator, stop it.
			if (thinkingStage >= 0) {
				thinkingStage = -1;
			}

			// Append incoming streaming tokens to the tracked raw markdown,
			// then re-render the sanitized HTML and replace the pane contents.
			rawMarkdown.append(text);

			try {
				final String safeHtml = renderMarkdownToSafeHtml(rawMarkdown.toString());
				textPane.setText(safeHtml);
				// Try to move caret to end so view scrolls with content
				try {
					textPane.setCaretPosition(textPane.getDocument().getLength());
				} catch (Exception ignore) {
				}
			} catch (Exception ex) {
				// If rendering fails for any reason, fall back to inserting plain text
				try {
					final StyledDocument doc = textPane.getStyledDocument();
					doc.insertString(doc.getLength(), text, null);
				} catch (BadLocationException e) {
					// Ignore insertion errors
				}
			}
		}
	}

	/**
	 * Gets the raw text content of this message.
	 *
	 * @return the text content as a string
	 */
	public String getText() {
		return rawMarkdown.toString();
	}

	/**
	 * Render markdown to HTML and sanitize the output with jsoup.
	 */
	private static String renderMarkdownToSafeHtml(final String markdown) {
		String md = markdown == null ? "" : markdown;
		// Guard against code fences that are on line ends instead of their own lines
	    md = md.replaceAll("([^\n])```", "$1\n```");

		final String html = MARKDOWN_RENDERER.render(MARKDOWN_PARSER.parse(md));

		final Safelist safelist = Safelist.relaxed()
			.addTags("pre", "code")
			.addAttributes("img", "src", "alt", "width", "height");

		final String clean = Jsoup.clean(html, safelist);
		return "<html><body>" + clean + "</body></html>";
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
