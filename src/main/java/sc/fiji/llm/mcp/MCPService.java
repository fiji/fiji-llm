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
