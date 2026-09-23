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
package sc.fiji.llm.guidance.data;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;

/** Guidance for Fiji services and contexts. */
@Plugin(type = AgentGuide.class)
public class ServicesAndContextGuide extends AbstractAgentGuide {

	public static final String ID = "services-and-context";

	private static final String CONTENT = """
			# SciJava Services and Context

			The `org.scijava.Context` is a platform for Inversion of Control extensibility.
			It discovers and initializes all service plugins discoverable on a classpath, injects
			services into `@Parameter` fields and constructors, and makes them available
			throughout Fiji. This plugin model backs Fiji plugin discovery: classes 
			annotated with `@Plugin` are indexed and instantiated when needed.

			Fiji itself is organized around the lifecycle of its application `Context`.
			An agent running inside Fiji should use the existing context and its injected
			services rather than create another context. A second context creates a separate
			service graph and can lead to duplicate services, disconnected application state,
			or resources that are not managed by Fiji's lifecycle. Separate contexts are
			appropriate only for deliberately isolated tests or standalone applications.

			Most services operate implicitly, so do not recreate or manually wire them.
			Presume that initialized services provide Fiji's normal integration: for example,
			a script that creates or opens an image will normally be handled by the dataset,
			display, and UI services and shown when a UI is available. Use the service
			interfaces explicitly when you need to inspect or control that state, request a
			specific update, or diagnose behavior that did not meet expectations. UI behavior
			is context-dependent and may be unavailable during headless execution.

			## Core SciJava services (`scijava-common`)

			- `PluginService`: discovers plugin implementations.
			- `CommandService` and `ModuleService`: find, configure, and run commands.
			- `EventService`: publishes and subscribes to application events.
			- `LogService` and `StatusService`: report diagnostics and progress.
			- `PrefService`, `ThreadService`, `IOService`, and `ConvertService`: provide
			  preferences, background execution, input/output, and type conversion.
			- `UIService`, `DisplayService`, `ObjectService`, and `PlatformService`: expose
			  user-interface, display, object-tracking, and platform integration.

			## ImageJ services (`imagej-common`)

			- `DatasetService`: creates and manages ImageJ datasets.
			- `ImageDisplayService`: coordinates image displays and their datasets.
			- `ROIService`, `OverlayService`, `LUTService`, `TableService`, and
			  `UnitService`: support common image-analysis data and operations.
			- `ImageJUIService`: provides ImageJ-specific user-interface integration.

			## Legacy integration (`imagej-legacy`)

			- `LegacyService` bridges ImageJ 1.x objects and ImageJ2 displays. It maintains
			  the legacy image map and provides converters among `ImagePlus`, `ImgPlus`, and
			  `Dataset` when the legacy integration is active.
			- These converters enable convenient interoperability: a script or command can
			  often declare an `ImagePlus`, `ImgPlus`, or `Dataset` parameter according to
			  the API it wants to use, and SciJava will perform the conversion.
			- Conversion is not proof that the objects are identical. The bridge may share
			  backing data, wrap it, or copy it, depending on the source and target. Live
			  synchronization between ImageJ 1.x and ImageJ2 is configurable and may be
			  disabled for performance, so verify state after cross-model mutations.
			- Use the existing `LegacyService` from Fiji's application context. Do not create
			  another legacy service or another ImageJ 1.x application instance.

			## Fiji-LLM services

			These services are also available through the same SciJava context:

			- `AgentGuidanceService`: discovers and reads curated guidance for agents.
			- `ProviderService`: discovers the configured LLM provider plugins.
			- `AssistantService`: creates assistants with a selected provider, model,
			  memory, and tools.
			- `AiToolService`: discovers and filters the tools available to an agent.
			- `ContextItemService`: discovers suppliers for user-selected Fiji context.
			- `ImageRenderingService`: renders an ImageJ display as bounded image content
			  for multimodal models.
			- `ConversationService`: manages saved conversations and their history.
			- `MCPService`: exposes discovered tools through a local MCP server for external
			  clients.
			""";

	public ServicesAndContextGuide() {
		super(ID, "Services and Context", AgentGuide.topics(Topic.DATA, Topic.SERVICES,
			Topic.CONTEXT), Authority.PROJECT_AUTHORED,
			List.of(DataTypesGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
