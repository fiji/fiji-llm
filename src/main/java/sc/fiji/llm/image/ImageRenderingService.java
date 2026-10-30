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

package sc.fiji.llm.image;

import java.io.IOException;
import java.util.Optional;

import net.imagej.ImageJService;

/** Renders open ImageJ images into bounded multimodal PNG content. */
public interface ImageRenderingService extends ImageJService {

	/** Preference key controlling image pixels sent to hosted models. */
	String HOSTED_IMAGE_ACCESS_KEY = "hostedImageAccess";

	/** Default value for {@link #HOSTED_IMAGE_ACCESS_KEY}. */
	boolean DEFAULT_HOSTED_IMAGE_ACCESS = true;

	/** Error returned when pixel-level image sharing is disabled. */
	String PIXEL_SHARING_DISABLED_MESSAGE =
		"Pixel-level image sharing is disabled.";

	/**
	 * Reports whether the current assistant may receive image pixels.
	 *
	 * @return true for local models or when hosted image access is enabled
	 */
	boolean isPixelSharingAllowed();

	/**
	 * Records whether the current assistant uses a local model.
	 *
	 * @param localModel whether the active model processes data locally
	 */
	void setLocalModel(boolean localModel);

	Optional<RenderedImageResult> renderActiveImage(ImageRenderOptions options)
		throws IOException;

	Optional<RenderedImageResult> render(int imageId, ImageRenderOptions options)
		throws IOException;

	/**
	 * Returns image and display metadata without rendering or sharing pixel data.
	 *
	 * @param imageId ID from the open-image list
	 * @return metadata when the image is open
	 */
	Optional<ImageRenderMetadata> getImageMetadata(int imageId);
}
