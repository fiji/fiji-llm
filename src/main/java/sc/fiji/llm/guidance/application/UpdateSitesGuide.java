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
			and installs matching files for the site, so users do not need to install each
			extension manually.

			## How the community uses them

			Update sites are a community distribution mechanism. The core ImageJ and Fiji
			projects publish important platform and distribution components, while other
			research groups and developers can publish their own extensions through a
			hosted or self-hosted site. A site may provide specialized functionality that
			is not part of the Fiji distribution, and an extension may depend on a
			particular site being enabled.

			Sites are not all equivalent in scope, maintenance, stability, or compatibility.
			Treat a community site as a source of extensions whose documentation and
			maintainer determine how it should be used; do not assume that every available
			site is required or appropriate for every Fiji installation.

			## Available versus active

			- An **available** site is known to the updater and can potentially be enabled.
			- An **active** site is enabled for the current Fiji installation and contributes
			  files when the updater checks for updates.
			- A site being available does not mean that its extensions are installed.
			- A site being active does not by itself prove that a particular plugin or script
			  is installed; inspect the installed command or extension separately.

			Fiji normally uses its core update sites, and they should not be disabled as a
			general troubleshooting step. If a workflow needs a different Fiji release
			channel, use the appropriate Fiji distribution or documented release mechanism
			instead of disabling core sites casually.

			## Inspecting update-site state

			Use the read-only `fiji_system_list_update_sites` tool to inspect the current
			installation. It reports each known site's name, URL, and active status. Use
			this live result when deciding whether a dependency site is enabled; do not
			infer the current state from general Fiji documentation or from a previous
			conversation.

			This guidance and the listing tool do not enable, disable, add, remove, or
			modify update sites. Do not recommend changing site configuration merely because
			an optional site is inactive. First establish that the requested extension
			actually depends on it and follow the site's or extension's documentation.

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
