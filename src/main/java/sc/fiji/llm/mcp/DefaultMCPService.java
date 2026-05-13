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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonReferenceSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import sc.fiji.llm.tools.AiToolService;

/**
 * Default implementation of MCPService.
 * <p>
 * Manages an MCP server running on localhost that exposes tools discovered by
 * AiToolService. Provides a ToolProvider wrapping the MCP client connection
 * for use with LangChain4j AiServices.
 * </p>
 */
@Plugin(type = Service.class)
public class DefaultMCPService extends AbstractService implements MCPService
{

	private static final int STARTUP_WAIT = 3;
	private static final int SHUTDOWN_WAIT = 3000;
	private static final String FIJI_MCP_VERSION = "0.1.0";

	@Parameter
	private LogService logService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private AiToolService aiToolService;

	private ToolProvider toolProvider;
	private Thread serverThread;
	private int toolCount = 0;
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final AtomicBoolean disposed = new AtomicBoolean(false);
	private final AtomicBoolean serverError = new AtomicBoolean(false);
	private final AtomicBoolean stopServer = new AtomicBoolean(false);
	private CountDownLatch serverReady;

	@Override
	public synchronized ToolProvider getToolProvider() {
		if (disposed.get()) {
			throw new IllegalStateException(
				"MCPService has been disposed and cannot be reused");
		}

		if (!initialized.get()) {
			initializeServer();
		}

		return toolProvider;
	}

	@Override
	public boolean isServerRunning() {
		return initialized.get() && toolProvider != null;
	}

	@Override
	public int getServerPort() {
		return prefService.getInt(MCPService.class, MCPService.PORT_KEY,
			MCPService.DEFAULT_PORT);
	}

	/**
	 * Gets whether the MCP server should launch on startup.
	 *
	 * @return true if server should launch automatically, false otherwise
	 */
	private boolean launchOnStartup() {
		return prefService.getBoolean(MCPService.class, MCPService.LAUNCH_ON_START_KEY,
			MCPService.DEFAULT_LAUNCH_ON_START);
	}

	/**
	 * Gets the number of tools currently available in the MCP server.
	 *
	 * @return the tool count (0 if server is not running)
	 */
	@Override
	public int getToolCount() {
		return toolCount;
	}

	@Override
	public void initialize() {
		if (!initialized.get() && launchOnStartup()) {
			initializeServer();
		}
	}

	@Override
	public void dispose() {
		if (!disposed.compareAndSet(false, true)) {
			return; // Already disposed
		}

		try {
			if (serverThread != null && serverThread.isAlive()) {
				logService.info("Shutting down MCP server thread");
				stopServer.set(true);
				serverThread.join(SHUTDOWN_WAIT); // Wait up to 3 seconds
				if (serverThread.isAlive()) {
					logService.warn("MCP server thread did not shut down gracefully");
				}
			}

			toolProvider = null;
			logService.info("MCPService disposed successfully");
		} catch (final InterruptedException e) {
			logService.error("Error while disposing MCPService", e);
			Thread.currentThread().interrupt();
		}
	}

	private synchronized void initializeServer() {
		if (initialized.get() || disposed.get()) {
			return;
		}

		try {
			final int port = getServerPort();
			logService.info("Initializing MCP server on localhost:" + port);

			// Initialize the server ready signal
			serverReady = new CountDownLatch(1);
			serverError.set(false);
			stopServer.set(false);

			// Start MCP server in a daemon thread
			serverThread = new Thread(() -> {
				try {
					runMcpServer(aiToolService.getToolsWithExecutors(), port);
				} catch (final Exception e) {
					logService.error("MCP server error", e);
					serverError.set(true);
					serverReady.countDown(); // Signal failure to unblock waiting thread
				}
			});
			serverThread.setDaemon(true);
			serverThread.setName("MCP-Server-Thread");
			serverThread.start();

			// Wait for server to be ready before creating client (with timeout)
			logService.debug("Waiting for Fiji MCP server to be ready...");
			final boolean serverStarted = serverReady.await(STARTUP_WAIT, TimeUnit.SECONDS);

			if (!serverStarted) {
				throw new RuntimeException(
					"Fiji MCP server failed to start");
			}

			if (serverError.get()) {
				throw new RuntimeException("Fiji MCP server encountered an error during startup");
			}

			logService.debug("Fiji MCP server is ready, creating client...");

			// Now that server is running, create the client and tool provider
			createClientAndProvider(port);

			initialized.set(true);
			logService.info("Fiji MCP server initialized successfully");
		} catch (final Exception e) {
			logService.error("Failed to initialize Fiji MCP server", e);
			throw new RuntimeException("Failed to initialize Fiji MCP server", e);
		}
	}

	private void createClientAndProvider(final int port) {
		if (toolProvider != null) {
			return; // Already created
		}

		logService.debug("Creating Fiji MCP client and tool provider");
		final McpTransport transport = StreamableHttpMcpTransport.builder()
			.url("http://localhost:" + port + "/mcp")
			.logRequests(true) // if you want to see the traffic in the log
			.logResponses(true)
			.build();
		final McpClient mcpClient = DefaultMcpClient.builder()
			.key("FijiMCPClient")
			.transport(transport)
			.build();

		toolProvider = McpToolProvider.builder()
			.mcpClients(mcpClient)
			.build();
		logService.debug("Tool provider created successfully");
	}

	/**
	 * Runs the MCP server with the given tools.
	 */
	private void runMcpServer(
		final Map<ToolSpecification, ToolExecutor> tools, final int port) throws Exception
	{
		// Create transport provider for streamable HTTP
		final HttpServletStreamableServerTransportProvider transportServlet =
			HttpServletStreamableServerTransportProvider.builder()
				.jsonMapper(McpJsonDefaults.getMapper())
				.mcpEndpoint("/mcp")
				.build();

		// Create MCP server with tools support
		final McpSyncServer mcpServer = McpServer.sync(transportServlet)
			.serverInfo("fiji-mcp-server", FIJI_MCP_VERSION)
			.capabilities(ServerCapabilities.builder()
				.tools(true)
				.build())
			.build();

		// Create and start Jetty server
		final Server jettyServer = new Server(port);
		final ServletContextHandler context = new ServletContextHandler(
			ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		jettyServer.setHandler(context);

		// Register the MCP servlet
		final ServletHolder holder = new ServletHolder(transportServlet);
		context.addServlet(holder, "/mcp/*");

		try {
			jettyServer.start();
			logService.info("Jetty server started on http://localhost:" + port);

			// Register each tool from AiToolService with the MCP server
			for (final Map.Entry<ToolSpecification, ToolExecutor> entry : tools
				.entrySet())
			{
				final ToolSpecification toolSpec = entry.getKey();
				final ToolExecutor toolExecutor = entry.getValue();

				final SyncToolSpecification syncTool = convertToolToSyncSpecification(
					toolSpec, toolExecutor);
				mcpServer.addTool(syncTool);
			}

			toolCount = tools.size();
			logService.info("MCP server started with " + tools.size() + " tools");

			// Signal that the server is ready for client connections
			if (serverReady != null) {
				serverReady.countDown();
			}

			// Keep server running until stop is requested
			while (!stopServer.get()) {
				try {
					Thread.sleep(100);
				} catch (final InterruptedException e) {
					logService.debug("Server thread interrupted, shutting down gracefully");
					break;
				}
			}
		} finally {
			logService.debug("Shutting down MCP server");
			try {
				mcpServer.close();
			} catch (final Exception e) {
				logService.warn("Error closing MCP server", e);
			}
			try {
				jettyServer.stop();
				toolCount = 0;
			logService.debug("Jetty server stopped");
			} catch (final Exception e) {
				logService.warn("Error stopping Jetty server", e);
			}
		}
	}

	/**
	 * Converts a map of named LangChain4j JsonSchemaElements to a
	 * {@code Map<String, Object>} suitable for MCP's JsonSchema properties or
	 * definitions field.
	 */
	private Map<String, Object> convertProperties(
		final Map<String, JsonSchemaElement> source)
	{
		if (source == null) return null;
		final Map<String, Object> result = new LinkedHashMap<>();
		for (final Map.Entry<String, JsonSchemaElement> entry : source.entrySet()) {
			result.put(entry.getKey(), schemaElementToMap(entry.getValue()));
		}
		return result;
	}

	/**
	 * Converts a LangChain4j JsonSchemaElement to a plain {@code Map<String, Object>}
	 * representing the equivalent JSON Schema.
	 */
	private Map<String, Object> schemaElementToMap(
		final JsonSchemaElement element)
	{
		final Map<String, Object> map = new LinkedHashMap<>();
		if (element instanceof JsonStringSchema) {
			map.put("type", "string");
			if (element.description() != null) map.put("description",
				element.description());
		} else if (element instanceof JsonIntegerSchema) {
			map.put("type", "integer");
			if (element.description() != null) map.put("description",
				element.description());
		} else if (element instanceof JsonNumberSchema) {
			map.put("type", "number");
			if (element.description() != null) map.put("description",
				element.description());
		} else if (element instanceof JsonBooleanSchema) {
			map.put("type", "boolean");
			if (element.description() != null) map.put("description",
				element.description());
		} else if (element instanceof JsonNullSchema) {
			map.put("type", "null");
			if (element.description() != null) map.put("description",
				element.description());
		} else if (element instanceof JsonEnumSchema) {
			final JsonEnumSchema e = (JsonEnumSchema) element;
			map.put("type", "string");
			map.put("enum", e.enumValues());
			if (e.description() != null) map.put("description", e.description());
		} else if (element instanceof JsonArraySchema) {
			final JsonArraySchema a = (JsonArraySchema) element;
			map.put("type", "array");
			if (a.items() != null) map.put("items",
				schemaElementToMap(a.items()));
			if (a.description() != null) map.put("description", a.description());
		} else if (element instanceof JsonObjectSchema) {
			final JsonObjectSchema o = (JsonObjectSchema) element;
			map.put("type", "object");
			if (o.description() != null) map.put("description", o.description());
			if (o.properties() != null) map.put("properties",
				convertProperties(o.properties()));
			if (o.required() != null) map.put("required", o.required());
			if (o.additionalProperties() != null) map.put("additionalProperties",
				o.additionalProperties());
			if (o.definitions() != null) map.put("definitions",
				convertProperties(o.definitions()));
		} else if (element instanceof JsonAnyOfSchema) {
			final JsonAnyOfSchema a = (JsonAnyOfSchema) element;
			final List<Object> anyOf = new ArrayList<>();
			for (final JsonSchemaElement e : a.anyOf()) {
				anyOf.add(schemaElementToMap(e));
			}
			map.put("anyOf", anyOf);
			if (a.description() != null) map.put("description", a.description());
		} else if (element instanceof JsonReferenceSchema) {
			final JsonReferenceSchema r = (JsonReferenceSchema) element;
			map.put("$ref", r.reference());
			if (r.description() != null) map.put("description", r.description());
		}
		return map;
	}

	/**
	 * Builds a mapping from argN keys to their description-based names, for use
	 * in remapping the required list.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, String> buildArgKeyMapping(
		final Map<String, Object> rawProperties)
	{
		final Map<String, String> mapping = new LinkedHashMap<>();
		if (rawProperties == null) return mapping;
		for (final Map.Entry<String, Object> entry : rawProperties.entrySet()) {
			if (entry.getValue() instanceof Map) {
				final Map<String, Object> propMap =
					(Map<String, Object>) entry.getValue();
				final Object desc = propMap.get("description");
				if (desc instanceof String) {
					mapping.put(entry.getKey(), (String) desc);
				}
			}
		}
		return mapping;
	}

	/**
	 * Remaps properties from argN-keyed form to name-keyed form using each
	 * property's "description" field as the new key. The "description" field is
	 * removed from each property since it served only as the parameter name.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> remapArgNames(
		final Map<String, Object> rawProperties,
		final Map<String, String> keyMapping)
	{
		if (rawProperties == null) return null;
		final Map<String, Object> result = new LinkedHashMap<>();
		for (final Map.Entry<String, Object> entry : rawProperties.entrySet()) {
			final String newKey = keyMapping.getOrDefault(entry.getKey(),
				entry.getKey());
			if (entry.getValue() instanceof Map) {
				final Map<String, Object> remapped = new LinkedHashMap<>(
					(Map<String, Object>) entry.getValue());
				remapped.remove("description");
				result.put(newKey, remapped);
			} else {
				result.put(newKey, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Converts a LangChain4j ToolSpecification and ToolExecutor into an MCP
	 * SyncToolSpecification.
	 *
	 * @param toolSpec the tool specification
	 * @param toolExecutor the tool executor
	 * @return the converted SyncToolSpecification
	 */
	private SyncToolSpecification convertToolToSyncSpecification(
		final ToolSpecification toolSpec, final ToolExecutor toolExecutor)
	{
		// Convert LangChain4j parameters to MCP JSON schema
		McpSchema.JsonSchema inputSchema = null;
		final JsonObjectSchema params = toolSpec.parameters();
		if (params != null) {
			final Map<String, Object> rawProperties = convertProperties(
				params.properties());
			final Map<String, String> keyMapping = buildArgKeyMapping(rawProperties);
			final Map<String, Object> properties = remapArgNames(rawProperties,
				keyMapping);
			final List<String> required = params.required() == null ? null
				: params.required().stream()
					.map(k -> keyMapping.getOrDefault(k, k))
					.collect(Collectors.toList());
			final Map<String, Object> definitions = convertProperties(
				params.definitions());
			inputSchema = new McpSchema.JsonSchema("object", properties,
				required, params.additionalProperties(), null,
				definitions);
		} else {
			inputSchema = new McpSchema.JsonSchema("object", null, null, null,
				null, null);
		}

		return SyncToolSpecification.builder()
			.tool(Tool.builder()
				.name(toolSpec.name())
				.description(toolSpec.description())
				.inputSchema(inputSchema)
				.build())
			.callHandler((exchange, request) -> {
				try {
					logService.debug("Executing MCP tool: " + toolSpec.name());
					// Create a ToolExecutionRequest for the LangChain4j tool executor
					final ToolExecutionRequest toolRequest = ToolExecutionRequest
						.builder()
						.name(toolSpec.name())
						.arguments(McpJsonDefaults.getMapper()
							.writeValueAsString(request.arguments()))
						.build();
					// Execute the tool and capture result
					final String result = toolExecutor.execute(toolRequest, null);
					return CallToolResult.builder()
						.content(List.of(new McpSchema.TextContent(result)))
						.build();
				} catch (final Exception e) {
					logService.error("Error executing tool " + toolSpec.name(), e);
					return CallToolResult.builder()
						.content(List.of(new McpSchema.TextContent(
							"Error: " + e.getMessage())))
						.isError(true)
						.build();
				}
			})
			.build();
	}
}
