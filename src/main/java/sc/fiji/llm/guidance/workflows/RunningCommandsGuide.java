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
package sc.fiji.llm.guidance.workflows;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;
import sc.fiji.llm.guidance.OnboardingGuide;
import sc.fiji.llm.guidance.application.EnvironmentInspectionGuide;
import sc.fiji.llm.guidance.application.UIInteractionGuide;
import sc.fiji.llm.guidance.application.UpdateSitesGuide;

/** Guidance for running Fiji commands. */
@Plugin(type = AgentGuide.class)
public class RunningCommandsGuide extends AbstractAgentGuide {

	public static final String ID = "running-commands";

	private static final String CONTENT = """
			# Running Commands in Fiji
			Commands are the core mechanism of action in Fiji: how to get things done. They range from
			one-off operations to opening standalone, dedicated UI's for complex tasks.

			Image analysis workflows are typically made up of a series of relevant commands.

			## Active Image

			One of the most important concepts in Fiji usage is the "active image": the most recently selectd
			image window is the default target for almost all image-related commands.

			## Interactive commands

			Indicated by an ellipses, "...", at the end of the plugin name. These commands require
			configuration via parameters. When executed via the menu they will trigger an input dialog
			before running. When run programmatically, this dialog can be skipped by fully specifying
			their parameters. Using the macro recorder (Guide ID: `macro-recorder`) is one way to determine
			these parameters.

			## Agentic use

			In addition to interactive commands, it is very possible that running commands will lead to blocking
			dialogs, whether informative or error related. To understand your options for dealing with these
			dialogs, read Guide ID: `%1$s`.

			When running commands, tools will make an effort to provide you information of what resulting application
			state may have changed. Read Guide ID `%2$s` for an overview of communication channels.

			## Menu overview

			Another major impact you can have is by supporting users in navigating Fiji's menus. They are massive and can
			be intimidating and overwhelming, especially for new users. Users do have a search bar (shortcut: `L`) which
			functions similarly to your `fiji_command_search` tool. However, you are likely faster at sorting and processing
			the sheer quantity of information available.
			
			| Menu | Primary function |
			|---|---|
			| File | Image input and output |
			| Edit | Application settings and basic image editing |
			| Process | Common image processing functions |
			| Analyze | Translating images to knowledge (data) |
			| Plugins | Massive entry point to the hundreds of Fiji plugins |
			| Window | Display management |
			| Help | Environment controls and external resources |

			## Important commands and sub-menus

			| Command | Purpose |
			|---|---|
			| Help > Update... | Fiji update mechanism; enable/disable update sites |
			| Help > Update ImageJ... | ImageJ 1.x update mechanism. DO NOT USE: breaks Fiji compatibility contract |
			| Edit > Options > Memory & Threads... | Configure the memory allocated to Fiji |
			| Edit > Options > Python... | Enable/disable "Python mode", allowing connection to complete Python environments |
			| Edit > Options > ImageJ2... | Enable/disable SCIFIO usage when opening images. Recommended state is disabled |
			| File > Open Samples > |  A collection of sample images for exploring image analysis workflows and concepts |
			| Plugins > Debug > | Tools for troubleshooting Fiji behavior |

			## Extension

			Fiji relies on its community for continued extension, bringing new features and improvements as the field advances.

			The most robust contribution option is using the SciJava plugin framework. However, this presents a high barrier,
			requiring knowledge of Java development practices. Contributions can also be made via macros and scripts. In all
			cases, update sites are the mechanism of distribution (Guide ID: `%3$s`).
			""".formatted(UIInteractionGuide.ID, EnvironmentInspectionGuide.ID,
				UpdateSitesGuide.ID);

	public RunningCommandsGuide() {
		super(ID, "Running Commands", AgentGuide.topics(Topic.IMAGE_ANALYSIS, Topic.COMMANDS,
			Topic.UI), Authority.PROJECT_AUTHORED,
			List.of(UIInteractionGuide.ID, EnvironmentInspectionGuide.ID, UpdateSitesGuide.ID,
				OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
