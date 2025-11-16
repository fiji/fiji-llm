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
package sc.fiji.llm.provider;

import org.scijava.plugin.Parameter;

import sc.fiji.llm.auth.APIKeyService;

public abstract class AbstractLLMProvider implements LLMProvider {
    @Parameter
    private APIKeyService apiKeyService;

    protected String apiKey() {
        String apiKey = apiKeyService.getApiKey(getName());
        if (apiKey == null) {
			throw new IllegalStateException("No API key configured for provider: " + getName());
		}
        return apiKey;
    }
}
