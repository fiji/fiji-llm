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
package sc.fiji.llm.ui;

import org.scijava.service.SciJavaService;

/**
 * Service interface for launching chatbot UIs.
 */
public interface ChatbotService extends SciJavaService {

    /**
     * Launch a chat window with the given assistant.
     * 
     * @param assistant the assistant instance (will be recreated with memory)
     * @param title the window title
     * @param providerName the name of the LLM provider (e.g., "OpenAI")
     * @param modelName the name of the model (e.g., "gpt-4")
     */
    void launchChat(String title, String providerName, String modelName);
}
