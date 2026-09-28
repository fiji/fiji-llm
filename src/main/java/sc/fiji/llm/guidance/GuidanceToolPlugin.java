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

	@Tool(value = { "List IDs, titles, summaries, topics, and authority for all available guides" }, name = "fiji_guide_list")
	public String list() {
		try {
			final JsonArray guides = new JsonArray();
			for (final AgentGuideMetadata guide : agentGuidanceService.listDocuments()) {
				guides.add(metadataJson(guide));
			}
			final JsonObject result = new JsonObject();
			result.add("guides", guides);
			result.addProperty("count", guides.size());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_guide_list: " + e.getMessage());
		}
	}

	@Tool(value = { "Return the contents of a guide." }, name = "fiji_guide_read")
	public String read(@P(name = "guide_id", value = "Guide ID from fiji_guide_list") final String guideId) {
		try {
			final var document = agentGuidanceService.read(guideId);

			final AgentGuideMetadata metadata = agentGuidanceService.listDocuments().stream()
				.filter(candidate -> candidate.id().equals(guideId == null ? "" : guideId.trim()))
				.findFirst().orElse(null);
			if (document.isEmpty() || metadata == null) return jsonError(
				"No guide found for ID: " + guideId, ErrorOptions.withTool("fiji_guide_list"));

			final JsonObject result = metadataJson(metadata);
			result.addProperty("content", document.get());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_guide_read: " + e.getMessage(),
				ErrorOptions.withTool("fiji_guide_list"));
		}
	}

	private JsonObject metadataJson(final AgentGuideMetadata document) {
		final JsonObject result = new JsonObject();
		result.addProperty("id", document.id());
		result.addProperty("title", document.title());
		result.addProperty("summary", document.summary());
		final JsonArray topics = new JsonArray();
		for (final String topic : document.topics()) topics.add(topic);
		result.add("topics", topics);
		result.addProperty("authority", document.authority().name().toLowerCase(Locale.ROOT)
			.replace('_', '-'));
		return result;
	}
}
