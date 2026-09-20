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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
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
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.invocation.InvocationContext;
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
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.AiToolService;

/**
 * Default implementation of MCPService.
 * <p>
 * Manages an MCP server running on localhost that exposes tools discovered by
 * AiToolService and makes them available to external MCP clients.
 * </p>
 */
@Plugin(type = Service.class)
public class DefaultMCPService extends AbstractService implements MCPService
{

	private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration RESTART_DELAY = Duration.ofSeconds(1);
	private static final Duration SERVER_POLL_INTERVAL = Duration.ofMillis(100);
	private static final int MAX_RESTART_ATTEMPTS = 3;
	private static final String LOOPBACK_HOST = "127.0.0.1";
	private static final String FIJI_MCP_VERSION = "0.1.0";
	private static final String MCP_INSTRUCTIONS =
		"""
This server exposes tools for inspecting and interacting with the current Fiji/ImageJ session.

Tool calls may modify application state, scripts, images, or other workspace artifacts.

State-query tools provide a current view of the application. Users may interact with Fiji between tool calls, so re-query state when accuracy matters.

Prefer inspection before modification. Use the narrowest applicable tool, and avoid modifying state unless it is necessary to fulfill the user's request.
""";

	@Parameter
	private LogService logService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private AiToolService aiToolService;

	private volatile Server jettyServer;
	private volatile Thread serverThread;
	private volatile int toolCount = 0;
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final AtomicBoolean disposed = new AtomicBoolean(false);
	private final AtomicBoolean stopServer = new AtomicBoolean(false);
	private CountDownLatch serverReady;

	@Override
	public synchronized void startServer() {
		if (disposed.get()) {
			throw new IllegalStateException(
				"MCPService has been disposed and cannot be reused");
		}

		if (!isServerRunning() &&
			(serverThread == null || !serverThread.isAlive()))
		{
			initializeServer();
		}

		if (!isServerRunning()) {
			throw new IllegalStateException("MCP server is not available");
		}
	}

	@Override
	public boolean isServerRunning() {
		final Server currentJettyServer = jettyServer;
		return initialized.get() &&
			serverThread != null && serverThread.isAlive() &&
			currentJettyServer != null && currentJettyServer.isRunning();
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
	public synchronized void dispose() {
		if (!disposed.compareAndSet(false, true)) {
			return; // Already disposed
		}

		try {
			stopServer.set(true);
			final Thread currentServerThread = serverThread;
			if (currentServerThread != null && currentServerThread.isAlive()) {
				logService.info("Shutting down MCP server thread");
				currentServerThread.join(SHUTDOWN_TIMEOUT.toMillis());
				if (currentServerThread.isAlive()) {
					logService.warn("MCP server thread did not shut down gracefully");
					currentServerThread.interrupt();
				}
			}
		} catch (final InterruptedException e) {
			logService.error("Error while disposing MCPService", e);
			Thread.currentThread().interrupt();
		} finally {
			initialized.set(false);
			toolCount = 0;
			jettyServer = null;
			logService.info("MCPService disposed successfully");
		}
	}

	private synchronized void initializeServer() {
		if (isServerRunning() || disposed.get() ||
			(serverThread != null && serverThread.isAlive()))
		{
			return;
		}

		try {
			final int port = getServerPort();
			logService.info("Initializing MCP server on localhost:" + port);

			// Initialize the server ready signal
			serverReady = new CountDownLatch(1);
			final Exception[] startupException = new Exception[1];
			stopServer.set(false);

			// Start MCP server in a daemon thread
			serverThread = new Thread(() -> runMcpServerWithRecovery(
				aiToolService.getToolsWithExecutors(), port, startupException));
			serverThread.setDaemon(true);
			serverThread.setName("MCP-Server-Thread");
			serverThread.start();

			// Wait for server to be ready (with timeout)
			logService.debug("Waiting for Fiji MCP server to be ready...");
			final boolean serverStarted = serverReady.await(STARTUP_TIMEOUT.toMillis(),
				TimeUnit.MILLISECONDS);

			final String startupMessage = startupException[0] == null ? null :
				startupException[0].getMessage();
			if (startupMessage != null && startupMessage.contains("Failed to bind")) {
				logService.error("Fiji MCP server failed to startup: another instance of the server may be running");
				return;
			}

			if (!serverStarted) {
				if (startupException[0] != null) {
					throw new RuntimeException( "Encountered exception during server startup",
					startupException[0]);
				}
				throw new RuntimeException(
					"Fiji MCP server failed to start for unknown reasons");
			}

			logService.debug("Fiji MCP server is ready");

			initialized.set(true);
			logService.info("Fiji MCP server initialized successfully");
		} catch (final Exception e) {
			initialized.set(false);
			logService.error("Failed to initialize Fiji MCP server", e);
			throw new RuntimeException("Failed to initialize Fiji MCP server", e);
		}
	}

	/**
	 * Runs the MCP server with the given tools.
	 */
	private void runMcpServerWithRecovery(
		final Map<ToolSpecification, ToolExecutor> tools, final int port,
		final Exception[] startupException)
	{
		boolean serverHasStarted = false;
		int restartAttempts = 0;
		while (!stopServer.get()) {
			try {
				runMcpServer(tools, port);
			} catch (final Exception e) {
				if (!serverHasStarted && serverReady.getCount() > 0) {
					startupException[0] = e;
					serverReady.countDown();
					return;
				}
				logService.error("MCP server failed unexpectedly", e);
			}

			if (serverReady.getCount() > 0) {
				if (stopServer.get()) {
					serverReady.countDown();
					return;
				}
				startupException[0] = new RuntimeException(
					"MCP server stopped before startup completed");
				serverReady.countDown();
				return;
			}

			serverHasStarted = true;
			if (stopServer.get()) return;

			initialized.set(false);
			toolCount = 0;
			if (++restartAttempts > MAX_RESTART_ATTEMPTS) {
				logService.error("MCP server exceeded the maximum number of restart attempts");
				return;
			}

			logService.warn("MCP server stopped unexpectedly; restarting (attempt " +
				restartAttempts + " of " + MAX_RESTART_ATTEMPTS + ")");
			try {
				Thread.sleep(RESTART_DELAY.toMillis());
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

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
			.instructions(buildMCPInstructions())
			.capabilities(ServerCapabilities.builder()
				.tools(true)
				.build())
			.build();

		// Create and start Jetty server
		final Server jettyServer = new Server();
		final ServerConnector connector = new ServerConnector(jettyServer);
		connector.setHost(LOOPBACK_HOST);
		connector.setPort(port);
		jettyServer.addConnector(connector);
		this.jettyServer = jettyServer;
		final ServletContextHandler context = new ServletContextHandler(
			ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		jettyServer.setHandler(context);

		// Register the MCP servlet
		final ServletHolder holder = new ServletHolder(transportServlet);
		context.addServlet(holder, "/mcp/*");
		context.addFilter(new FilterHolder(new OriginValidationFilter()), "/*",
			EnumSet.of(DispatcherType.REQUEST));

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
			if (!disposed.get()) {
				initialized.set(true);
			}

			// Keep server running until stop is requested
			while (!stopServer.get() && jettyServer.isRunning()) {
				try {
					Thread.sleep(SERVER_POLL_INTERVAL.toMillis());
				} catch (final InterruptedException e) {
					logService.debug("Server thread interrupted, shutting down gracefully");
					break;
				}
			}
			if (!stopServer.get() && !jettyServer.isRunning()) {
				logService.warn("Jetty server stopped unexpectedly");
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
			if (this.jettyServer == jettyServer) {
				this.jettyServer = null;
			}
		}
	}

	private String buildMCPInstructions()
	{
		final StringJoiner sj = new StringJoiner("\n");
		sj.add(MCP_INSTRUCTIONS);
		for (AiToolPlugin toolPlugin : aiToolService.getInstances()) {
			sj.add(toolPlugin.getUsage());
		}
		return sj.toString();
	}

	private static class OriginValidationFilter implements Filter
	{

		@Override
		public void doFilter(final ServletRequest request,
			final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException
		{
			final HttpServletRequest httpRequest = (HttpServletRequest) request;
			final String origin = httpRequest.getHeader("Origin");
			if (origin != null && !isAllowedOrigin(origin)) {
				((HttpServletResponse) response).sendError(
					HttpServletResponse.SC_FORBIDDEN, "Invalid Origin");
				return;
			}
			chain.doFilter(request, response);
		}

		private static boolean isAllowedOrigin(final String origin)
		{
			try {
				final URI uri = URI.create(origin);
				final String host = uri.getHost();
				return "http".equalsIgnoreCase(uri.getScheme()) &&
					host != null && (LOOPBACK_HOST.equalsIgnoreCase(host) ||
						"localhost".equalsIgnoreCase(host)) &&
					uri.getRawUserInfo() == null && uri.getRawPath().isEmpty() &&
					uri.getRawQuery() == null && uri.getRawFragment() == null;
			} catch (final IllegalArgumentException e) {
				return false;
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
		final Map<String, String> reverseMapping = new LinkedHashMap<>();
		if (params != null) {
			final Map<String, Object> rawProperties = convertProperties(
				params.properties());
			final Map<String, String> keyMapping = buildArgKeyMapping(rawProperties);
			// Build reverse mapping: friendly name -> argN for use in callHandler
			keyMapping.forEach((argN, friendlyName) -> reverseMapping.put(friendlyName, argN));
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
					// Remap argument keys from friendly names back to positional argN names
					final Map<String, Object> remappedArgs = new LinkedHashMap<>();
					if (request.arguments() != null) {
						request.arguments().forEach((k, v) ->
							remappedArgs.put(reverseMapping.getOrDefault(k, k), v));
					}
					// Create a ToolExecutionRequest for the LangChain4j tool executor
					final String args = McpJsonDefaults.getMapper()
							.writeValueAsString(remappedArgs);
					final ToolExecutionRequest toolRequest = ToolExecutionRequest
						.builder()
						.name(toolSpec.name())
						.arguments(args)
						.build();
					// Execute the tool and capture result
					final ToolExecutionResult result = toolExecutor.executeWithContext(
						toolRequest, InvocationContext.builder().build());
					return CallToolResult.builder()
						.content(convertToolResult(result))
						.isError(result.isError())
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

	static List<McpSchema.Content> convertToolResult(
		final ToolExecutionResult result)
	{
		final List<McpSchema.Content> contents = new ArrayList<>();
		for (final Content content : result.resultContents()) {
			if (content instanceof TextContent) {
				contents.add(new McpSchema.TextContent(((TextContent) content).text()));
			}
			else if (content instanceof ImageContent) {
				final ImageContent image = (ImageContent) content;
				contents.add(new McpSchema.ImageContent(null, image.image().base64Data(),
					image.image().mimeType()));
			}
			else {
				throw new IllegalArgumentException("Unsupported tool result content: " +
					content.getClass().getName());
			}
		}
		return contents;
	}
}
