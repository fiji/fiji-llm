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

import java.util.Locale;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/** Read-only AI tools for discovering and reading curated Fiji guidance. */
@Plugin(type = AiToolPlugin.class)
public class GuidanceToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private AgentGuidanceService agentGuidanceService;

	public GuidanceToolPlugin() {
		super(GuidanceToolPlugin.class);
	}

	@Override
	public String getName() {
		return "Fiji Guidance";
	}

	@Tool(value = { "List the exact topic keywords available in the curated Fiji guidance catalog. Use this before fiji_guide_search" }, name = "fiji_guide_topics")
	public String listTopics() {
		try {
			final JsonArray topics = new JsonArray();
			for (final String topic : agentGuidanceService.getAvailableTopics()) topics.add(topic);
			final JsonObject result = new JsonObject();
			result.add("topics", topics);
			result.addProperty("count", topics.size());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_guide_topics: " + e.getMessage());
		}
	}

	@Tool(value = { "Read the curated Fiji onboarding document before using other Fiji-specific tools; the result contains bounded Markdown content" }, name = "fiji_guide_onboarding")
	public String readOnboarding() {
		return read(OnboardingGuide.ID);
	}

	@Tool(value = { "Search curated Fiji guidance by one exact topic keyword. Use fiji_guide_topics first; results contain document metadata and IDs, not article content" }, name = "fiji_guide_search")
	public String search(@P(name = "topic", value = "Topic keyword from fiji_guide_topics") final String topic) {
		try {
			final JsonArray documents = new JsonArray();
			for (final AgentGuideMetadata document : agentGuidanceService.search(topic)) {
				documents.add(metadataJson(document));
			}

			final JsonObject result = new JsonObject();
			result.addProperty("topic", topic == null ? "" : topic.trim());
			result.add("documents", documents);
			result.addProperty("count", documents.size());

			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_guide_search: " + e.getMessage());
		}
	}

	@Tool(value = { "Read one curated Fiji guidance document by ID. Use fiji_guide_search to find the ID; the result contains metadata and bounded Markdown content" }, name = "fiji_guide_read")
	public String read(@P(name = "id", value = "Document ID from fiji_guide_search") final String id) {
		try {
			final var document = agentGuidanceService.read(id);

			if (document.isEmpty()) return jsonError("No guidance document found for ID: " + id,
				"fiji_guide_search");

			final AgentGuide guide = findGuide(id);
			if (guide == null) return jsonError("No guidance document found for ID: " + id,
				"fiji_guide_search");

			final JsonObject result = metadataJson(guide.metadata());
			result.addProperty("content", document.get());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_guide_read: " + e.getMessage());
		}
	}

	private JsonObject metadataJson(final AgentGuideMetadata document) {
		final JsonObject result = new JsonObject();
		result.addProperty("id", document.id());
		result.addProperty("title", document.title());
		final JsonArray topics = new JsonArray();
		for (final String topic : document.topics()) topics.add(topic);
		result.add("topics", topics);
		result.addProperty("authority", document.authority().name().toLowerCase(Locale.ROOT)
			.replace('_', '-'));
		final JsonArray relatedDocuments = new JsonArray();
		for (final String relatedDocument : document.relatedDocuments()) relatedDocuments.add(
			relatedDocument);
		result.add("related_documents", relatedDocuments);
		return result;
	}

	private AgentGuide findGuide(final String id) {
		if (id == null) return null;
		final String normalizedId = id.trim();
		return agentGuidanceService.getInstances().stream().filter(guide -> guide.metadata().id()
			.equals(normalizedId)).findFirst().orElse(null);
	}
}
