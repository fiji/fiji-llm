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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

/** Default implementation of {@link AgentGuidanceService}. */
@Plugin(type = Service.class)
public class DefaultAgentGuidanceService extends
	AbstractSingletonService<AgentGuide> implements AgentGuidanceService
{
	private List<String> availableTopics = Collections.emptyList();

	@Override
	public void initialize() {
		final Set<String> topics = new TreeSet<>();
		for (final AgentGuide guide : getInstances()) topics.addAll(guide.metadata().topics());
		availableTopics = Collections.unmodifiableList(new ArrayList<>(topics));
	}

	@Override
	public Class<AgentGuide> getPluginType() {
		return AgentGuide.class;
	}

	@Override
	public List<AgentGuideMetadata> listDocuments() {
		return guidesById().values().stream().sorted(Comparator.comparing(guide -> guide
			.metadata().id())).map(AgentGuide::metadata).collect(Collectors.toList());
	}

	@Override
	public List<String> getAvailableTopics() {
		return availableTopics;
	}

	@Override
	public List<AgentGuideMetadata> search(final String topic) {
		if (topic == null || topic.trim().isEmpty()) return Collections.emptyList();
		final String normalizedTopic = topic.trim().toLowerCase(Locale.ROOT);
		return guidesById().values().stream().filter(guide -> guide.metadata().topics()
			.contains(normalizedTopic)).sorted(Comparator.comparing(guide -> guide.metadata()
			.id())).map(AgentGuide::metadata).collect(Collectors.toList());
	}

	@Override
	public Optional<String> read(final String id,
		final int maxContentCharacters)
	{
		if (id == null || id.trim().isEmpty()) return Optional.empty();
		final int contentLimit = contentLimit(maxContentCharacters);
		final AgentGuide guide = guidesById().get(id.trim());
		if (guide == null) return Optional.empty();
		final String content = guide.content();
		final String boundedContent = content.length() <= contentLimit ? content : content
			.substring(0, contentLimit);
		return Optional.of(boundedContent);
	}

	private Map<String, AgentGuide> guidesById() {
		final Map<String, AgentGuide> guides = new HashMap<>();
		for (final AgentGuide guide : getInstances()) {
			final String id = guide.metadata().id();
			if (guides.put(id, guide) != null) throw new IllegalStateException(
				"Duplicate guidance document: " + id);
		}
		return guides;
	}

	private static int contentLimit(final int requested) {
		if (requested <= 0) throw new IllegalArgumentException(
			"maxContentCharacters must be positive");
		return Math.min(requested, AgentGuidanceService.MAX_CONTENT_CHARACTERS);
	}
}
