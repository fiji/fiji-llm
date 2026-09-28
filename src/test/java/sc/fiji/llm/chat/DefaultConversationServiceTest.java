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

package sc.fiji.llm.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.scijava.Context;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import sc.fiji.llm.Setup;

public class DefaultConversationServiceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Context context;
	private DefaultConversationService service;
	private File dir;

	@Before
	public void setUp() throws IOException {
		context = Setup.context();
		service = (DefaultConversationService) context.getService(
			ConversationService.class);
		dir = folder.newFolder("history");
		service.setConversationDirectory(dir);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testSameNamedConversationsStayDistinct() {
		final Conversation first = createWithMessage("Count cells", "first");
		final Conversation second = createWithMessage("Count cells", "second");
		assertNotEquals(first.id(), second.id());
		assertEquals(List.of(second, first), service.getConversations());

		// Save, then reload from disk.
		service.setConversationDirectory(dir);
		assertEquals(2, dir.listFiles().length);
		assertEquals("first", text(service.getConversation(first.id())));
		assertEquals("second", text(service.getConversation(second.id())));
	}

	@Test
	public void testDeleteRemovesOnlyThatConversation() {
		final Conversation first = createWithMessage("Count cells", "first");
		final Conversation second = createWithMessage("Count cells", "second");
		service.setConversationDirectory(dir);

		assertTrue(service.deleteConversation(first.id()));
		assertNull(service.getConversation(first.id()));
		assertEquals("second", text(service.getConversation(second.id())));
		assertFalse(new File(dir, first.id() + ".json").exists());
		assertTrue(new File(dir, second.id() + ".json").exists());
	}

	@Test
	public void testLoadsLegacyFileUsingFileNameAsId() throws IOException {
		Files.writeString(new File(dir, "Count_cells.json").toPath(),
			"{\"name\":\"Count cells\",\"systemMessage\":\"sys\",\"messages\":[" +
				"{\"displayMessage\":\"hi\",\"memoryMessage\":{\"type\":\"USER\"," +
				"\"content\":\"hi\"}}]}", StandardCharsets.UTF_8);
		service.setConversationDirectory(dir);

		final Conversation legacy = service.getConversation("Count_cells");
		assertEquals("Count cells", legacy.displayName());
		legacy.addMessage("more", UserMessage.from("more"));

		// Saving writes back to the same file rather than creating another.
		service.setConversationDirectory(dir);
		assertEquals(1, dir.listFiles().length);
		assertEquals(2, service.getConversation("Count_cells").messages().size());
	}

	@Test
	public void testUnnamedConversationCanBeNamedOnceAndPersisted() {
		final Conversation conversation = service.createConversation(null,
			SystemMessage.from("sys"));
		assertNull(conversation.displayName());

		service.nameConversation(conversation.id(), "Count cells [2026-09-28 12:00]");
		assertEquals("Count cells [2026-09-28 12:00]", conversation.displayName());

		try {
			service.nameConversation(conversation.id(), "Different name");
			throw new AssertionError("Expected already-named conversation to fail");
		}
		catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("already has a display name"));
		}

		service.setConversationDirectory(dir);
		assertEquals("Count cells [2026-09-28 12:00]", service.getConversation(
			conversation.id()).displayName());
	}

	private Conversation createWithMessage(final String name,
		final String text)
	{
		final Conversation conversation = service.createConversation(name,
			SystemMessage.from("sys"));
		conversation.addMessage(text, UserMessage.from(text));
		return conversation;
	}

	private static String text(final Conversation conversation) {
		return conversation.messages().get(0).display();
	}
}
