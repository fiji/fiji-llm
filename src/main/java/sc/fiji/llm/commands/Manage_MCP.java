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

package sc.fiji.llm.commands;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;

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
@Plugin(type = Command.class, description = "Manage Fiji MCP Server settings",
	iconPath = "/icons/robot-icon-32.png", menu = { @Menu(label = "Help"), @Menu(
		label = "Assistants"), @Menu(label = "Manage Fiji MCP Server...") })
public class Manage_MCP extends DynamicCommand {

	private static final String WIDTH = "300";
	private static final String COPY_PLACEHOLDER = "Choose...";
	private static final String COPY_SERVER_URL = "Server URL";
	private static final String COPY_VS_CODE_CONFIG = "VS Code Config";
	private static final String COPY_CLAUDE_CODE_COMMAND = "Claude Code Command";

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

	@Parameter(label = "Available tools", visibility = ItemVisibility.MESSAGE,
		persist = false, required = false)
	private String availableTools = "";

	@Parameter(label = "View tools...", persist = false, callback = "showTools")
	private Button viewToolsButton;

	@Parameter(label = "Port Configuration", description = "Port for MCP server",
		persist = false)
	private int port;

	@Parameter(label = "Copy:", description = "Select an item to copy it to the clipboard",
		choices = { COPY_PLACEHOLDER, COPY_SERVER_URL, COPY_VS_CODE_CONFIG,
			COPY_CLAUDE_CODE_COMMAND }, persist = false,
		callback = "copySelectionChanged")
	private String copySelection = COPY_PLACEHOLDER;

	@Parameter(label = "Launch MCP on Startup",
		description = "Automatically launch MCP server when Fiji starts")
	private boolean launchOnStartup;

	@Parameter(label = "Start Server", persist = false, callback = "startServer")
	private Button startServerButton;

	@Override
	public void initialize() {
		StringBuilder welcomeMsg = new StringBuilder();
		welcomeMsg.append("<body style='width: " + WIDTH + "px'>");
		welcomeMsg.append("<h2 style='text-align: center'>Fiji MCP Server</h2>");
		welcomeMsg.append(
			"<p>The <b>Model Context Protocol (MCP)</b> server exposes Fiji tools to AI assistants.</p>");
		welcomeMsg.append(
			"<p>Configure the server port and check its status here.</p>");
		welcomeMsg.append(
			"<p>To use Fiji MCP from your assistant software, such as Claude Desktop or VS Code, it's often easiest to ask the built-in agent to help connect and follow its instructions. If it asks for connection details, use the <code>Copy:</code> selector below.</p>");
		welcomeMsg.append("</body>");
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
			availableTools = "<p>" + toolCountText(mcpService.getToolCount()) + "</p>";

			// Update MCP Server URL display
			final int serverPort = mcpService.getServerPort();
			StringBuilder urlMsg = new StringBuilder();
			urlMsg.append("<p>http://localhost:" + serverPort + "/mcp</p>");
			mcpServerUrl = urlMsg.toString();
		} else {
			statusMsg.append("<p style='color: orange;'><b>⚠ Server Not Running</b></p>");
			statusMsg.append("<p>Click 'Start Server' to initialize.</p>");
			availableTools = "<p style='color: gray;'>Unavailable until the server is running.</p>";

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

		// Update the available tools display
		final MutableModuleItem<String> toolsItem = getInfo().getMutableInput(
			"availableTools", String.class);
		toolsItem.setValue(this, availableTools);
	}

	private String toolCountText(final int count) {
		return count + (count == 1 ? " tool" : " tools");
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
			mcpService.startServer();
			updateServerStatus();
			uiService.showDialog("MCP Server started successfully on port " + port);
		} catch (final Exception e) {
			uiService.showDialog("Failed to start MCP Server: " + e.getMessage(),
				"Error");
		}
	}

	private void copyToClipboard(final String value, final String description) {
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				new StringSelection(value), null);
			uiService.showDialog(description + " copied to the clipboard.");
		} catch (final RuntimeException e) {
			uiService.showDialog("Could not copy " + description.toLowerCase() + ": " +
				e.getMessage(), "Clipboard Error");
		}
	}

	private String getRunningServerUrl() {
		if (!mcpService.isServerRunning()) {
			uiService.showDialog("Start the MCP server before copying its connection details.",
				"MCP Server Not Running");
			return null;
		}
		return "http://localhost:" + mcpService.getServerPort() + "/mcp";
	}

	@SuppressWarnings("unused")
	private void copySelectionChanged() {
		if (COPY_PLACEHOLDER.equals(copySelection)) return;

		try {
			final String url = getRunningServerUrl();
			if (url == null) return;

			switch (copySelection) {
				case COPY_SERVER_URL:
					copyToClipboard(url, "MCP server URL");
					break;
				case COPY_VS_CODE_CONFIG:
					copyToClipboard("""
						{
						  "servers": {
						    "fiji": {
						      "type": "http",
						      "url": "%s"
						    }
						  }
						}
						""".formatted(url), "VS Code MCP configuration");
					break;
				case COPY_CLAUDE_CODE_COMMAND:
					copyToClipboard(
						"claude mcp add --transport http fiji --scope user " + url,
						"Claude Code command");
					break;
				default:
					break;
			}
		}
		finally {
			copySelection = COPY_PLACEHOLDER;
			getInfo().getMutableInput("copySelection", String.class).setValue(this,
				copySelection);
		}
	}

	@SuppressWarnings("unused")
	private void showTools() {
		if (!mcpService.isServerRunning()) {
			uiService.showDialog("Start the MCP server before viewing its tools.",
				"MCP Server Not Running");
			return;
		}

		final List<String> toolNames = mcpService.getToolNames();
		if (toolNames.isEmpty()) {
			uiService.showDialog("No MCP tools are currently exposed.",
				"Fiji MCP Tools");
			return;
		}

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode(
			"Fiji MCP tools");
		final Map<String, List<String>> toolsByCategory = new TreeMap<>();
		for (final String toolName : toolNames) {
			final String category = categoryFor(toolName);
			toolsByCategory.computeIfAbsent(category, key -> new ArrayList<>())
				.add(toolName);
		}

		for (final Map.Entry<String, List<String>> entry : toolsByCategory.entrySet()) {
			final List<String> categoryTools = entry.getValue();
			categoryTools.sort(String::compareTo);
			final DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(
				entry.getKey() + " (" + categoryTools.size() + ")");
			for (final String toolName : categoryTools) {
				categoryNode.add(new DefaultMutableTreeNode(toolName));
			}
			root.add(categoryNode);
		}

		final JTree toolsTree = new JTree(root);
		toolsTree.setRootVisible(false);
		toolsTree.setShowsRootHandles(true);
		final JScrollPane scrollPane = new JScrollPane(toolsTree);
		scrollPane.setPreferredSize(new Dimension(420, 360));

		final Window owner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getActiveWindow();
		final JDialog dialog = new JDialog(owner, "Fiji MCP Tools",
			Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.add(scrollPane);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
	}

	private String categoryFor(final String toolName) {
		final String prefix = "fiji_";
		if (!toolName.startsWith(prefix)) return "Other";
		final int separator = toolName.indexOf('_', prefix.length());
		return separator > prefix.length() ? toolName.substring(prefix.length(),
			separator) : "Other";
	}
}
