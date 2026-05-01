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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Generalist LLM provider plugin for Ollama with user-selectable models.
 * Allows users to choose from available installed models and download remote
 * models from the Ollama library.
 */
@Plugin(type = LLMProvider.class, name = "Ollama")
public class OllamaProvider extends AbstractOllamaProvider {

	private static final String MODEL_URL =
		"https://ollama.com/search?c=tools&c=thinking";
	private static final String TAG_BASE_URL = "https://ollama.com/library/";

	private Set<String> cachedRemoteTags;

	@Parameter
	private UIService uIService;

	@Parameter
	private StatusService statusService;

	@Override
	public String getName() {
		return "Ollama";
	}

	@Override
	public String getDescription() {
		return "Local Ollama models (user-selectable)";
	}

	@Override
	public List<String> getAvailableModels() {
		// Get actual list of installed models from Ollama
		List<String> models = getAvailableLocalModels();
		// Get basic available remote models
		Set<String> remoteTags = fetchRemoteTags();
		// Mark remote models that aren't installed
		remoteTags.removeAll(models);
		remoteTags.stream().map(this::appendRemoteString).forEach(models::add);

		return models;
	}


	/**
	 * See https://github.com/ollama/ollama/issues/8241
	 */
	private Set<String> fetchRemoteTags() {
		if (cachedRemoteTags != null) return cachedRemoteTags;

		Set<String> remoteTags = new LinkedHashSet<>();
		// Fetch and parse the page
		try {
			Document doc = Jsoup.connect(MODEL_URL).userAgent(
				"Mozilla/5.0 (compatible; Java Jsoup)").get();

			// Step 1: get tool names
			Elements models = doc.select("span[x-test-search-response-title]");
			for (Element model : models) {
				String modelName = model.text();

				// Step 2: fetch tags for this tool
				String tagsUrl = TAG_BASE_URL + modelName;
				Document tagsDoc = Jsoup.connect(tagsUrl).userAgent(
					"Mozilla/5.0 (compatible; Java Jsoup)").get();

				// Select <a> elements that contain the tags. Prefer extracting the
				// canonical tag from the href (e.g. /library/qwen3:8b -> qwen3:8b).
				// Some anchors contain additional UI labels (like a separate
				// "latest" span) so using .text() yields e.g. "qwen3:8b latest"
				// which previously caused us to filter out valid tags. Use the
				// href attribute and the hidden input.command value as fallback.
				Elements tagLinks = tagsDoc.select("a[href^='/library/" + modelName +
					"']");
				for (Element tagLink : tagLinks) {
					String href = tagLink.attr("href"); // e.g. /library/qwen3:8b
					String tag = null;

					if (href != null && href.startsWith("/library/")) {
						tag = href.substring("/library/".length());
						// strip query or trailing slash if any
						int q = tag.indexOf('?');
						if (q != -1) tag = tag.substring(0, q);
						if (tag.endsWith("/")) tag = tag.substring(0, tag.length() - 1);
					}

					// fallback: some desktop rows include an <input class="command"
					// value="qwen3:8b" />
					if ((tag == null || tag.isEmpty())) {
						Element input = tagLink.selectFirst("input.command[value]");
						if (input != null) {
							tag = input.attr("value");
						}
					}

					if (tag == null || tag.isEmpty()) continue;

					String lower = tag.toLowerCase();
					// Skip aliases like `qwen3:latest` (we prefer explicit version tags)
					if (!lower.contains(":") || lower.endsWith(":latest") || lower.equals(
						"latest") || lower.contains("cloud"))
					{
						continue;
					}

					remoteTags.add(tag);
				}
			}
		}
		catch (IOException e) {
			// Remote tags not available
		}
		cachedRemoteTags = remoteTags;
		return remoteTags;
	}
}
