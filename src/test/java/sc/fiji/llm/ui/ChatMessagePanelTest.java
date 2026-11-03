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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

import org.junit.Test;

public class ChatMessagePanelTest {

	@Test
	public void preservesPromptLinkFragmentWhenSanitizingMarkdown() {
		final String html = ChatMessagePanel.renderMarkdownToSafeHtml(
			"[Try this prompt](" + ChatMessagePanel.PROMPT_LINK + ")\n\n" +
				"[Take a quick tour](" + ChatMessagePanel.TOUR_LINK + ")");

		assertTrue(html.contains("href=\"" + ChatMessagePanel.PROMPT_LINK + "\""));
		assertTrue(html.contains("href=\"" + ChatMessagePanel.TOUR_LINK + "\""));
	}

	@Test
	public void promptLinkPassesItsVisibleTextToHandler() {
		final String[] selectedPrompt = { null };
		final ChatMessagePanel panel = new ChatMessagePanel(
			ChatMessagePanel.MessageType.SYSTEM,
			"[Try this prompt](" + ChatMessagePanel.PROMPT_LINK + ")", 13f,
			prompt -> selectedPrompt[0] = prompt);
		final JEditorPane editorPane = findEditorPane(panel);
		final Element link = findLink(editorPane.getDocument().getDefaultRootElement(),
			ChatMessagePanel.PROMPT_LINK);
		assertNotNull(link);
		final HyperlinkEvent event = new HyperlinkEvent(editorPane,
			HyperlinkEvent.EventType.ACTIVATED, null, ChatMessagePanel.PROMPT_LINK,
			link);
		assertEquals(ChatMessagePanel.PROMPT_LINK, event.getDescription());

		for (final HyperlinkListener listener : editorPane.getHyperlinkListeners()) {
			listener.hyperlinkUpdate(event);
		}

		assertEquals("Try this prompt", selectedPrompt[0]);
	}

	@Test
	public void tourLinkInvokesHandler() {
		final boolean[] tourStarted = { false };
		final ChatMessagePanel panel = new ChatMessagePanel(
			ChatMessagePanel.MessageType.SYSTEM,
			"[Take a quick tour](" + ChatMessagePanel.TOUR_LINK + ")", 13f,
			null, () -> tourStarted[0] = true);
		final JEditorPane editorPane = findEditorPane(panel);
		final Element link = findLink(editorPane.getDocument().getDefaultRootElement(),
			ChatMessagePanel.TOUR_LINK);
		assertNotNull(link);
		final HyperlinkEvent event = new HyperlinkEvent(editorPane,
			HyperlinkEvent.EventType.ACTIVATED, null, ChatMessagePanel.TOUR_LINK,
			link);

		for (final HyperlinkListener listener : editorPane.getHyperlinkListeners()) {
			listener.hyperlinkUpdate(event);
		}

		assertTrue(tourStarted[0]);
	}

	private static JEditorPane findEditorPane(final Container parent) {
		if (parent instanceof JEditorPane editorPane) return editorPane;
		for (final Component component : parent.getComponents()) {
			if (component instanceof Container container) {
				final JEditorPane editorPane = findEditorPane(container);
				if (editorPane != null) return editorPane;
			}
		}
		return null;
	}

	private static Element findLink(final Element element, final String href) {
		final AttributeSet attributes = element.getAttributes();
		final Object anchor = attributes.getAttribute(HTML.Tag.A);
		if (href.equals(attributes.getAttribute(HTML.Attribute.HREF)) ||
			anchor instanceof AttributeSet anchorAttributes && href.equals(
				anchorAttributes.getAttribute(HTML.Attribute.HREF))) return element;
		for (int index = 0; index < element.getElementCount(); index++) {
			final Element link = findLink(element.getElement(index), href);
			if (link != null) return link;
		}
		return null;
	}
}
