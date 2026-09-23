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

/** Guidance for regions of interest in Fiji. */
@Plugin(type = AgentGuide.class)
public class RoisGuide extends AbstractAgentGuide {

	public static final String ID = "rois";

	private static final String CONTENT = """
			# ImageJ 1.x Regions of Interest

			`ij.gui.Roi` is ImageJ 1.x's selection and geometry object. It can represent
			an area, line, point, angle, or composite selection. A ROI is not an image or a
			mask: its geometry is expressed in image pixel coordinates, while calibration
			affects how measurements are interpreted. Do not confuse `ij.gui.Roi` with the
			newer ImageJ2 or ImgLib2 ROI types.

			## Creating and adding ROIs

			- Fiji's selection tools create the current ROI on an `ImagePlus`; code can do
			  the same with constructors such as `new Roi(x, y, width, height)`,
			  `OvalRoi`, `PolygonRoi`, `Line`, or `PointRoi`.
			- The current image ROI is separate from the ROI Manager list. Set or inspect it
			  with `imp.setRoi(roi)` and `imp.getRoi()`.
			- Add a ROI to the existing manager with
			  `RoiManager.getInstance().addRoi(roi)`. `getInstance()` may return `null` if
			  the manager does not exist, so do not assume that a manager is open.
			- `addRoi` stores a clone. Editing the original ROI after adding it does not
			  update the manager entry; update or replace the entry explicitly instead.

			## ROI Manager semantics

			The ImageJ 1.x `RoiManager` is a shared, application-level construct rather
			than state owned by one `ImagePlus`. Its list can therefore be used to select
			or measure the same geometry on multiple open images. This is convenient for
			comparing the same region across differently processed versions of an image.

			That convenience requires care. A ROI's pixel coordinates are not semantic
			landmarks: reuse is meaningful only when the images have compatible dimensions,
			registration, orientation, and pixel grids. Cropping, scaling, rotation, or
			misregistration can make a reused ROI select the wrong region. Calibration and
			processing differences can also change the meaning of the measurements.

			ROIs can carry an optional `ImagePlus` association and a stack or hyperstack
			position. When adding a current ROI from a stack, the manager records the
			current position if the ROI has no position. Check the ROI's C/Z/T position
			before reusing it; position zero generally means that it applies to all stack
			positions in overlays and ROI Manager Show All mode.

			The manager is shared, so always verify the target image, dimensions,
			calibration, current position, and ROI geometry before measuring or modifying
			anything. Manager indices are zero-based and can change when entries are added
			or removed; re-read the manager before relying on an index.

			## `RoiManagerToolPlugin`

			The Fiji-LLM ROI tools are currently read-only:

			- `fiji_rois_read` reports whether the manager exists, its count, and each
			  entry's index, name, selection state, type, and bounds.
			- `fiji_rois_read_details` returns the exact shape, bounds, and polygon
			  coordinates for one entry. Call `fiji_rois_read` first and use its current
			  zero-based index.

			Use an ImageJ command or script for ROI creation, addition, selection, or
			measurement; use these tools to inspect the resulting shared state and verify
			that the intended ROI and image are involved.
			""";

	public RoisGuide() {
		super(ID, "Regions of Interest", List.of("data", "rois"),
			Authority.PROJECT_AUTHORED, List.of(DataTypesGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
