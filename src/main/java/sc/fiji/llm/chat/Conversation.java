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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;

/**
 * Represents an ongoing conversation as a series of messages with separate
 * display and memory context.
 */
public class Conversation {

	private final List<Message> messages;
	private final String name;
	private final SystemMessage systemMessage;

	public Conversation(String name, SystemMessage systemMessage) {
		this.name = name;
		messages = new ArrayList<>();
		this.systemMessage = systemMessage;
	}

	public void addMessage(String displayMessage, ChatMessage memoryMessage) {
		messages.add(new Message(displayMessage, memoryMessage));
	}

	/**
	 * @return The {@link SystemMessage} for this conversation.
	 */
	public SystemMessage systemMessage() {
		return systemMessage;
	}

	/**
	 * @return The list of messages in this conversation, in chronological order
	 *         (oldest first).
	 */
	public List<Message> messages() {
		return Collections.unmodifiableList(messages);
	}

	/**
	 * @return A display name for this conversation
	 */
	public String name() {
		return name;
	}

	/**
	 * Helper class represents one message in a conversation
	 */
	public static class Message {

		private final String displayMessage;
		private final ChatMessage memoryMessage;

		public Message(String displayMessage, ChatMessage memoryMessage) {
			this.displayMessage = displayMessage;
			this.memoryMessage = memoryMessage;
		}

		public String display() {
			return displayMessage;
		}

		public ChatMessage memory() {
			return memoryMessage;
		}

		@Override
		public String toString() {
			return "Message [displayMessage=" + displayMessage + ", memoryMessage=" +
				memoryMessage + "]";
		}
	}
}
