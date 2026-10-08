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
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.prefs.PrefService;

import dev.langchain4j.agent.tool.ToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import sc.fiji.llm.Setup;
import sc.fiji.llm.tools.AiToolService;

/**
 * Unit tests for DefaultMCPService.
 */
public class DefaultMCPServiceTest {

	private static Context context;
	private MCPService mcpService;
	private AiToolService aiToolService;
	private PrefService prefService;
	private int originalPort;
	private int testPort;

	@BeforeClass
	public static void setUpContext() {
		context = Setup.context();
	}

	@AfterClass
	public static void disposeContext() {
		Setup.dispose(context);
	}

	@Before
	public void setUp() throws Exception {
		prefService = context.getService(PrefService.class);
		originalPort = prefService.getInt(MCPService.class, MCPService.PORT_KEY,
			MCPService.DEFAULT_PORT);
		prefService.put(MCPService.class, MCPService.PORT_KEY, 0);
		aiToolService = context.getService(AiToolService.class);
		mcpService = new DefaultMCPService();
		context.inject(mcpService);
	}

	@After
	public void tearDown() {
		if (mcpService != null) {
			mcpService.dispose();
		}
		if (prefService != null) {
			prefService.put(MCPService.class, MCPService.PORT_KEY, originalPort);
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
		startMcpServer();

		// And: the server should be running after initialization
		assertTrue(mcpService.isServerRunning());
	}

	@Test
	public void testToolNamesMatchRegisteredTools() {
		startMcpServer();

		assertEquals(mcpService.getToolCount(), mcpService.getToolNames().size());
		assertEquals(mcpService.getToolNames(), mcpService.getToolNames().stream()
			.sorted().toList());
		assertTrue(mcpService.getToolNames().stream().allMatch(name -> name.startsWith(
			"fiji_")));
		assertTrue(aiToolService.getToolsWithExecutors().keySet().stream().anyMatch(
			specification -> "fiji_conversation_name".equals(specification.name())));
		assertFalse(mcpService.getToolNames().contains("fiji_conversation_name"));
	}

	@Test
	public void testServerRecoversAfterJettyStops() throws Exception {
		startMcpServer();
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
		startMcpServer();

		assertEquals(403, getMcpResponseCode("https://attacker.example"));
	}

	@Test
	public void testAllowsLoopbackOrigin() throws Exception {
		startMcpServer();

		assertTrue(getMcpResponseCode("http://127.0.0.1:" + testPort) != 403);
	}

	@Test
	public void testServerPort() {
		// When: we start the server and get its bound port
		startMcpServer();
		final int port = mcpService.getServerPort();

		// Then: it should be an ephemeral port assigned by the operating system
		assertTrue(port > 0);
		assertTrue(port == testPort);
	}

	@Test
	public void testServerWithTools() {
		// Given: an MCPService with available tools
		assertNotNull(aiToolService);

		// When: we start the server
		startMcpServer();

		// Then: the server should expose the discovered tools
		assertTrue(mcpService.isServerRunning());
		assertTrue(mcpService.getToolCount() > 0);
	}

	private void startMcpServer() {
		mcpService.startServer();
		testPort = mcpService.getServerPort();
		assertTrue(testPort > 0);
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
			try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				socket.getOutputStream(), StandardCharsets.US_ASCII)))
			{
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
}
