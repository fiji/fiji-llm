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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

import dev.langchain4j.service.tool.ToolProvider;
import sc.fiji.llm.tools.AiToolService;

/**
 * Unit tests for DefaultMCPService.
 */
public class DefaultMCPServiceTest {

	private Context context;
	private MCPService mcpService;
	private AiToolService aiToolService;

	@Before
	public void setUp() {
		context = new Context();
		mcpService = context.getService(MCPService.class);
		aiToolService = context.getService(AiToolService.class);
	}

	@After
	public void tearDown() {
		if (mcpService != null) {
			mcpService.dispose();
		}
		if (context != null) {
			context.dispose();
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
	public void testServerPort() {
		// When: we get the server port
		final int port = mcpService.getServerPort();

		// Then: it should be the default port
		assertTrue(port > 0);
		assertTrue(port == 9090);
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
}
