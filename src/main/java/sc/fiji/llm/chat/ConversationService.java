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

import java.util.List;

import net.imagej.ImageJService;

/**
 * {@link ImageJService} for managing {@link Conversation}s.
 */
public interface ConversationService extends ImageJService {

	/**
	 * @return All saved conversations, most recently modified first. Names are
	 *         for display and need not be unique.
	 */
	List<Conversation> getConversations();

	/**
	 * Gets a saved conversation by ID.
	 *
	 * @param id The conversation ID
	 * @return The conversation, or null if not found
	 */
	Conversation getConversation(String id);

	/**
	 * Creates and registers a new conversation with a new unique ID.
	 *
	 * @param name The conversation's display name
	 * @param systemMessage The system message for the conversation
	 * @return The created conversation
	 */
	Conversation createConversation(String name,
		dev.langchain4j.data.message.SystemMessage systemMessage);

	/**
	 * Adds a conversation, replacing any with the same ID.
	 *
	 * @param newConversation The conversation to add
	 * @return true if successful
	 */
	boolean addConversation(Conversation newConversation);

	/**
	 * Removes a conversation.
	 *
	 * @param id The ID of the conversation to remove
	 * @return true if the conversation was found and removed
	 */
	boolean removeConversation(String id);

	/**
	 * Deletes a conversation permanently from disk and memory.
	 *
	 * @param id The ID of the conversation to delete
	 * @return true if the conversation was found and deleted
	 */
	boolean deleteConversation(String id);
}
