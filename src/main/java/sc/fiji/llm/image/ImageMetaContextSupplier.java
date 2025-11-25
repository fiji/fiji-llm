/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
import sc.fiji.llm.context.ContextItemSupplier;

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

		for (ContextItem i : items) {
			System.out.println(i);
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
