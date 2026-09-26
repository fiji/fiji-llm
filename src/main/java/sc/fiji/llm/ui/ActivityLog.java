/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

/**
 * A collapsible record of what the assistant did while producing a response:
 * its thinking and each tool call with arguments, outcome, and result. It is
 * collapsed by default so it does not distract from the response itself. Must
 * be used on the EDT.
 */
public class ActivityLog extends JPanel {

	private static final int MAX_DETAILS_HEIGHT = 240;
	private static final int MAX_RESULT_LENGTH = 600;
	private static final int RENDER_DELAY_MS = 250;
	private static final int CODE_LINE_LENGTH = 50;
	private static final String EXPANDED = "▾";
	private static final String COLLAPSED = "▸";

	private final List<Entry> entries = new ArrayList<>();
	private final JLabel toggle;
	private final JTextPane details;
	private final JScrollPane detailsScroll;
	private final Timer renderTimer;
	private final boolean arrowsSupported;
	private boolean expanded;
	private boolean finished;
	private long elapsedSeconds;

	public ActivityLog(final float fontSize) {
		super(new MigLayout("insets 0, wrap 1, hidemode 3, fillx", "[grow, fill]",
			""));
		setOpaque(false);

		toggle = new JLabel();
		toggle.setFont(toggle.getFont().deriveFont(Font.PLAIN, fontSize - 1));
		toggle.setForeground(new Color(70, 90, 120));
		toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		toggle.setToolTipText("Show or hide what the assistant did");
		toggle.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				setExpanded(!expanded);
			}
		});
		arrowsSupported = toggle.getFont().canDisplayUpTo(EXPANDED +
			COLLAPSED) == -1;

		details = createDetailsPane(fontSize - 2);
		detailsScroll = new JScrollPane(details,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
		{

			@Override
			public Dimension getPreferredSize() {
				final Dimension size = super.getPreferredSize();
				final int width = getViewport().getWidth();
				if (width > 0) {
					// Measure the wrapped height at the actual width.
					details.setSize(width, Short.MAX_VALUE);
					final java.awt.Insets insets = getInsets();
					size.height = details.getPreferredSize().height + insets.top +
						insets.bottom;
				}
				size.height = Math.min(size.height, MAX_DETAILS_HEIGHT);
				return size;
			}
		};
		detailsScroll.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0,
			new Color(144, 202, 249)));
		detailsScroll.setOpaque(false);
		detailsScroll.getViewport().setOpaque(false);
		detailsScroll.setVisible(false);

		renderTimer = new Timer(RENDER_DELAY_MS, e -> render());
		renderTimer.setRepeats(false);

		add(toggle);
		add(detailsScroll, "gapleft 4");
		setVisible(false);
		updateToggle();
	}

	/** Appends streamed thinking text, starting a new entry after a tool call. */
	public void appendThinking(final String text) {
		if (text == null || text.isEmpty()) return;
		final Entry last = entries.isEmpty() ? null : entries.get(entries.size() -
			1);
		if (last != null && last.toolName == null) last.text.append(text);
		else entries.add(Entry.thinking(text));
		changed();
	}

	/** Records the start of a tool call. */
	public void toolStarted(final String name, final String arguments) {
		entries.add(Entry.tool(name, arguments));
		changed();
	}

	/** Records the outcome of the most recent unfinished call to the tool. */
	public void toolFinished(final String name, final boolean failed,
		final long millis, final String result)
	{
		for (int i = entries.size() - 1; i >= 0; i--) {
			final Entry entry = entries.get(i);
			if (name.equals(entry.toolName) && entry.millis < 0) {
				entry.failed = failed;
				entry.millis = millis;
				entry.text.append(result == null ? "" : result);
				break;
			}
		}
		changed();
	}

	/** Marks the response as complete, summarizing the activity. */
	public void finish(final long seconds) {
		finished = true;
		elapsedSeconds = seconds;
		updateToggle();
		render();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/** @return a short summary, such as "Thought and used 2 tools (12s)" */
	String summary() {
		final long toolCount = entries.stream().filter(e -> e.toolName != null)
			.count();
		final long failures = entries.stream().filter(e -> e.failed).count();
		final boolean thought = entries.stream().anyMatch(e -> e.toolName == null);
		final StringBuilder sb = new StringBuilder();
		if (thought) sb.append("Thought");
		if (toolCount > 0) {
			sb.append(thought ? " and used " : "Used ").append(toolCount).append(
				toolCount == 1 ? " tool" : " tools");
			if (failures > 0) sb.append(", ").append(failures).append(" failed");
		}
		if (finished) sb.append(" (").append(elapsedSeconds).append("s)");
		return sb.toString();
	}

	private void setExpanded(final boolean expand) {
		expanded = expand;
		detailsScroll.setVisible(expand);
		updateToggle();
		if (expand) render();
		revalidateBubble();
	}

	private void changed() {
		setVisible(true);
		updateToggle();
		if (expanded && !renderTimer.isRunning()) renderTimer.start();
	}

	private void updateToggle() {
		final String arrow = arrowsSupported ? (expanded ? EXPANDED : COLLAPSED)
			: (expanded ? "[-]" : "[+]");
		toggle.setText(arrow + " " + summary());
	}

	private void render() {
		if (!expanded) return;
		details.setText(ChatMessagePanel.renderMarkdownToSafeHtml(toMarkdown()));
		revalidateBubble();
		// Follow new activity while the response is in progress.
		if (!finished) {
			SwingUtilities.invokeLater(() -> detailsScroll.getVerticalScrollBar()
				.setValue(detailsScroll.getVerticalScrollBar().getMaximum()));
		}
	}

	String toMarkdown() {
		final StringBuilder md = new StringBuilder();
		for (final Entry entry : entries) {
			if (entry.toolName == null) {
				md.append("**Thinking**\n\n").append(entry.text).append("\n\n");
				continue;
			}
			md.append("**Tool** `").append(entry.toolName).append("` ");
			if (entry.millis < 0) md.append("*running*");
			else md.append(entry.failed ? "*failed* after " : "*done* in ").append(
				entry.millis).append(" ms");
			md.append("\n\n");
			appendCode(md, entry.arguments);
			if (entry.millis >= 0) appendCode(md, truncate(entry.text.toString()));
		}
		return md.toString();
	}

	private static void appendCode(final StringBuilder md, final String text) {
		if (text == null || text.isBlank()) return;
		// Note: tildes, since ChatMessagePanel splits backtick runs mid-line.
		md.append("~~~~\n").append(hardWrap(text.strip())).append("\n~~~~\n\n");
	}

	/**
	 * Breaks long lines, since Swing HTML cannot wrap preformatted text and
	 * tool arguments and results are often single-line JSON.
	 */
	static String hardWrap(final String text) {
		final StringBuilder sb = new StringBuilder();
		for (final String line : text.split("\n", -1)) {
			if (sb.length() > 0) sb.append("\n");
			for (int i = 0; i < line.length(); i += CODE_LINE_LENGTH) {
				if (i > 0) sb.append("\n");
				sb.append(line, i, Math.min(line.length(), i + CODE_LINE_LENGTH));
			}
		}
		return sb.toString();
	}

	private static String truncate(final String text) {
		if (text.length() <= MAX_RESULT_LENGTH) return text;
		return text.substring(0, MAX_RESULT_LENGTH) + "\n... (" + (text.length() -
			MAX_RESULT_LENGTH) + " more characters)";
	}

	private void revalidateBubble() {
		revalidateParent();
		// Note: again after layout, since the wrapped height depends on the width.
		SwingUtilities.invokeLater(this::revalidateParent);
	}

	private void revalidateParent() {
		// Note: revalidating the scroll pane invalidates it and every ancestor.
		detailsScroll.revalidate();
		revalidate();
		repaint();
	}

	private static JTextPane createDetailsPane(final float fontSize) {
		final JTextPane pane = new JTextPane() {

			// Note: always wrap prose, even when a code line is wider than the view.
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return true;
			}
		};
		pane.setEditable(false);
		pane.setOpaque(false);
		final HTMLEditorKit kit = new HTMLEditorKit();
		final StyleSheet ss = kit.getStyleSheet();
		ss.addRule("body { font-family: Dialog, Arial, sans-serif; font-size: " +
			(int) fontSize + "px; color: #555; margin: 2px 4px; }");
		ss.addRule("pre { font-family: monospace; background: #f6f8fa; border: 1px solid #ddd; padding: 4px; }");
		ss.addRule("code { font-family: monospace; }");
		ss.addRule("p { margin-top: 1px; margin-bottom: 3px; }");
		pane.setEditorKit(kit);
		pane.setContentType("text/html");
		// Note: never move the caret, so updates do not scroll the chat.
		((DefaultCaret) pane.getCaret()).setUpdatePolicy(
			DefaultCaret.NEVER_UPDATE);
		return pane;
	}

	private static class Entry {

		private final String toolName;
		private final String arguments;
		private final StringBuilder text = new StringBuilder();
		private boolean failed;
		private long millis = -1;

		private Entry(final String toolName, final String arguments) {
			this.toolName = toolName;
			this.arguments = arguments;
		}

		static Entry thinking(final String text) {
			final Entry entry = new Entry(null, null);
			entry.text.append(text);
			return entry;
		}

		static Entry tool(final String name, final String arguments) {
			return new Entry(name, arguments);
		}
	}
}
