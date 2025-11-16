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
import java.util.List;
import java.util.Objects;

/**
 * Serializable container for a Conversation, used for JSON persistence.
 */
public class SerializedConversation {

	private String name;
	private String systemMessage;
	private List<SerializedConversationMessage> messages = new ArrayList<>();

	// No-arg constructor for GSON
	public SerializedConversation() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSystemMessage() {
		return systemMessage;
	}

	public void setSystemMessage(String systemMessage) {
		this.systemMessage = systemMessage;
	}

	public List<SerializedConversationMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<SerializedConversationMessage> messages) {
		this.messages = messages;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SerializedConversation that = (SerializedConversation) o;
		return Objects.equals(name, that.name) && Objects.equals(systemMessage,
			that.systemMessage) && Objects.equals(messages, that.messages);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, systemMessage, messages);
	}

	/**
	 * Represents a single message in a serialized conversation.
	 */
	public static class SerializedConversationMessage {

		private String displayMessage;
		private SerializedMessage memoryMessage;

		public SerializedConversationMessage() {}

		public String getDisplayMessage() {
			return displayMessage;
		}

		public void setDisplayMessage(String displayMessage) {
			this.displayMessage = displayMessage;
		}

		public SerializedMessage getMemoryMessage() {
			return memoryMessage;
		}

		public void setMemoryMessage(SerializedMessage memoryMessage) {
			this.memoryMessage = memoryMessage;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SerializedConversationMessage that = (SerializedConversationMessage) o;
			return Objects.equals(displayMessage, that.displayMessage) && Objects
				.equals(memoryMessage, that.memoryMessage);
		}

		@Override
		public int hashCode() {
			return Objects.hash(displayMessage, memoryMessage);
		}
	}
}
