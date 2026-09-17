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

package sc.fiji.llm.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.console.ConsoleService;
import org.scijava.console.OutputEvent;
import org.scijava.console.OutputEvent.Source;
import org.scijava.log.LogService;

import com.google.gson.JsonObject;

import ij.measure.ResultsTable;

public class ExecutionEnvironmentSnapshotServiceTest {

	private Context context;
	private ExecutionEnvironmentSnapshotService snapshotService;

	@Before
	public void setUp() {
		context = new Context();
		snapshotService = context.getService(ExecutionEnvironmentSnapshotService.class);
		ResultsTable.getResultsTable().reset();
	}

	@After
	public void tearDown() {
		ResultsTable.getResultsTable().reset();
		context.dispose();
	}

	@Test
	public void testEmptyCaptureHasStableEnvironmentShape() {
		final JsonObject environment = snapshotService.capture().finish().toJson();

		assertTrue(environment.has("before"));
		assertTrue(environment.has("after"));
		assertTrue(environment.has("changes"));
		assertTrue(environment.has("imagej_log"));
		assertTrue(environment.has("scijava_log"));
		assertTrue(environment.has("console_stdout"));
		assertTrue(environment.has("console_stderr"));
		assertTrue(environment.getAsJsonObject("before").has("images"));
		assertTrue(environment.getAsJsonObject("before").has("active_image"));
		assertTrue(environment.getAsJsonObject("before").has("results_table"));
		assertTrue(environment.getAsJsonObject("changes").has("images_opened"));
		assertTrue(environment.getAsJsonObject("changes").has("images_closed"));
		assertFalse(environment.getAsJsonObject("changes").get("active_image_changed")
			.getAsBoolean());
	}

	@Test
	public void testCaptureReportsResultsTableAndSciJavaChanges() {
		final LogService logService = context.getService(LogService.class);
		final ConsoleService consoleService = context.getService(ConsoleService.class);
		final ExecutionEnvironmentSnapshotService.EnvironmentCapture capture = snapshotService
			.capture();
		try {
			logService.info("environment snapshot test message");
			consoleService.notifyListeners(new OutputEvent(context, Source.STDERR,
				"environment snapshot stderr\n", true));
			final ResultsTable table = ResultsTable.getResultsTable();
			table.incrementCounter();
			table.addValue("area", 42);

			final JsonObject environment = capture.finish().toJson();
			final JsonObject afterResults = environment.getAsJsonObject("after")
				.getAsJsonObject("results_table");
			final JsonObject changes = environment.getAsJsonObject("changes");

			assertEquals(1, afterResults.get("rows").getAsInt());
			assertTrue(afterResults.get("column_headings").getAsString().contains("area"));
			assertTrue(changes.get("results_table_changed").getAsBoolean());
			assertTrue(environment.get("scijava_log").getAsString().contains(
				"environment snapshot test message"));
			assertEquals("environment snapshot stderr\n", environment.get("console_stderr")
				.getAsString());
			assertTrue(changes.get("images_opened").isJsonArray());
		}
		finally {
			capture.close();
		}
	}
}
