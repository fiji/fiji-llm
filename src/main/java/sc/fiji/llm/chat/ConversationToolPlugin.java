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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolScope;

/** Tools for naming persisted Fiji conversations. */
@Plugin(type = AiToolPlugin.class)
public class ConversationToolPlugin extends AbstractAiToolPlugin {

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm");

	@Parameter
	private ConversationService conversationService;

	public ConversationToolPlugin() {
		super(ConversationToolPlugin.class);
	}

	@Override
	public String getName() {
		return "Conversation Tools";
	}

	@Override
	public String getToolScope() {
		return ToolScope.CHAT;
	}

	@Tool(value = { "Name a conversation based on its conversation history. When a required conversation-naming action is provided, call this tool before addressing the user's request. Do not include a timestamp in the name because one is added automatically." }, name = "fiji_conversation_name")
	public String nameConversation(
		@P(name = "conversation_id", value = "Conversation UUID from the required naming action") final String conversationId,
		@P(name = "name", value = "A concise name based on the conversation history, without a timestamp") final String name)
	{
		try {
			if (name == null || name.isBlank()) {
				return jsonError("Conversation name cannot be blank");
			}
			final String timestampedName = name.trim() + " [" + LocalDateTime.now()
				.format(TIMESTAMP_FORMAT) + "]";
			final Conversation conversation = conversationService.nameConversation(
				conversationId, timestampedName);
			final JsonObject result = new JsonObject();
			result.addProperty("conversation_id", conversation.id());
			result.addProperty("display_name", conversation.displayName());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_conversation_name: " + e
				.getMessage());
		}
	}
}
