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

			Use this guide when a user needs help, documentation, or a place to report a
			reproducible problem. For the general agent workflow and available Fiji tools,
			first read the guide with Guide ID `%s`.

			## Choose a channel

			- **Learn:** Use the official documentation at https://imagej.net/ for Fiji,
			  ImageJ, SciJava, plugins, scripting, and update sites.
			- **Discuss or troubleshoot:** Use the Image.sc forum at https://forum.image.sc/.
			  Search existing discussions first and include the relevant version, operating
			  system, extensions, minimal example, and exact error or unexpected result.
			- **Report a defect:** Use the issue tracker for the project that owns the failing
			  component: ImageJ at https://github.com/imagej/imagej2/issues, Fiji at
			  https://github.com/fiji/fiji/issues, SciJava at
			  https://github.com/scijava/scijava-common/issues, or ImgLib2 at
			  https://github.com/imglib/imglib2/issues. If ownership is unclear, ask on the
			  forum first.

			Provide current environment details, exact steps, expected and actual behavior,
			and useful logs or stack traces. Preserve the smallest shareable reproduction,
			and never include API keys, passwords, private images, or other sensitive data.
			Treat official documentation, community advice, and issue reports as different
			kinds of evidence.
			""".formatted(OnboardingGuide.ID).strip();

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
