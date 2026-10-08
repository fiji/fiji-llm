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
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import sc.fiji.llm.context.ContextItem;
import sc.fiji.llm.data.ImageJ1HelperService;
import sc.fiji.llm.ui.ContextItemSupplier;

/**
 * ContextItemSupplier implementation for {@link ImageMetaContextItem}s.
 * Provides the open ImageJ images from the Fiji application and creates
 * context items.
 */
@Plugin(type = ContextItemSupplier.class, priority = Priority.LOW)
public class ImageMetaContextSupplier implements ContextItemSupplier {

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	@Parameter
	private ImageRenderingService imageRenderingService;

	@Override
	public String getDisplayName() {
		return "Image";
	}

	protected boolean includesOverlays() {
		return false;
	}

	@Override
	public ImageIcon getIcon() {
		final URL iconUrl = getClass().getResource("/icons/image-noun-32.png");
		if (iconUrl != null) {
			return new ImageIcon(iconUrl);
		}
		return null;
	}

	@Override
	public Set<ContextItem> listAvailable() {
		final Set<ContextItem> items = new LinkedHashSet<>();
		for (final int id : imageJ1HelperService.getImageIds()) {
			try {
				final Optional<ImagePlus> image = imageJ1HelperService.getImage(id);
				if (image.isPresent()) items.add(createImageContextItem(image.get()));
			} catch (Exception e) {
			}
		}
		return items;
	}

	@Override
	public ContextItem createActiveContextItem() {
		return imageJ1HelperService.getActiveImage().map(
			this::createImageContextItem).orElse(null);
	}

	/**
	 * Creates an {@link ImageMetaContextItem} from an image. Extracts metadata
	 * and creates a descriptive text for the LLM.
	 */
	protected ImageMetaContextItem createImageContextItem(final ImagePlus image) {
		final ImgPlus<?> imgPlus = ImageJ1HelperService.wrap(image);
		final int id = image.getID();

		// Extract all dimensions with their types and lengths
		final List<ImageMetaContextItem.Dimension> dimensions = extractDimensions(
			imgPlus);
		final String pixelType = imgPlus.getImg().getType().getClass().getSimpleName();

		final RenderedImageResult rendered = renderImage(id);
		return new ImageMetaContextItem(image.getTitle(), id, dimensions, pixelType,
			rendered == null ? null : rendered.getImageContent(), rendered == null ? null :
			rendered.getMetadata(), includesOverlays());
	}

	private RenderedImageResult renderImage(final int imageId) {
		if (imageRenderingService == null) return null;
		try {
			final ImageRenderOptions options = includesOverlays() ? new ImageRenderOptions(
				ImageRenderOptions.DEFAULT_MAX_DIMENSION, true, true) :
				new ImageRenderOptions();
			return imageRenderingService.render(imageId, options).orElse(null);
		}
		catch (final IOException e) {
			return null;
		}
	}

	/**
	 * Extracts all dimensions from an image with their types and lengths.
	 */
	private List<ImageMetaContextItem.Dimension> extractDimensions(
		final ImgPlus<?> imgPlus)
	{
		final List<ImageMetaContextItem.Dimension> dimensions = new ArrayList<>();

		final int numDims = imgPlus.numDimensions();
		for (int i = 0; i < numDims; i++) {
			try {
				final AxisType axisType = imgPlus.axis(i).type();
				final String type = axisType != null ? axisType.getLabel() : "Unknown";
				final long length = imgPlus.dimension(i);
				dimensions.add(new ImageMetaContextItem.Dimension(type, length));
			}
			catch (Exception e) {
				// If we can't get axis type, use a generic label
				final long length = imgPlus.dimension(i);
				dimensions.add(new ImageMetaContextItem.Dimension("Dim" + i, length));
			}
		}

		return dimensions;
	}
}
