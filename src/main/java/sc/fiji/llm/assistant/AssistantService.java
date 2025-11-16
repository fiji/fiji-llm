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

import org.scijava.service.SciJavaService;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

/**
 * SciJava service for creating LLM-powered assistants.
 * This service creates AiService instances (LangChain4j assistants) with
 * specified providers, models, and optional tools.
 */
public interface AssistantService extends SciJavaService {

	/**
	 * Create an AI service instance (LangChain4j assistant) with the given interface,
	 * chat model, optional memory, request parameters, and all available tools.
	 *
	 * @param <T> the assistant interface type
	 * @param assistantInterface the interface class defining the assistant methods
	 * @param providerName the name of the LLM provider
	 * @param modelName the name of the model within that provider
	 * @param chatMemory optional chat memory to persist conversation history
	 * @param defaultChatParameters optional request parameters (temperature, top_p, etc.)
	 * @return an implementation of the assistant interface
	 * @throws IllegalArgumentException if the provider is not found
	 * @throws IllegalStateException if no API key is configured for the provider
	 */
	<T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName, ChatMemory chatMemory, ChatRequestParameters defaultChatParameters);
}
