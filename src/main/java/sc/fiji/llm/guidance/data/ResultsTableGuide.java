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
package sc.fiji.llm.guidance.data;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;

/** Guidance for the ImageJ Results Table. */
@Plugin(type = AgentGuide.class)
public class ResultsTableGuide extends AbstractAgentGuide {

	public static final String ID = "results-table";

	private static final String CONTENT = """
			# ImageJ 1.x Results Table

			`ij.measure.ResultsTable` is ImageJ 1.x's general-purpose table for measurement
			and script or plugin output. It stores rows and named columns, with numeric and
			text values and optional row labels. A row records whatever operation wrote it;
			it does not automatically retain a reference to the source image or ROI.

			## Relationship to image measurement

			ImageJ's `Analyzer` writes the default Results Table used by Analyze/Measure.
			The active `ImagePlus`, its current ROI, calibration, stack position, and the
			options from Analyze/Set Measurements determine what each new row means:

			- With no ROI, a measurement generally describes the active image plane.
			- With an area ROI, it describes pixels inside that selection.
			- Line, point, and angle ROIs use specialized measurement behavior.
			- ROI Manager measurement can apply selected ROIs to an image or stack and
			  usually contributes one or more rows per ROI or position.

			The selected measurement options control the headings and columns, so do not
			assume that every table has `Area`, `Mean`, or the same set of columns. Labels,
			stack positions, and other provenance columns appear only when their options or
			the calling script requests them. Calibration can affect units and values.

			Measurements commonly append to the shared table. Before starting a new logical
			run, either reset the table deliberately or record the initial row count and
			track the rows added by the run. Never assume that every existing row came from
			the current image, ROI, or measurement settings.

			## Creating and reading tables

			`ResultsTable.getResultsTable()` refers to ImageJ's default measurement table.
			A separate `new ResultsTable()` creates an independent in-memory table. To build
			one programmatically, add a row with `incrementCounter()` or `addRow()`, then
			write values with `addValue("Column", value)` and labels with `addLabel(...)`.
			`addValue` writes to the last row, so add the row before adding its values.

			The default table is shared application state, and a displayed table is not a
			provenance record. Before interpreting results, verify the active image and
			position, ROI or ROI Manager state, calibration, measurement options, table
			headings, and row range. Treat missing or non-finite values as distinct from
			zero, and preserve image, ROI, and processing metadata when results need to be
			compared or reproduced.

			## `ResultsTableToolPlugin`

			The Fiji-LLM Results tools are currently read-only. `fiji_results_read` reads
			ImageJ's default Results Table through `ImageJ1HelperService` and reports:

			- whether a table is present;
			- the current row count and actual non-empty column headings; and
			- each row's numeric values, text values, and row labels when available.

			It does not measure an image, clear or modify the table, identify the source
			image or ROI for a row, or read every independently created or displayed table.
			Use an ImageJ command or script to perform measurements or manage table state,
			then use `fiji_results_read` to inspect the resulting shared table.
			""";

	public ResultsTableGuide() {
		super(ID, "Results Table", List.of("data", "results-table"),
			Authority.PROJECT_AUTHORED, List.of(DataTypesGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
