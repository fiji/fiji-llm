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

package sc.fiji.llm.mcp;

import dev.langchain4j.service.tool.ToolProvider;
import net.imagej.ImageJService;

/**
 * Service for managing MCP (Model Context Protocol) server integration.
 * <p>
 * This service exposes tools discovered by AiToolService through an MCP server
 * running on localhost, making them available to LangChain4j AiServices via a
 * ToolProvider interface.
 * </p>
 */
public interface MCPService extends ImageJService {

	/**
	 * Preference key for the MCP server port configuration.
	 */
	String PORT_KEY = "sc.fiji.mcp.port";

	/**
	 * Default port for the MCP server.
	 */
	int DEFAULT_PORT = 9090;

	/**
	 * Preference key for the MCP launch on startup setting.
	 */
	String LAUNCH_ON_START_KEY = "sc.fiji.mcp.launchOnStartup";

	/**
	 * Default value for launch on startup (false).
	 */
	boolean DEFAULT_LAUNCH_ON_START = false;

	/**
	 * Gets the ToolProvider wrapping the MCP server connection.
	 * <p>
	 * This method lazy-initializes the MCP server on first call and returns a
	 * ToolProvider that integrates with LangChain4j's AiServices.
	 * </p>
	 *
	 * @return a ToolProvider connected to the MCP server
	 */
	ToolProvider getToolProvider();

	/**
	 * Checks if the MCP server is currently running.
	 *
	 * @return true if the server is running, false otherwise
	 */
	boolean isServerRunning();

	/**
	 * Gets the port on which the MCP server is running.
	 *
	 * @return the server port (default 9090)
	 */
	int getServerPort();

	/**
	 * Gets the number of tools currently available in the MCP server.
	 *
	 * @return the tool count (0 if server is not running)
	 */
	int getToolCount();
}
