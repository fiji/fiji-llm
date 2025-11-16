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

package sc.fiji.llm.assistant;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;

/**
 * The main Fiji/ImageJ assistant interface powered by LangChain4j. This
 * interface defines the capabilities of the LLM assistant. LangChain4j
 * automatically generates an implementation of this interface.
 */
public interface FijiAssistant {

	/**
	 * General chat interaction with structured messages.
	 *
	 * @param chatRequest a {@link ChatRequest} containing messages and parameters
	 * @return a {@link ChatResponse} with the assistant's response
	 */
	AiMessage chat(ChatRequest chatRequest);

	/**
	 * Streaming chat interaction for real-time responses.
	 *
	 * @param chatRequest a {@link ChatRequest} containing messages and parameters
	 * @return a token stream for progressive response rendering
	 */
	TokenStream chatStreaming(ChatRequest chatRequest);
}
