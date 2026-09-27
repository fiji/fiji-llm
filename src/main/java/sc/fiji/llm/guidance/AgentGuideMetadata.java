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
package sc.fiji.llm.guidance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Metadata for one curated guidance document. */
public final class AgentGuideMetadata {

	public enum Authority {
		/** Authoritative external documentation or API reference. */
		OFFICIAL,
		/** Guidance authored by the Fiji-LLM project based on project behavior. */
		PROJECT_AUTHORED,
		/** Illustrative code or workflow that is not a universal guarantee. */
		EXAMPLE,
		/** Community experience that may be version-specific or unverified. */
		COMMUNITY
	}

	private final String id;
	private final String title;
	private final String summary;
	private final List<String> topics;
	private final Authority authority;

	/**
	 * Creates guidance metadata.
	 * <p>
	 * Lists are copied defensively; null lists become empty lists. Topic values
	 * are trimmed and normalized to lower case.
	 * </p>
	 *
	 * @param id stable identifier used by guidance search and read operations
	 * @param title human-readable document title
	 * @param summary concise description of the document's scope
	 * @param topics taxonomy terms used to classify the document
	 * @param authority trust classification for the document source
	 */
	public AgentGuideMetadata(final String id, final String title,
		final String summary, final List<String> topics, final Authority authority)
	{
		this.id = requireText(id, "id");
		this.title = requireText(title, "title");
		this.summary = requireText(summary, "summary");
		this.topics = immutableTopics(topics);
		this.authority = Objects.requireNonNull(authority, "authority");
	}

	public String id() {
		return id;
	}

	public String title() {
		return title;
	}

	public String summary() {
		return summary;
	}

	public List<String> topics() {
		return topics;
	}

	public Authority authority() {
		return authority;
	}

	private static String requireText(final String value, final String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty");
		}
		return value.trim();
	}

	private static List<String> immutableTopics(final List<String> values) {
		if (values == null) return Collections.emptyList();
		final List<String> topics = new ArrayList<>();
		for (final String value : values) {
			if (value != null && !value.trim().isEmpty()) {
				final String topic = value.trim().toLowerCase(Locale.ROOT);
				if (!topics.contains(topic)) topics.add(topic);
			}
		}
		return Collections.unmodifiableList(topics);
	}
}
