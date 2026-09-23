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
package sc.fiji.llm.guidance.application;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;
import sc.fiji.llm.guidance.OnboardingGuide;

/** Guidance for Fiji support and community resources. */
@Plugin(type = AgentGuide.class)
public class SupportAndCommunityGuide extends AbstractAgentGuide {

	public static final String ID = "support-and-community";

	private static final String CONTENT = """
			# Fiji Support and Community

			Use this guide when a user needs help, official documentation, or a place to
			report a reproducible problem. For the general agent workflow and available Fiji
			tools, first read the guide with Guide ID `%s`.

			## Choose the right channel

			- **Ask for help or discuss a workflow:** Use the Image.sc forum at
			  https://forum.image.sc/. It is the main community discussion space for Fiji,
			  ImageJ, and bioimage analysis. Search existing discussions first, then include
			  the Fiji/ImageJ version, operating system, update sites or extensions involved,
			  a minimal example, and the exact error or unexpected result.
			- **Learn how Fiji works:** Use the official documentation at https://imagej.net/.
			  It covers Fiji, ImageJ, SciJava, plugins, scripting, update sites, and related
			  concepts. Treat official documentation as the primary reference for documented
			  behavior, and label forum advice as community experience.
			- **Report a reproducible software problem:** Use the issue tracker for the
			  project that owns the failing component. Relevant core projects include:
			  - ImageJ: https://github.com/imagej/imagej2/issues
			  - Fiji: https://github.com/fiji/fiji/issues
			  - SciJava: https://github.com/scijava/scijava-common/issues
			  - ImgLib2: https://github.com/imglib/imglib2/issues
			  If ownership is unclear, ask on the Image.sc forum first rather than filing
			  the same report in several repositories.

			## Make a report useful

			Before asking for help or filing an issue, inspect the current environment and
			preserve the smallest reproducible example. Record the relevant versions, exact
			steps, input or sample data when shareable, expected behavior, actual behavior,
			and useful logs or stack traces. Distinguish a Fiji packaging problem from an
			ImageJ, SciJava, ImgLib2, plugin, script, or local-environment problem when
			possible.

			Do not include API keys, passwords, private images, or other sensitive data in a
			forum post or issue. Replace sensitive inputs with a minimal public example and
			keep the report focused on one problem. A request for help is not automatically a
			software bug, and a bug report should not assume that a community workaround is
			an official fix.

			These resources are complementary: use imagej.net to learn, Image.sc to discuss
			and troubleshoot, and GitHub issues to track actionable defects or feature
			requests in the appropriate project.
			""".formatted(OnboardingGuide.ID).trim();

	public SupportAndCommunityGuide() {
		super(ID, "Fiji Support and Community", AgentGuide.topics(Topic.APPLICATION,
			Topic.SUPPORT, Topic.COMMUNITY, Topic.DOCUMENTATION, Topic.ISSUES),
			Authority.PROJECT_AUTHORED, List.of(OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
