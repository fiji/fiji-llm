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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.scijava.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.langchain4j.data.message.SystemMessage;
import sc.fiji.llm.Setup;

public class ConversationToolPluginTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Context context;
	private DefaultConversationService service;
	private ConversationToolPlugin tool;

	@Before
	public void setUp() throws IOException {
		context = Setup.context();
		service = (DefaultConversationService) context.getService(
			ConversationService.class);
		service.setConversationDirectory(folder.newFolder("history"));
		tool = context.getService(sc.fiji.llm.tools.AiToolService.class).getInstance(
			ConversationToolPlugin.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testNamesUnnamedConversationWithTimestamp() {
		final Conversation conversation = service.createConversation(null,
			SystemMessage.from("sys"));

		final JsonObject result = JsonParser.parseString(tool.nameConversation(
			conversation.id(), "Count cells")).getAsJsonObject();

		assertEquals(conversation.id(), result.get("conversation_id").getAsString());
		assertTrue(result.get("display_name").getAsString().matches(
			"Count cells \\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\]"));
		assertEquals(result.get("display_name").getAsString(), conversation
			.displayName());
	}

	@Test
	public void testRejectsBlankAndAlreadyNamedConversations() {
		final Conversation conversation = service.createConversation(null,
			SystemMessage.from("sys"));

		final JsonObject blank = JsonParser.parseString(tool.nameConversation(
			conversation.id(), " ")).getAsJsonObject();
		assertTrue(blank.has("error"));
		assertNull(conversation.displayName());

		tool.nameConversation(conversation.id(), "Count cells");
		final JsonObject duplicate = JsonParser.parseString(tool.nameConversation(
			conversation.id(), "Different name")).getAsJsonObject();
		assertTrue(duplicate.has("error"));
	}
}
