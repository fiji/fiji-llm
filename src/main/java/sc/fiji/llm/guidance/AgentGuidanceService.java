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
package sc.fiji.llm.guidance;

import java.util.List;
import java.util.Optional;

import org.scijava.plugin.SingletonService;

import net.imagej.ImageJService;

/** Service for discovering and retrieving curated guidance for AI agents. */
public interface AgentGuidanceService extends SingletonService<AgentGuide>,
	ImageJService
{

	int DEFAULT_MAX_CONTENT_CHARACTERS = 2400;
	int MAX_CONTENT_CHARACTERS = 12000;

	/**
	 * Lists guide metadata without including guide content.
	 *
	 * @return guides in stable identifier order
	 */
	List<AgentGuideMetadata> listDocuments();

	/**
	 * Lists the topic categories present in the loaded guides.
	 *
	 * @return an unmodifiable, sorted list of canonical, lower-case topic keywords
	 */
	List<String> getAvailableTopics();

	/**
	 * Lists guide metadata for guides assigned to one topic category.
	 *
	 * @param topic one topic category, matched case-insensitively
	 * @return matching guides in stable identifier order
	 */
	List<AgentGuideMetadata> search(String topic);

	/**
	 * Reads one guide using the default content bound.
	 *
	 * @param id stable guide identifier
	 * @return bounded guide content, or empty when the identifier is unknown
	 */
	default Optional<String> read(final String id) {
		return read(id, DEFAULT_MAX_CONTENT_CHARACTERS);
	}

	/**
	 * Reads one guide with a caller-requested, service-enforced content bound.
	 *
	 * @param id stable guide identifier
	 * @param maxContentCharacters maximum returned content length
	 * @return bounded guide content, or empty when the identifier is unknown
	 */
	Optional<String> read(String id, int maxContentCharacters);
}
