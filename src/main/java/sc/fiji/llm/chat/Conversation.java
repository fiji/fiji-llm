/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
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
