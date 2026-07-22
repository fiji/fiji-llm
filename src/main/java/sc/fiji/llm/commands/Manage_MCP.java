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

package sc.fiji.llm.commands;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import sc.fiji.llm.mcp.MCPService;

/**
 * Manage MCP (Model Context Protocol) server settings. Provides a user
 * interface to configure the MCP server port and view its status.
 */
@Plugin(type = Command.class, description = "Manage MCP Server settings",
	iconPath = "/icons/robot-icon-32.png", menu = { @Menu(label = "Help"), @Menu(
		label = "Assistants"), @Menu(label = "Manage MCP Server...") })
public class Manage_MCP extends DynamicCommand {

	private static final String WIDTH = "300";

	@Parameter
	private MCPService mcpService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private UIService uiService;

	@Parameter(label = "", visibility = ItemVisibility.MESSAGE, persist = false,
		required = false)
	private String welcomeMessage = "";

	@Parameter(label = "Server Status", visibility = ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String serverStatus = "";

	@Parameter(label = "MCP Server URL", visibility = ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String mcpServerUrl = "";

	@Parameter(label = "Port Configuration", description = "Port for MCP server",
		persist = false)
	private int port;

	@Parameter(label = "Launch MCP on Startup",
		description = "Automatically launch MCP server when Fiji starts")
	private boolean launchOnStartup;

	@Parameter(label = "Start Server", persist = false, callback = "startServer")
	private Button help;

	@Override
	public void initialize() {
		StringBuilder welcomeMsg = new StringBuilder();
		welcomeMsg.append("<body style='width: " + WIDTH + "px'>");
		welcomeMsg.append("<h2 style='text-align: center'>MCP Server Management</h2>");
		welcomeMsg.append(
			"<p><b>Model Context Protocol (MCP)</b> exposes Fiji tools to AI assistants.</p>");
		welcomeMsg.append(
			"Configure the server port and check its status here.</p></body>");
		welcomeMessage = welcomeMsg.toString();

		// Load current port from preferences
		port = prefService.getInt(MCPService.class, MCPService.PORT_KEY,
			MCPService.DEFAULT_PORT);

		// Load current launch on startup setting from preferences
		launchOnStartup = prefService.getBoolean(MCPService.class, MCPService.LAUNCH_ON_START_KEY,
			MCPService.DEFAULT_LAUNCH_ON_START);

		// Update server status display
		updateServerStatus();
	}

	/**
	 * Updates the server status message display.
	 */
	private void updateServerStatus() {
		StringBuilder statusMsg = new StringBuilder();
		statusMsg.append("<body style='width: " + WIDTH + "px'>");

		if (mcpService.isServerRunning()) {
			statusMsg.append("<p style='color: green;'><b>✓ Server Running</b></p>");
			statusMsg.append("<p>Port: " + mcpService.getServerPort() + "</p>");
			statusMsg.append("<p>Tools available: " + mcpService.getToolCount() + "</p>");

			// Update MCP Server URL display
			final int serverPort = mcpService.getServerPort();
			StringBuilder urlMsg = new StringBuilder();
			urlMsg.append("<p>http://localhost:" + serverPort + "/mcp</p>");
			mcpServerUrl = urlMsg.toString();
		} else {
			statusMsg.append("<p style='color: orange;'><b>⚠ Server Not Running</b></p>");
			statusMsg.append("<p>Click 'Start Server' to initialize.</p>");

			// Clear MCP Server URL when server is not running
			mcpServerUrl = "<p style='color: gray;'>Server URL will appear here when running.</p>";
		}

		statusMsg.append("</body>");
		serverStatus = statusMsg.toString();

		// Update the UI display
		final MutableModuleItem<String> statusItem = getInfo().getMutableInput(
			"serverStatus", String.class);
		statusItem.setValue(this, serverStatus);

		// Update the MCP Server URL display
		final MutableModuleItem<String> urlItem = getInfo().getMutableInput(
			"mcpServerUrl", String.class);
		urlItem.setValue(this, mcpServerUrl);
	}

	@Override
	public void run() {
		// Check if port has changed
		final int previousPort = prefService.getInt(MCPService.class, MCPService.PORT_KEY,
			MCPService.DEFAULT_PORT);
		if (port != previousPort) {
			prefService.put(MCPService.class, MCPService.PORT_KEY, port);

			// Notify user if server is running
			if (mcpService.isServerRunning()) {
				uiService.showDialog(
					"Port configuration updated to " + port + ".\n\n"
						+ "Please restart Fiji for the new port to take effect.",
					"MCP Server Port Changed");
			}
		}

		// Check if launch on startup setting has changed
		final boolean previousLaunchOnStartup = prefService.getBoolean(MCPService.class,
			MCPService.LAUNCH_ON_START_KEY, MCPService.DEFAULT_LAUNCH_ON_START);
		if (launchOnStartup != previousLaunchOnStartup) {
			prefService.put(MCPService.class, MCPService.LAUNCH_ON_START_KEY,
				launchOnStartup);
		}
	}

	/**
	 * Callback triggered when the Start Server button is clicked.
	 */
	@SuppressWarnings( "unused" )
	private void startServer() {
		if (mcpService.isServerRunning()) {
			uiService.showDialog("MCP Server is already running on port " +
				mcpService.getServerPort());
			return;
		}

		try {
			mcpService.getToolProvider(); // This initializes the server
			updateServerStatus();
			uiService.showDialog("MCP Server started successfully on port " + port);
		} catch (final Exception e) {
			uiService.showDialog("Failed to start MCP Server: " + e.getMessage(),
				"Error");
		}
	}
}
