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

package sc.fiji.llm.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LogUtilsTest {

	@Test
	public void testDeltaForAppendedText() {
		assertEquals("new\n", LogUtils.delta("old\n", "old\nnew\n"));
	}

	@Test
	public void testDeltaForResetText() {
		assertEquals("replacement", LogUtils.delta("old", "replacement"));
	}

	@Test
	public void testTextLogsDelta() {
		final TextLogs initial = new TextLogs("output\n", "error\n");
		final TextLogs current = new TextLogs("output\nnew\n", "error\nnew\n");
		final TextLogs delta = current.deltaFrom(initial);

		assertEquals("new\n", delta.getOutput());
		assertEquals("new\n", delta.getErrors());
	}

	@Test
	public void testTextLogsDeltaBeforeRemovingKnownNoise() {
		final TextLogs initial = new TextLogs("Started first at now\noutput\n",
			"Started first at now\nerror\n[INFO] Execution errors handled by the Macro Interpreter.\n");
		final TextLogs current = new TextLogs(
			"Started first at now\noutput\nStarted second at later\nnew\n",
			"Started first at now\nerror\n[INFO] Execution errors handled by the Macro Interpreter.\n" +
				"Started second at later\nnew\n");
		final TextLogs delta = current.deltaFrom(initial)
			.withoutGenericMacroInterpreterMessages();

		assertEquals("new\n", delta.getOutput());
		assertEquals("new\n", delta.getErrors());
	}

	@Test
	public void testTextLogsRemoveStartedBanners() {
		final TextLogs logs = new TextLogs("Started script at now\noutput\n",
			"Started script at now\nerror\n");
		final TextLogs cleaned = logs.withoutStartedBanners();

		assertEquals("output\n", cleaned.getOutput());
		assertEquals("error\n", cleaned.getErrors());
	}
}
