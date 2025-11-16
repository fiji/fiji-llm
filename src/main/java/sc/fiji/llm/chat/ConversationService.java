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

package sc.fiji.llm.chat;

import java.util.List;

import org.scijava.service.SciJavaService;

/**
 * {@link SciJavaService} for managing {@link Conversation}s.
 */
public interface ConversationService extends SciJavaService {

	/**
	 * @return Names of all saved conversations
	 */
	List<String> getConversationNames();

	/**
	 * Gets a saved conversation by name.
	 *
	 * @param name The conversation name
	 * @return The conversation, or null if not found
	 */
	Conversation getConversation(String name);

	/**
	 * Creates and registers a new conversation.
	 *
	 * @param name The conversation name
	 * @param systemMessage The system message for the conversation
	 * @return The created conversation
	 */
	Conversation createConversation(String name,
		dev.langchain4j.data.message.SystemMessage systemMessage);

	/**
	 * Adds/updates a conversation.
	 *
	 * @param newConversation The conversation to add
	 * @return true if successful
	 */
	boolean addConversation(Conversation newConversation);

	/**
	 * Removes a conversation.
	 *
	 * @param name The conversation name to remove
	 * @return true if the conversation was found and removed
	 */
	boolean removeConversation(String name);

	/**
	 * Deletes a conversation permanently from disk and memory.
	 *
	 * @param name The conversation name to delete
	 * @return true if the conversation was found and deleted
	 */
	boolean deleteConversation(String name);
}
