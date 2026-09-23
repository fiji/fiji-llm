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

/** Guidance for image data in Fiji. */
@Plugin(type = AgentGuide.class)
public class ImagesInFiji extends AbstractAgentGuide {

	public static final String ID = "image-types";

	private static final String CONTENT = """
			# Image Data in Fiji

			Fiji has both a legacy ImageJ 1.x data model and the modern ImageJ2/ImgLib2
			data model. `ImagePlus`, `ImgPlus`, and `Dataset` are related, but they are not
			interchangeable and should not be treated as three independent copies of an
			image. Use the existing SciJava context and image services to find the current
			image and to move between representations.

			## `ij.ImagePlus` (ImageJ 1.x)

			`ImagePlus` is ImageJ 1.x's image, stack, calibration, ROI, and legacy UI handle.
			Its dimensional model is fixed to five slots: X, Y, channel, slice, and frame.
			`getDimensions()` returns these as an `int[5]`, and the channel/slice/frame
			product must describe the image stack. It is therefore a 2D-to-5D model, not a
			general N-dimensional model.

			An `ImageStack` stores the planes as Java pixel arrays and exposes one current
			plane through an `ImageProcessor`. This is convenient and fast for ImageJ 1.x
			commands and plugins, but each plane and the stack bookkeeping are subject to
			Java array and integer-index limits. `ImagePlus` has no native notion of
			chunked, lazy, or dynamically cached image storage.

			Use `ImagePlus` when interacting with an ImageJ 1.x API, such as legacy commands,
			`ij.gui.Roi`, `RoiManager`, or `ResultsTable`. Do not assume that an `ImagePlus`
			is the best representation for new N-dimensional image algorithms.

			### Virtual `ImagePlus`

			An `ImagePlus` whose stack reports `isVirtual()` is backed by a virtual stack,
			usually reading planes on demand from files or another source instead of keeping
			all pixels in memory. Accessing a plane can therefore perform I/O, and the
			`ImageProcessor` used for one slice may be reused or replaced when another slice
			is selected. Do not retain a processor or pixel array across slice changes.

			For an ordinary ImageJ 1.x `VirtualStack`, `getImageArray()` is null and
			`setPixels(...)` does nothing. Do not assume that modifying the current processor
			will persist to the source or that a virtual image is writable merely because it
			is an `ImagePlus`. Check the concrete stack and operation, and materialize or
			copy the data into an explicitly writable representation when necessary.

			The legacy bridge can wrap an `ImagePlus` or Dataset in a virtual-stack adapter,
			so conversion may remain lazy rather than materializing the whole image. This is
			useful for large data, but it means that pixel access, copying, and algorithms
			that require random writable storage can have surprising cost or limitations.

			## `net.imagej.ImgPlus` (ImageJ2 and ImgLib2)

			`ImgPlus<T>` is an N-dimensional ImgLib2 `Img<T>` together with image metadata,
			including a name, typed axes, calibration, and related display metadata. Its
			dimension lengths are `long` values, and its axes are not limited to the
			ImageJ 1.x X/Y/channel/slice/frame convention.

			`ImgPlus` delegates pixel storage and access to its backing `Img`. The backing
			container determines the performance and memory behavior. Depending on the
			factory and data source, it may be an `ArrayImg` with one linear native array,
			a `PlanarImg` with plane arrays, or a cell-backed or cached image such as
			`CellImg`. Array storage can be very fast but has Java array limits; planar and
			cell-backed storage can make large, sparse, lazy, or out-of-core workflows
			possible. Do not assume that every `ImgPlus` is contiguous or cheap to copy.

			## `net.imagej.Dataset` (ImageJ2)

			`Dataset` is ImageJ's primary application-level image data structure. It wraps
			an `ImgPlus<? extends RealType<?>>` and adds ImageJ metadata and behavior such
			as typed pixel information, calibration and axes, dirty state, RGB handling,
			and plane-oriented convenience methods. It is itself an ImgLib2 image, so
			N-dimensional algorithms can usually operate on the Dataset without extracting
			its backing `ImgPlus`.

			A Dataset is not automatically a copy of its `ImgPlus`, and converting between
			representations may either share data or copy it depending on the bridge and
			storage. `getPlane(plane, false)` can return no plane when the backing container
			cannot expose one directly; requesting a copy can be expensive. A multiscale or
			pyramid workflow is also not implied by `Dataset` alone; it requires a suitable
			data source, container, view, and metadata.

			## Choosing and inspecting a representation

			- Use `Dataset` for Fiji application data and modern ImageJ algorithms.
			- Use `ImgPlus` when the algorithm needs direct N-dimensional ImgLib2 access,
				storage-aware behavior, or axis metadata.
			- Use `ImagePlus` when a legacy ImageJ 1.x API requires it.
			- Never assume that dimension index 2 is always Z or that a dataset is only
				X/Y/Z/C/T. Inspect `numDimensions()`, `dimension(i)`, and `axis(i).type()` on
				Dataset or `ImgPlus`; use the explicit channel/slice/frame methods on
				`ImagePlus`.
			- For images already open in Fiji, use `ImageDisplayService` and its
				`DatasetView` to identify the active display and Dataset. This service bridges
				ImageJ 1.x and ImageJ2 displays through `LegacyService`; do not create a second
				context or a parallel image merely to obtain a different representation.
			- The Fiji-LLM image tools identify open images by their Fiji image id, then
				inspect the corresponding display and Dataset. Use `fiji_image_list` before
				`fiji_image_details`, `fiji_image_view`, or
				`fiji_image_view_annotated` when an image id is needed.

			When moving between models, preserve and verify dimensions, axis types, pixel
			type, calibration, current position, virtual/lazy status, and whether data was
			shared or copied. Automatic parameter conversion makes APIs convenient, but it
			does not remove the need to understand synchronization and storage behavior.
			A displayed image, an ImagePlus, a Dataset, and an ImgPlus may be different
			views or bridges over related state; object class alone is not enough to infer
			identity, ownership, or storage cost.
			""".strip();

	public ImagesInFiji() {
		super(ID, "Image Data in Fiji", AgentGuide.topics(Topic.DATA, Topic.IMAGES,
			Topic.IMAGEPLUS, Topic.DATASET, Topic.IMGPLUS),
			Authority.PROJECT_AUTHORED, List.of(DataTypesGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
