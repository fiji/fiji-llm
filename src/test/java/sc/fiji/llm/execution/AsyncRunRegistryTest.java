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
package sc.fiji.llm.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class AsyncRunRegistryTest {

	@Test
	public void allowsOneActiveRunPerCategoryAndReleasesOnCompletion() {
		final AtomicLong now = new AtomicLong();
		final AsyncRunRegistry<String, Object> registry = new AsyncRunRegistry<>(1000,
			now::get, false);
		final Object scriptRun = new Object();

		registry.start("script", "script-1", scriptRun);
		try {
			registry.start("script", "script-2", new Object());
			fail("Expected a duplicate script run to be rejected");
		}
		catch (final AsyncRunRegistry.RunInProgressException e) {
			assertEquals("script-1", e.getActiveRunID());
		}
		registry.start("macro", "macro-1", new Object());
		assertSame(scriptRun, registry.getActive("script"));

		registry.complete("script-1");
		assertNull(registry.getActive("script"));
		assertSame(scriptRun, registry.get("script-1"));
		registry.start("script", "script-3", new Object());
	}

	@Test
	public void expiresTerminalRunsAfterRetentionPeriod() {
		final AtomicLong now = new AtomicLong(100);
		final AsyncRunRegistry<String, Object> registry = new AsyncRunRegistry<>(1000,
			now::get, false);
		final Object run = new Object();
		registry.start("command", "command-1", run);
		registry.complete("command-1");

		now.set(1099);
		assertSame(run, registry.get("command-1"));
		now.set(1100);
		assertNull(registry.get("command-1"));
		registry.start("command", "command-2", new Object());
	}
}
