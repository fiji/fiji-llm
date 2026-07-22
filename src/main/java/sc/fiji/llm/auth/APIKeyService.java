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

package sc.fiji.llm.auth;

import net.imagej.ImageJService;

/**
 * SciJava service for managing encrypted API keys for LLM providers. This
 * service handles secure storage and retrieval of API keys.
 */
public interface APIKeyService extends ImageJService {

	/**
	 * Get the API key for the specified provider.
	 *
	 * @param providerName the name of the provider (e.g., "OpenAI", "Anthropic")
	 * @return the API key, or null if not set
	 */
	String getApiKey(String providerName);

	/**
	 * Set the API key for the specified provider.
	 *
	 * @param providerName the name of the provider
	 * @param apiKey the API key to store
	 */
	void setApiKey(String providerName, String apiKey);

	/**
	 * Check if an API key is configured for the specified provider.
	 *
	 * @param providerName the name of the provider
	 * @return true if an API key is configured, false otherwise
	 */
	boolean hasApiKey(String providerName);

	/**
	 * Remove the API key for the specified provider.
	 *
	 * @param providerName the name of the provider
	 */
	void removeApiKey(String providerName);

	/**
	 * Validate the API key for the specified provider by attempting a test
	 * connection.
	 *
	 * @param providerName the name of the provider
	 * @param apiKey the API key to validate
	 * @return true if the API key is valid, false otherwise
	 */
	boolean validateApiKey(String providerName, String apiKey);
}
