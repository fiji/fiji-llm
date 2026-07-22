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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Utility class for converting between ChatMessage and SerializedMessage.
 */
public class ChatMessageConverter {

	/**
	 * Converts a ChatMessage to a SerializedMessage for JSON persistence.
	 */
	public static SerializedMessage toSerialized(ChatMessage message) {
		if (message instanceof SystemMessage) {
			return new SerializedMessage("SYSTEM", ((SystemMessage) message).text());
		}
		else if (message instanceof UserMessage) {
			return new SerializedMessage("USER", ((UserMessage) message)
				.singleText());
		}
		else if (message instanceof AiMessage) {
			return new SerializedMessage("AI", ((AiMessage) message).text());
		}
		else if (message instanceof ToolExecutionResultMessage) {
			return new SerializedMessage("TOOL_EXECUTION_RESULT",
				((ToolExecutionResultMessage) message).text());
		}
		else {
			throw new IllegalArgumentException("Unsupported message type: " + message
				.getClass().getName());
		}
	}

	/**
	 * Converts a SerializedMessage back to a ChatMessage.
	 */
	public static ChatMessage fromSerialized(SerializedMessage serialized) {
		switch (serialized.getType()) {
			case "SYSTEM":
				return new SystemMessage(serialized.getContent());
			case "USER":
				return new UserMessage(serialized.getContent());
			case "AI":
				return new AiMessage(serialized.getContent());
			case "TOOL_EXECUTION_RESULT":
				// For now, treat as AI message. This may need enhancement
				// if you need to preserve tool execution metadata
				return new AiMessage(serialized.getContent());
			default:
				throw new IllegalArgumentException("Unknown message type: " + serialized
					.getType());
		}
	}
}
