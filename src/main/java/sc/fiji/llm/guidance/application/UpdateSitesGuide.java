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

/** Guidance for Fiji update sites. */
@Plugin(type = AgentGuide.class)
public class UpdateSitesGuide extends AbstractAgentGuide {

	public static final String ID = "update-sites";

	private static final String CONTENT = """
			# Fiji Update Sites

			An update site is web space used by the ImageJ Updater to distribute Fiji and
			ImageJ extensions such as plugins, scripts, and macros. The updater downloads
			and installs matching files for the site so users do not need to install each
			extension manually, and handles checks for updates over time.

			The core ImageJ and Fiji projects use update sites to publish key platform
			components and curated plugins, while other research groups and developers can
			publish their own extensions through a community or self-hosted site. A site
			may provide specialized functionality that is not part of the Fiji distribution,
			and an extension may depend on a particular site being enabled.

			Sites are not all equivalent in scope, maintenance, stability, or compatibility.
			Treat a community site as a source of extensions whose documentation and
			maintainer determine how it should be used; do not assume that every available
			site is required or appropriate for every Fiji installation.

			The following update sites should never be manually enabled or disabled:
			- Fiji-latest: https://sites.imagej.net/Fiji/
			- Java-8: https://sites.imagej.net/Java-8/
			- ImageJ: https://update.imagej.net/
			- Fiji: https://update.fiji.sc/

			## Available versus active

			- An **available** site is automatically known to the updater and may or may not
			  be enabled.
			- An **active** site is enabled for the current Fiji installation and contributes
			  files when the updater checks for updates.

			## Inspecting update-site state

			Use the read-only `fiji_system_list_update_sites` tool if you need to determine
			whether a dependency site is enabled; do not infer the current state from general
			Fiji documentation or from a conversation.

			Update sites should not be modified by an agent. Do not recommend changing site
			configuration merely because an optional site is inactive. First establish that the
			requested extension actually depends on it and follow the site's or extension's
			wiki documentation.

			## Additional resources

			- Official overview: https://imagej.net/update-sites/
			- Available sites: https://imagej.net/list-of-update-sites
			- Following a site: https://imagej.net/update-sites/following
			""".strip();

	public UpdateSitesGuide() {
		super(ID, "Update Sites", AgentGuide.topics(Topic.APPLICATION, Topic.UPDATE_SITES,
			Topic.PLUGINS), Authority.PROJECT_AUTHORED, List.of(
				OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
