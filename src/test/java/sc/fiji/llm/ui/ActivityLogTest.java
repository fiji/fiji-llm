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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.Test;

public class ActivityLogTest {

	@Test
	public void testSummaryDescribesThinkingAndTools() {
		final ActivityLog log = new ActivityLog(13f);
		assertTrue(log.isEmpty());
		log.appendThinking("Let me ");
		log.appendThinking("check.");
		log.toolStarted("fiji_image_list", "{}");
		log.toolFinished("fiji_image_list", false, 12, "[]");
		log.toolStarted("fiji_image_view", "{\"image_id\": 3}");
		log.toolFinished("fiji_image_view", true, 5, "No such image");
		assertFalse(log.isEmpty());
		assertEquals("Thoughts and 2 tool uses, 1 failed", log.summary());

		log.finish(7);
		assertEquals("Thoughts and 2 tool uses, 1 failed (7s)", log.summary());
	}

	@Test
	public void testLiveActivityExpandsAndCompletedActivityCollapses() {
		final ActivityLog log = new ActivityLog(13f);
		final Component details = log.getComponent(1);

		log.appendThinking("Let me check.");
		assertTrue(details.isVisible());

		log.finish(7);
		assertFalse(details.isVisible());
	}

	@Test
	public void testMarkdownRecordsEachStep() {
		final ActivityLog log = new ActivityLog(13f);
		log.appendThinking("Let me check.");
		log.toolStarted("fiji_image_list", "{}");
		log.toolFinished("fiji_image_list", false, 12, "has ``` fence");
		log.appendThinking("Now view it.");
		log.toolStarted("fiji_image_view", "{\"image_id\": 3}");

		final String md = log.toMarkdown();
		assertTrue(md, md.contains("**Thinking**\n\nLet me check."));
		assertTrue(md, md.contains("`fiji_image_list` *done* in 12 ms"));
		assertTrue(md, md.contains("~~~~\nhas ``` fence\n~~~~"));
		assertTrue(md, md.contains("**Thinking**\n\nNow view it."));
		assertTrue(md, md.contains("`fiji_image_view` *running*"));
	}

	@Test
	public void testHardWrapBreaksLongLines() {
		final String wrapped = ActivityLog.hardWrap("x".repeat(110) + "\nshort");
		assertEquals("x".repeat(50) + "\n" + "x".repeat(50) + "\n" + "x".repeat(10) +
			"\nshort", wrapped);
	}

	@Test
	public void testSummaryForToolsWithoutThinking() {
		final ActivityLog log = new ActivityLog(13f);
		log.toolStarted("fiji_image_list", "{}");
		assertEquals("1 tool use", log.summary());
	}

	@Test
	public void testLookAndFeelUpdateCanBeRequestedOffEdt() throws Exception {
		final ChatMessagePanel[] panel = new ChatMessagePanel[1];
		SwingUtilities.invokeAndWait(() -> panel[0] = new ChatMessagePanel(
			ChatMessagePanel.MessageType.ASSISTANT, "response"));
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final Thread worker = new Thread(() -> {
			try {
				SwingUtilities.updateComponentTreeUI(panel[0]);
			}
			catch (final Throwable error) {
				failure.set(error);
			}
		}, "laf-update-test");
		worker.start();
		worker.join(5000);
		assertFalse("LAF update worker did not finish", worker.isAlive());
		SwingUtilities.invokeAndWait(() -> {});
		assertNull(failure.get());
	}
}
