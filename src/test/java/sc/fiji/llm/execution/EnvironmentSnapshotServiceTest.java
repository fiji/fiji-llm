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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.console.ConsoleService;
import org.scijava.console.OutputEvent;
import org.scijava.console.OutputEvent.Source;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;

import com.google.gson.JsonObject;

import ij.measure.ResultsTable;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;
import sc.fiji.llm.Setup;

public class EnvironmentSnapshotServiceTest {

	private Context context;
	private EnvironmentSnapshotService snapshotService;

	@Before
	public void setUp() {
		context = Setup.context();
		snapshotService = context.getService(EnvironmentSnapshotService.class);
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

		assertTrue(environment.entrySet().isEmpty());
	}

	@Test
	public void testCaptureReportsResultsTableAndSciJavaChanges() {
		final LogService logService = context.getService(LogService.class);
		final ConsoleService consoleService = context.getService(ConsoleService.class);
		final EnvironmentSnapshotService.EnvironmentCapture capture = snapshotService
			.capture();
		try {
			logService.info("environment snapshot test message");
			consoleService.notifyListeners(new OutputEvent(context, Source.STDERR,
				"environment snapshot stderr\n", true));
			final ResultsTable table = ResultsTable.getResultsTable();
			table.incrementCounter();
			table.addValue("area", 42);

			final JsonObject environment = capture.finish().toJson();
			final JsonObject changes = environment.getAsJsonObject("changes");
			final JsonObject resultsTable = changes.getAsJsonObject("results_table");

			assertEquals(1, resultsTable.get("rows").getAsInt());
			assertTrue(resultsTable.get("column_headings").getAsString().contains("area"));
			assertTrue(environment.get("scijava_log").getAsString().contains(
				"environment snapshot test message"));
			assertEquals("environment snapshot stderr\n", environment.get("console_stderr")
				.getAsString());
			assertFalse(changes.has("images_opened"));
		}
		finally {
			capture.close();
		}
	}

	@Test
	public void testFinalPixelTrackingReportsInPlaceChanges() {
		final DatasetService datasetService = context.getService(DatasetService.class);
		final DisplayService displayService = context.getService(DisplayService.class);
		final Dataset dataset = datasetService.create(new long[] { 2, 2 }, "pixel test",
			new AxisType[] { Axes.X, Axes.Y }, 8, false, false);
		final ImageDisplay display = (ImageDisplay) displayService.createDisplay(dataset);
		final EnvironmentSnapshotService.EnvironmentCapture capture = snapshotService
			.capture(EnvironmentSnapshotService.PixelChangeTracking.FINAL_SHA256);
		try {
			final Cursor<? extends RealType<?>> cursor = dataset.cursor();
			while (cursor.hasNext()) cursor.next().setReal(255);

			final JsonObject liveChanges = capture.current().toJson().getAsJsonObject(
				"changes");
			assertEquals("deferred", liveChanges.get("pixel_changes").getAsString());
			assertFalse(liveChanges.has("images_changed"));

			final JsonObject environment = capture.finish().toJson();
			final JsonObject changes = environment.getAsJsonObject("changes");
			assertEquals("changed", changes.get("pixel_changes").getAsString());
			assertEquals(1, changes.get("images_changed").getAsJsonArray().size());
			final JsonObject afterImage = changes.get("images_changed").getAsJsonArray()
				.get(0).getAsJsonObject();
			assertEquals("captured", afterImage.get("pixel_hash_status").getAsString());
			assertEquals("SHA-256", afterImage.get("pixel_hash_algorithm").getAsString());
			assertEquals("imglib2-native-storage-v1", afterImage.get("pixel_hash_encoding")
				.getAsString());
			assertTrue(afterImage.has("pixel_hash"));
		}
		finally {
			capture.close();
			display.close();
		}
	}

	@Test
	public void testFinalPixelTrackingSkipsCellImages() {
		final DatasetService datasetService = context.getService(DatasetService.class);
		final DisplayService displayService = context.getService(DisplayService.class);
		final Dataset dataset = datasetService.create(new long[] { 2, 2 }, "cell test",
			new AxisType[] { Axes.X, Axes.Y }, 8, false, false, true);
		final ImageDisplay display = (ImageDisplay) displayService.createDisplay(dataset);
		final EnvironmentSnapshotService.EnvironmentCapture capture = snapshotService
			.capture(EnvironmentSnapshotService.PixelChangeTracking.FINAL_SHA256);
		try {
			final JsonObject environment = capture.finish().toJson();
			final JsonObject changes = environment.getAsJsonObject("changes");
			assertEquals("inconclusive", changes.get("pixel_changes").getAsString());
			assertFalse(changes.has("images_changed"));
		}
		finally {
			capture.close();
			display.close();
		}
	}
}
