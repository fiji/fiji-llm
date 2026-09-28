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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Serializable container for a Conversation, used for JSON persistence.
 */
public class SerializedConversation {

	private String id;
	@SerializedName(value = "displayName", alternate = { "name" })
	private String displayName;
	private String systemMessage;
	private List<SerializedConversationMessage> messages = new ArrayList<>();

	// No-arg constructor for GSON
	public SerializedConversation() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
		return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName) &&
			Objects.equals(systemMessage, that.systemMessage) && Objects.equals(
				messages, that.messages);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, displayName, systemMessage, messages);
	}

	/**
	 * Represents a single message in a serialized conversation.
	 */
	public static class SerializedConversationMessage {

		private String displayMessage;
		private SerializedMessage memoryMessage;
		private ActivityRecord activity;

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

		public ActivityRecord getActivity() {
			return activity;
		}

		public void setActivity(ActivityRecord activity) {
			this.activity = activity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SerializedConversationMessage that = (SerializedConversationMessage) o;
			return Objects.equals(displayMessage, that.displayMessage) && Objects
				.equals(memoryMessage, that.memoryMessage) &&
				Objects.equals(activity, that.activity);
		}

		@Override
		public int hashCode() {
			return Objects.hash(displayMessage, memoryMessage, activity);
		}
	}
}
