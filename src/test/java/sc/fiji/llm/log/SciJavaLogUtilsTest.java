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

package sc.fiji.llm.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.console.ConsoleService;
import org.scijava.console.OutputEvent;
import org.scijava.console.OutputEvent.Source;
import org.scijava.log.LogService;

import sc.fiji.llm.Setup;
import sc.fiji.llm.log.SciJavaLogUtils.LogCapture;
import sc.fiji.llm.log.SciJavaLogUtils.LogMessages;

public class SciJavaLogUtilsTest {

	private Context context;

	@Before
	public void setUp() {
		context = Setup.context();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testCaptureDeltaAndClose() {
		final LogService logService = context.getService(LogService.class);
		final ConsoleService consoleService = context.getService(ConsoleService.class);
		final LogCapture capture = SciJavaLogUtils.capture(logService, consoleService);
		try {
			final LogMessages initial = capture.getLogs();
			logService.info("captured message");
			consoleService.notifyListeners(new OutputEvent(context, Source.STDERR,
				"captured stderr\n", true));

			final LogMessages delta = capture.getLogs().deltaFrom(initial);
			assertEquals(1, delta.getMessages().size());
			assertTrue(delta.getText().contains("captured message"));
			assertEquals("captured stderr\n", delta.getStderr());

			final int capturedCount = capture.getLogs().getMessages().size();
			capture.close();
			logService.info("ignored message");
			assertEquals(capturedCount, capture.getLogs().getMessages().size());
		}
		finally {
			capture.close();
		}
	}
}
