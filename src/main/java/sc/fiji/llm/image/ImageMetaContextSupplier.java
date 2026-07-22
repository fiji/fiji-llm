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

package sc.fiji.llm.image;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import sc.fiji.llm.context.ContextItem;
import sc.fiji.llm.ui.ContextItemSupplier;

/**
 * ContextItemSupplier implementation for {@link ImageMetaContextItem}s.
 * Provides available images/datasets from the Fiji application and creates
 * context items. Uses ImageDisplayService which properly handles both ImageJ1
 * and ImageJ2 images.
 */
@Plugin(type = ContextItemSupplier.class, priority = Priority.LOW)
public class ImageMetaContextSupplier implements ContextItemSupplier {

	@Parameter
	private ImagePlusHelper iPlusHelper;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Override
	public String getDisplayName() {
		return "Image";
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

		// Get all image displays (handles both ImageJ1 and ImageJ2)
		final List<ImageDisplay> imageDisplays = imageDisplayService
				.getImageDisplays();

		if (imageDisplays == null || imageDisplays.isEmpty()) {
				return items;
		}

		for (final ImageDisplay imageDisplay : imageDisplays) {
			try {
					// Get the active DatasetView from the ImageDisplay
					final DatasetView datasetView = imageDisplayService
							.getActiveDatasetView(imageDisplay);
					if (datasetView == null) {
						continue;
					}

					final Dataset dataset = datasetView.getData();
					if (dataset == null) {
						continue;
					}

					int id = iPlusHelper.getId(imageDisplay);
					items.add(createImageContextItem(dataset, id));
			} catch (Exception e) {
			}
		}
		return items;
	}

	@Override
	public ContextItem createActiveContextItem() {
		// Get the active dataset view (automatically handles ImageJ1 to ImageJ2
		// conversion)
		final ImageDisplay display = imageDisplayService.getActiveImageDisplay();
		if (display == null) {
			return null;
		}

		final DatasetView datasetView = imageDisplayService.getActiveDatasetView(display);
		if (datasetView == null) {
				return null;
		}

		final Dataset dataset = datasetView.getData();
		if (dataset == null) {
				return null;
		}

		int id = iPlusHelper.getId(display);
		return createImageContextItem(dataset, id);
	}

	/**
	 * Creates an {@link ImageMetaContextItem} from a Dataset. Extracts metadata
	 * and creates a descriptive text for the LLM.
	 */
	private ImageMetaContextItem createImageContextItem(final Dataset dataset, final int id) {
		if (dataset == null) {
			return null;
		}

		String imageTitle = iPlusHelper.getTitle(id);

		// Extract all dimensions with their types and lengths
		final List<ImageMetaContextItem.Dimension> dimensions = extractDimensions(
			dataset);
		final String pixelType = dataset.getType().getClass().getSimpleName();

		return new ImageMetaContextItem(imageTitle, id, dimensions, pixelType);
	}

	/**
	 * Extracts all dimensions from a dataset with their types and lengths.
	 */
	private List<ImageMetaContextItem.Dimension> extractDimensions(
		final Dataset dataset)
	{
		final List<ImageMetaContextItem.Dimension> dimensions = new ArrayList<>();

		final int numDims = dataset.numDimensions();
		for (int i = 0; i < numDims; i++) {
			try {
				final AxisType axisType = dataset.axis(i).type();
				final String type = axisType != null ? axisType.getLabel() : "Unknown";
				final long length = dataset.dimension(i);
				dimensions.add(new ImageMetaContextItem.Dimension(type, length));
			}
			catch (Exception e) {
				// If we can't get axis type, use a generic label
				final long length = dataset.dimension(i);
				dimensions.add(new ImageMetaContextItem.Dimension("Dim" + i, length));
			}
		}

		return dimensions;
	}
}
