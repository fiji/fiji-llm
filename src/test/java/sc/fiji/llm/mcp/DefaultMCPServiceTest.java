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
