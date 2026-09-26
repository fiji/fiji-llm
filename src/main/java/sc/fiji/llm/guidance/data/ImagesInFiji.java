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
			directly interchangeable.

			## `ij.ImagePlus` (ImageJ 1.x)

			`ImagePlus` is ImageJ 1.x's image, stack, calibration, ROI, and legacy UI handle.
			Its dimensional model is fixed to five slots: X, Y, channel, slice, and frame.
			`getDimensions()` returns these as an `int[5]`, and the channel/slice/frame
			product must describe the image stack.

			An `ImageStack` stores the planes as Java pixel arrays and exposes one current
			plane through an `ImageProcessor`. This is convenient and fast for ImageJ 1.x
			commands and plugins, but each plane and the stack bookkeeping are subject to
			Java array and integer-index limits.

			### Virtual `ImagePlus`

			An `ImagePlus` who reports `isVirtual()` is backed by a virtual stack,
			usually reading planes on demand from files or another source instead of keeping
			all pixels in memory. Accessing a plane can therefore perform I/O, and the
			`ImageProcessor` used for one slice may be reused or replaced when another slice
			is selected. Do not retain a processor or pixel array across slice changes.

			## `net.imagej.ImgPlus` (ImageJ2 and ImgLib2)

			`ImgPlus` is an N-dimensional ImgLib2 `Img` together with image metadata,
			including a name, typed axes, calibration, and related display metadata. Its
			dimension lengths are `long` values, and its axes are not limited to the
			ImageJ 1.x X/Y/channel/slice/frame convention.

			`ImgPlus` delegates pixel storage and access to its backing `Img`. The backing
			container determines the performance and memory behavior. For example, this
			could be an `ArrayImg` with one linear native array, a `PlanarImg` with an
			array per plane, or a `CellImg` that caches image regions to disk.

			## `net.imagej.Dataset` (ImageJ2)

			`Dataset` is ImageJ's primary application-level image data structure. It wraps
			an `ImgPlus` and adds ImageJ metadata and behavior such as typed pixel
			information, calibration and axes, dirty state, RGB handling,
			and plane-oriented convenience methods. It is itself an ImgLib2 image, so
			N-dimensional algorithms can usually operate on the Dataset without extracting
			its backing `ImgPlus`.

			## Choosing a type

			- Use `Dataset` for Fiji application data and modern ImageJ algorithms.
			- Use `ImgPlus` when the algorithm needs direct N-dimensional ImgLib2 access,
				storage-aware behavior, or axis metadata.
			- Use `ImagePlus` when a legacy ImageJ 1.x API requires it.

			Note that image class alone does not make any guarantees about backing data storage.
			Inspect further or check runtime class if such differentiation is necessary.

			## Image inspection

			- Use `fiji_image_view` to inspect base image content
			- Use `fiji_image_view_annotated` when visible ROIs or other overlays are relevant
			- Never assume that dimension index 2 is always Z or that a dataset is only
				X/Y/Z/C/T. Inspect `numDimensions()`, `dimension(i)`, and `axis(i).type()` on
				Dataset or `ImgPlus`; use channel/slice/frame methods on `ImagePlus`.
			- For images already open in Fiji, use `ImageDisplayService` and its
				`DatasetView` to identify the active display and Dataset.
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
