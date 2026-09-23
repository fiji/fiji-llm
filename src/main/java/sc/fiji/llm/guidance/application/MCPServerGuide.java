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

/** Information about the integrated MCP server. */
@Plugin(type = AgentGuide.class)
public class MCPServerGuide extends AbstractAgentGuide {

	public static final String ID = "mcp-server";

	private static final String CONTENT = """
			# Fiji MCP Server

			The Model Context Protocol (MCP) server is the standard connection point between
			Fiji and external LLM applications. It exposes Fiji's discovered AI tools through
			one protocol, so the same Fiji capabilities can be used by the integrated chat,
			VS Code, Claude Desktop, or another MCP-compatible client.

			The integrated chat and the MCP server are separate interfaces over the same
			extensible Fiji tool framework. They share tool implementations and the live Fiji
			application state, but they do not share conversations, chat memory, or window
			controls. Starting or configuring the MCP server does not configure the integrated
			chat, and an external MCP conversation does not appear in the chat window.

			## Why Fiji uses MCP

			- **One integration surface:** Client applications do not need separate Fiji
			  plugins or custom APIs for every assistant. They connect to MCP and discover
			  the tools that the running Fiji instance provides.
			- **Live application context:** Tool calls execute in the running Fiji process,
			  where they can inspect the current images, displays, scripts, logs, tables, and
			  other application state rather than working from a detached copy.
			- **Agentic automation:** An MCP client can combine state inspection, reasoning,
			  script generation, command execution, and verification into a workflow. This
			  extends chat beyond answering questions while keeping the actions in Fiji's
			  existing tool and service architecture.
			- **Extensibility:** Fiji tools are discovered through the shared AI tool
			  registry. Adding a suitable tool makes it available to MCP clients without
			  creating a parallel client-specific integration.
			- **Structured and multimodal results:** Tools can return structured text and,
			  where supported, image content such as rendered Fiji images and overlays.

			## How it works

			The MCP service starts a local loopback HTTP server for the running Fiji
			application. The default endpoint is `http://localhost:9090/mcp`. The endpoint
			is tied to that Fiji process: it is unavailable when Fiji is closed, and its
				tool list and results describe the current installation and session.

			An external MCP client connects to the endpoint, discovers the available tools,
			and invokes them on the user's behalf. The server currently exposes tools for
			images, scripts and macros, commands, UI inspection, logs, system information,
			ROI and Results Table inspection, and curated Fiji guidance. The exact set is
			dynamic, so clients should discover tools from the active server rather than
			assume that every Fiji installation exposes the same list.

			For an unfamiliar Fiji-specific workflow, start with the read-only guidance tool
			`fiji_guidance_read` and Guide ID `%s`, then retrieve only the additional
			guidance needed for the task.

			## Managing the server

			Use **Help > Assistants > Manage MCP Server...** to see whether the server is
			running, its URL, and the number of exposed tools. The dialog can start the
			server, configure its port, and enable launch on Fiji startup. The default port
			is `9090`; changing the port of a running server requires restarting Fiji before
			clients can use the new endpoint.

			An MCP client must be configured with the endpoint shown by Fiji. Start Fiji and
			the MCP server before connecting the client. If Fiji or the server restarts, the
			client may need to reconnect and refresh its cached tool definitions.

			## Safety and state

			MCP is a communication protocol, not a guarantee that every tool is read-only.
			Some exposed tools can modify images, scripts, commands, dialogs, or other
			workspace state. Inspect the current state first, choose the narrowest applicable
			tool, and verify the result afterward. Re-query state when accuracy matters
			because a human or another client may change Fiji between tool calls.
			""".formatted(OnboardingGuide.ID);

	public MCPServerGuide() {
		super(ID, "MCP Server", List.of("application", "ai", "llm", "agent", "mcp"),
			Authority.PROJECT_AUTHORED, List.of(OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
