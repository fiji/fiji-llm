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

package sc.fiji.llm.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.prefs.PrefService;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.service.tool.ToolExecutionResult;
import io.modelcontextprotocol.spec.McpSchema;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.Setup;
import sc.fiji.llm.data.ImageJ1HelperService;
import sc.fiji.llm.tools.AiToolService;

/**
 * Unit tests for DefaultMCPService.
 */
public class DefaultMCPServiceTest {

	private Context context;
	private MCPService mcpService;
	private AiToolService aiToolService;
	private PrefService prefService;
	private int originalPort;
	private Preferences mcpPreferences;
	private String originalLaunchOnStartup;
	private int testPort;

	@Before
	public void setUp() throws Exception {
		mcpPreferences = Preferences.userNodeForPackage(MCPService.class)
			.node(MCPService.class.getSimpleName());
		originalLaunchOnStartup = mcpPreferences.get(
			MCPService.LAUNCH_ON_START_KEY, null);
		mcpPreferences.putBoolean(MCPService.LAUNCH_ON_START_KEY, false);

		context = Setup.context();
		prefService = context.getService(PrefService.class);
		originalPort = prefService.getInt(MCPService.class, MCPService.PORT_KEY,
			MCPService.DEFAULT_PORT);
		try (ServerSocket socket = new ServerSocket(0)) {
			testPort = socket.getLocalPort();
		}
		prefService.put(MCPService.class, MCPService.PORT_KEY, testPort);
		mcpService = context.getService(MCPService.class);
		aiToolService = context.getService(AiToolService.class);
	}

	@After
	public void tearDown() {
		if (mcpService != null) {
			mcpService.dispose();
		}
		if (prefService != null) {
			prefService.put(MCPService.class, MCPService.PORT_KEY, originalPort);
		}
		if (context != null) {
			context.dispose();
		}
		if (mcpPreferences != null) {
			if (originalLaunchOnStartup == null) {
				mcpPreferences.remove(MCPService.LAUNCH_ON_START_KEY);
			}
			else {
				mcpPreferences.put(MCPService.LAUNCH_ON_START_KEY,
					originalLaunchOnStartup);
			}
		}
	}

	@Test
	public void testMCPServiceInitialization() {
		// Given: a fresh MCPService
		assertNotNull(mcpService);

		// When/Then: service should not be running initially
		assertFalse(mcpService.isServerRunning());
	}

	@Test
	public void testInputSchemasUseToolParameterNames() {
		for (final ToolSpecification spec : aiToolService.getToolsWithExecutors()
			.keySet())
		{
			if (spec.parameters() == null) continue;
			final McpSchema.JsonSchema schema = ((DefaultMCPService) mcpService)
				.toInputSchema(spec);
			assertEquals(spec.name(), spec.parameters().properties().keySet(), schema
				.properties().keySet());
			assertEquals(spec.name(), spec.parameters().required(), schema
				.required());
		}
	}

	@Test
	public void testStartServer() {
		// Given: a fresh MCPService
		assertNotNull(mcpService);

		// When: we start the server
		mcpService.startServer();

		// And: the server should be running after initialization
		assertTrue(mcpService.isServerRunning());
	}

	@Test
	public void testServerRecoversAfterJettyStops() throws Exception {
		mcpService.startServer();
		final Field jettyServerField = DefaultMCPService.class
			.getDeclaredField("jettyServer");
		jettyServerField.setAccessible(true);
		final Server jettyServer = (Server) jettyServerField.get(mcpService);
		assertNotNull(jettyServer);

		jettyServer.stop();
		waitForServerState(false);
		waitForServerState(true);

		assertTrue(mcpService.isServerRunning());
	}

	@Test
	public void testRejectsExternalOrigin() throws Exception {
		mcpService.startServer();

		assertEquals(403, getMcpResponseCode("https://attacker.example"));
	}

	@Test
	public void testAllowsLoopbackOrigin() throws Exception {
		mcpService.startServer();

		assertTrue(getMcpResponseCode("http://127.0.0.1:" + testPort) != 403);
	}

	@Test
	public void testServerPort() {
		// When: we get the server port
		final int port = mcpService.getServerPort();

		// Then: it should be the configured test port
		assertTrue(port > 0);
		assertTrue(port == testPort);
	}

	@Test
	public void testDispose() {
		// Given: a running MCPService
		mcpService.startServer();
		assertTrue(mcpService.isServerRunning());

		// When: we dispose the service
		mcpService.dispose();

		// Then: the service should no longer be running
		assertFalse(mcpService.isServerRunning());
	}

	@Test(expected = IllegalStateException.class)
	public void testStartServerAfterDispose() {
		// Given: a disposed MCPService
		mcpService.startServer();
		mcpService.dispose();

		// When/Then: requesting a ToolProvider should throw an exception
		mcpService.startServer();
	}

	@Test
	public void testServerWithTools() {
		// Given: an MCPService with available tools
		assertNotNull(aiToolService);

		// When: we start the server
		mcpService.startServer();

		// Then: the server should expose the discovered tools
		assertTrue(mcpService.isServerRunning());
		assertTrue(mcpService.getToolCount() > 0);
	}

	@Test
	public void testImageToolsHaveLegacyDependencies() {
		LegacyService legacyService = context.getService(LegacyService.class);
		assertNotNull(legacyService);
		assertTrue(legacyService.isActive());
		assertNotNull(legacyService.getIJ1Helper());
		assertNotNull(context.getService(ImageJ1HelperService.class));

		assertTrue(aiToolService.getToolsWithExecutors().keySet().stream()
			.anyMatch(specification -> "fiji_image_list".equals(specification.name())));
		assertTrue(aiToolService.getToolsWithExecutors().keySet().stream()
			.anyMatch(specification -> "fiji_image_view".equals(specification.name())));
	}

	@Test
	public void testConvertsMultimodalToolResult() {
		final ToolExecutionResult result = ToolExecutionResult.builder()
			.resultContents(List.of(TextContent.from("image result"), ImageContent.from(
				"AQID", "image/png")))
			.build();

		final List<McpSchema.Content> contents = DefaultMCPService
			.convertToolResult(result);

		assertEquals(2, contents.size());
		assertTrue(contents.get(0) instanceof McpSchema.TextContent);
		assertTrue(contents.get(1) instanceof McpSchema.ImageContent);
		final McpSchema.ImageContent image = (McpSchema.ImageContent) contents.get(1);
		assertEquals("AQID", image.data());
		assertEquals("image/png", image.mimeType());
	}

	private void waitForServerState(final boolean expected)
		throws InterruptedException
	{
		final long deadline = System.nanoTime() +
			TimeUnit.SECONDS.toNanos(5);
		while (mcpService.isServerRunning() != expected &&
			System.nanoTime() < deadline)
		{
			Thread.sleep(50);
		}
		assertTrue("Unexpected MCP server running state",
			mcpService.isServerRunning() == expected);
	}

	private int getMcpResponseCode(final String origin)
		throws Exception
	{
		try (Socket socket = new Socket("127.0.0.1", testPort)) {
			final PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				socket.getOutputStream(), StandardCharsets.US_ASCII));
			writer.print("GET /mcp HTTP/1.1\r\n");
			writer.print("Host: 127.0.0.1:" + testPort + "\r\n");
			writer.print("Origin: " + origin + "\r\n");
			writer.print("Accept: application/json, text/event-stream\r\n");
			writer.print("Connection: close\r\n\r\n");
			writer.flush();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream(), StandardCharsets.US_ASCII)))
			{
				final String statusLine = reader.readLine();
				return Integer.parseInt(statusLine.split(" ")[1]);
			}
		}
	}
}
