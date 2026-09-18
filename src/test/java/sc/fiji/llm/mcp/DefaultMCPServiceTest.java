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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.prefs.PrefService;

import dev.langchain4j.service.tool.ToolProvider;
import net.imagej.legacy.LegacyService;
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

		context = new Context();
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
	public void testGetToolProvider() {
		// Given: a fresh MCPService
		assertNotNull(mcpService);

		// When: we request a ToolProvider
		final ToolProvider toolProvider = mcpService.getToolProvider();

		// Then: we should get a non-null ToolProvider
		assertNotNull(toolProvider);

		// And: the server should be running after initialization
		assertTrue(mcpService.isServerRunning());
	}

	@Test
	public void testServerRecoversAfterJettyStops() throws Exception {
		final ToolProvider toolProvider = mcpService.getToolProvider();
		final Field jettyServerField = DefaultMCPService.class
			.getDeclaredField("jettyServer");
		jettyServerField.setAccessible(true);
		final Server jettyServer = (Server) jettyServerField.get(mcpService);
		assertNotNull(jettyServer);

		jettyServer.stop();
		waitForServerState(false);
		waitForServerState(true);

		assertSame(toolProvider, mcpService.getToolProvider());
	}

	@Test
	public void testRejectsExternalOrigin() throws Exception {
		mcpService.getToolProvider();

		assertEquals(403, getMcpResponseCode("https://attacker.example"));
	}

	@Test
	public void testAllowsLoopbackOrigin() throws Exception {
		mcpService.getToolProvider();

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
		final ToolProvider toolProvider = mcpService.getToolProvider();
		assertNotNull(toolProvider);
		assertTrue(mcpService.isServerRunning());

		// When: we dispose the service
		mcpService.dispose();

		// Then: the service should no longer be running
		assertFalse(mcpService.isServerRunning());
	}

	@Test(expected = IllegalStateException.class)
	public void testGetToolProviderAfterDispose() {
		// Given: a disposed MCPService
		mcpService.getToolProvider();
		mcpService.dispose();

		// When/Then: requesting a ToolProvider should throw an exception
		mcpService.getToolProvider();
	}

	@Test
	public void testToolProviderWithTools() {
		// Given: an MCPService with available tools
		assertNotNull(aiToolService);

		// When: we get the ToolProvider
		final ToolProvider toolProvider = mcpService.getToolProvider();

		// Then: it should be non-null
		assertNotNull(toolProvider);

		// And: calling getToolProvider again should return the same provider
		final ToolProvider toolProvider2 = mcpService.getToolProvider();
		assertNotNull(toolProvider2);
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
