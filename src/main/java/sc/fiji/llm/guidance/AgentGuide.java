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

import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.SingletonPlugin;

/** A singleton plugin containing one curated guide for an AI agent. */
public interface AgentGuide extends SingletonPlugin {

	/** Canonical topic keywords used by the built-in guidance catalog. */
	enum Topic {
		AGENT("agent"),
		AI("ai"),
		APPLICATION("application"),
		COMMANDS("commands"),
		COMMUNITY("community"),
		CONTEXT("context"),
		CONTRIBUTION("contribution"),
		DATA("data"),
		DATASET("dataset"),
		DATA_TYPES("data-types"),
		DEVELOPMENT("development"),
		DIALOGS("dialogs"),
		DOCUMENTATION("documentation"),
		ENVIRONMENT("environment"),
		ERRORS("errors"),
		EXTENSIONS("extensions"),
		FIJI("fiji"),
		GROOVY("groovy"),
		IMAGE_ANALYSIS("image-analysis"),
		IMAGEPLUS("imageplus"),
		IMAGES("images"),
		IMGPLUS("imgplus"),
		INPUT("input"),
		ISSUES("issues"),
		LLM("llm"),
		LOGS("logs"),
		MACROS("macros"),
		MCP("mcp"),
		ONBOARDING("onboarding"),
		PLUGINS("plugins"),
		PRINT_STREAM("print-stream"),
		PYTHON("python"),
		RESULTS_TABLE("results-table"),
		ROIS("rois"),
		SCRIPTS("scripts"),
		SERVICES("services"),
		SUPPORT("support"),
		TOOLS("tools"),
		UI("ui"),
		UPDATE_SITES("update-sites"),
		WORKFLOWS("workflows");

		private final String keyword;

		Topic(final String keyword) {
			this.keyword = keyword;
		}

		/**
		 * @return the canonical lower-case keyword used in metadata and searches
		 */
		public String keyword() {
			return keyword;
		}
	}

	/**
	 * Converts canonical topic values to the string form used by guide metadata.
	 *
	 * @param topics canonical topic values
	 * @return topic keywords in the supplied order
	 */
	static List<String> topics(final Topic... topics) {
		return Arrays.stream(topics).map(Topic::keyword).toList();
	}

	/**
	 * @return the stable metadata for this guide
	 */
	AgentGuideMetadata metadata();

	/**
	 * @return the guide content, normally Markdown
	 */
	String content();
}
