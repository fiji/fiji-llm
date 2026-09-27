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

package sc.fiji.llm.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class AWTDialogUtilsTest {

	private Dialog dialog;
	private boolean closeRequested;

	@Before
	public void setUp() {
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		dialog = new Dialog((java.awt.Frame) null, "Close test");
		dialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent event) {
				closeRequested = true;
				event.getWindow().dispose();
			}
		});
		dialog.setSize(100, 100);
		dialog.setVisible(true);
	}

	@After
	public void tearDown() {
		if (dialog != null) dialog.dispose();
	}

	@Test
	public void closeDialogDispatchesWindowClosingAndReportsVisibility() {
		final AWTDialogUtils.DialogCloseResponse response = AWTDialogUtils
			.closeDialog("Close test");

		assertTrue(closeRequested);
		assertFalse(response.isDialogVisibleAfter());
		assertFalse(dialog.isVisible());
		assertTrue("Close test".equals(response.getDialogTitle()));
	}
}
