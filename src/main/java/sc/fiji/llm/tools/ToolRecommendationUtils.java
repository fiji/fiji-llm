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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.llm.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Adds structured tool and guide recommendations to JSON responses. */
public final class ToolRecommendationUtils {

	private ToolRecommendationUtils() {
		// utility class
	}

	public static void addGuideRecommendations(final JsonObject result,
		final String... guideIds)
	{
		if (guideIds == null || guideIds.length == 0) return;
		final JsonArray recommendations = new JsonArray();
		for (final String guideId : guideIds) {
			if (guideId == null || guideId.trim().isEmpty()) continue;
			final JsonObject recommendation = new JsonObject();
			recommendation.addProperty("tool", "fiji_guide_read");
			final JsonObject arguments = new JsonObject();
			arguments.addProperty("guide_id", guideId.trim());
			recommendation.add("arguments", arguments);
			recommendations.add(recommendation);
		}
		if (recommendations.size() > 0) result.add("guide_recommendations",
			recommendations);
	}

	public static void addToolRecommendations(final JsonObject result,
		final String... toolNames)
	{
		if (toolNames == null || toolNames.length == 0) return;
		final JsonArray recommendations = new JsonArray();
		for (final String toolName : toolNames) {
			if (toolName != null && !toolName.trim().isEmpty()) recommendations.add(
				toolName.trim());
		}
		if (recommendations.size() > 0) result.add("recommended_tools",
			recommendations);
	}
}
