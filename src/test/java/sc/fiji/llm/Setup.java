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
package sc.fiji.llm;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.prefs.Preferences;

import org.scijava.Context;
import org.scijava.launcher.ReflectionUnlocker;

import sc.fiji.llm.mcp.MCPService;

/**
 * Helper class for setting up SciJava {@link Context}s with
 * ImageJ Legacy support in Java 17+.
 */
public final class Setup {

	private static final Preferences MCP_PREFERENCES = Preferences
		.userNodeForPackage(MCPService.class).node(MCPService.class.getSimpleName());
	private static final Set<Context> CONTEXTS = Collections.newSetFromMap(
		new IdentityHashMap<>());
	private static String originalLaunchOnStartup;
	private static boolean launchPreferenceCaptured;

	static {
		// NB: Necessary for ImageJ Legacy support in Java 17+.
		ReflectionUnlocker.unlockAll();
	}

	/**
	 * Creates a test context without launching the MCP server.
	 *
	 * @return a new SciJava context
	 */
	public static synchronized Context context() {
		if (CONTEXTS.isEmpty()) {
			originalLaunchOnStartup = MCP_PREFERENCES.get(
				MCPService.LAUNCH_ON_START_KEY, null);
			MCP_PREFERENCES.putBoolean(MCPService.LAUNCH_ON_START_KEY, false);
			launchPreferenceCaptured = true;
		}

		try {
			final Context context = new Context();
			CONTEXTS.add(context);
			return context;
		}
		catch (final RuntimeException e) {
			if (CONTEXTS.isEmpty()) restoreLaunchOnStartup();
			throw e;
		}
	}

	/**
	 * Disposes a test context and restores MCP startup preferences when no
	 * managed test contexts remain.
	 *
	 * @param context the test context to dispose
	 */
	public static synchronized void dispose(final Context context) {
		try {
			context.dispose();
		}
		finally {
			CONTEXTS.remove(context);
			if (CONTEXTS.isEmpty()) restoreLaunchOnStartup();
		}
	}

	private static void restoreLaunchOnStartup() {
		if (!launchPreferenceCaptured) return;
		if (originalLaunchOnStartup == null) {
			MCP_PREFERENCES.remove(MCPService.LAUNCH_ON_START_KEY);
		}
		else {
			MCP_PREFERENCES.put(MCPService.LAUNCH_ON_START_KEY,
				originalLaunchOnStartup);
		}
		originalLaunchOnStartup = null;
		launchPreferenceCaptured = false;
	}
}
