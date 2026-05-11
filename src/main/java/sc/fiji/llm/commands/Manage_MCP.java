/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
		} else {
			statusMsg.append("<p style='color: orange;'><b>⚠ Server Not Running</b></p>");
			statusMsg.append("<p>Click 'Start Server' to initialize.</p>");
		}

		statusMsg.append("</body>");
		serverStatus = statusMsg.toString();

		// Update the UI display
		final MutableModuleItem<String> statusItem = getInfo().getMutableInput(
			"serverStatus", String.class);
		statusItem.setValue(this, serverStatus);
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
