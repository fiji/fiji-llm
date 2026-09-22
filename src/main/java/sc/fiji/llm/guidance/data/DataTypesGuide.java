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

/** Guidance for common Fiji data types. */
@Plugin(type = AgentGuide.class)
public class DataTypesGuide extends AbstractAgentGuide {

	private static final String CONTENT = """
			# Fiji Data Types: Choose a Guide

			Find the article that matches the data or behavior you need to understand:

			- **Image pixels and image representations:** Read Guide ID `images-and-datasets`
			  for `ImagePlus`, `ImgPlus`, `Dataset`, dimensions, axes, storage, virtual
			  images, and legacy/modern image conversion.
			- **Fiji runtime and service behavior:** Read Guide ID `services-and-context`
			  for the SciJava `Context`, injected services, displays, UI behavior, and
			  `LegacyService` interoperability.
			- **Selection geometry:** Read Guide ID `rois` for `Roi`, current-image selections,
			  `RoiManager`, positions, and reuse across images.
			- **Measurement and tabular output:** Read Guide ID `results-table` for
			  `ResultsTable`, `Analyzer`, measurement provenance, rows, columns, and shared
			  table state.

			Many workflows combine these concerns. For example, interpreting a measurement
			may require the image, its ROI, the Results Table, and the services that connect
			legacy ImageJ state to modern Fiji state. Read the image article first when the
			image representation is unclear, then add the article for each state or data type
			that the operation uses.
			""";

	public DataTypesGuide() {
		super("data-types", "Fiji Data Types", List.of("data-types",
			"data", "images", "rois", "results-table"), Authority.PROJECT_AUTHORED, List.of(
				"rois", "results-table", "images-and-datasets", "services-and-context",
				"data-types"));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
