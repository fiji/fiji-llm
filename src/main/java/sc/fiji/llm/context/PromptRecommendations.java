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
package sc.fiji.llm.context;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import sc.fiji.llm.tools.ToolRecommendationUtils;

/** Formats context-specific tool and guide hints for the model. */
public final class PromptRecommendations {

	private static final String HEADER =
		"=== RECOMMENDED FIJI RESOURCES (relevant to attached context) ===";
	private static final String FOOTER = "=== END RECOMMENDED FIJI RESOURCES ===";

	private PromptRecommendations() {
		// utility class
	}

	/**
	 * Formats relevant resources for the supplied user-attached context.
	 *
	 * @return a prompt block, or an empty string when no supported context is
	 *         attached
	 */
	public static String format(final List<? extends ContextItem> contextItems) {
		boolean hasScript = false;
		boolean hasImage = false;
		if (contextItems != null) {
			for (final ContextItem item : contextItems) {
				if (item == null || item.getType() == null) continue;
				if ("script".equalsIgnoreCase(item.getType())) hasScript = true;
				if ("image".equalsIgnoreCase(item.getType())) hasImage = true;
			}
		}
		if (!hasScript && !hasImage) return "";

		final JsonObject recommendations = new JsonObject();
		final JsonArray toolFamilies = new JsonArray();
		if (hasScript) {
			toolFamilies.add("fiji_script_*");
		}
		if (hasImage) {
			toolFamilies.add("fiji_image_*");
		}
		if (hasScript && hasImage) ToolRecommendationUtils.addGuideRecommendations(
			recommendations, "scripting", "image-types");
		else if (hasScript) ToolRecommendationUtils.addGuideRecommendations(
			recommendations, "scripting");
		else ToolRecommendationUtils.addGuideRecommendations(recommendations,
			"image-types");
		recommendations.add("tool_families", toolFamilies);
		return HEADER + "\n" + recommendations + "\n" + FOOTER;
	}
}
