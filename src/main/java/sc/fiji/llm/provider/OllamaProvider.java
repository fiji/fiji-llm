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

package sc.fiji.llm.provider;

import java.io.IOException;
import java.util.ArrayList;
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
	protected int getContextSize() {
		return 32 * 1024;
	}

	@Override
	public List<String> getAvailableModels() {
		// Get actual list of installed models from Ollama
		List<String> models = new ArrayList<>(getAvailableLocalModels());
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
