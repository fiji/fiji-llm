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
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import sc.fiji.llm.chat.ContextItem;
import sc.fiji.llm.chat.ContextItemSupplier;

/**
 * ContextItemSupplier implementation for {@link ImageMetaContextItem}s.
 * Provides available images/datasets from the Fiji application and creates
 * context items. Uses ImageDisplayService which properly handles both ImageJ1
 * and ImageJ2 images.
 */
@Plugin(type = ContextItemSupplier.class, priority = Priority.LOW)
public class ImageMetaContextSupplier implements ContextItemSupplier {

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter(required = false)
	private StatusService statusService;

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

		try {
			if (imageDisplayService == null) {
				return items;
			}

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
					if (datasetView != null) {
						final Dataset dataset = datasetView.getData();
						if (dataset != null) {
							final ContextItem item = createImageContextItem(dataset);
							if (item != null) {
								items.add(item);
							}
						}
					}
				}
				catch (Exception e) {
					// Skip this display if we can't create a context item for it
					if (statusService != null) {
						statusService.warn(
							"Could not create image metadata context item: " + e
								.getMessage());
					}
				}
			}
		}
		catch (Exception e) {
			// If we can't access images, return empty list
			if (statusService != null) {
				statusService.warn("Could not list available images: " + e
					.getMessage());
			}
		}

		return items;
	}

	@Override
	public ContextItem createActiveContextItem() {
		try {
			if (imageDisplayService == null) {
				return null;
			}

			// Get the active dataset view (automatically handles ImageJ1 to ImageJ2
			// conversion)
			final DatasetView datasetView = imageDisplayService
				.getActiveDatasetView();
			if (datasetView == null) {
				return null;
			}

			final Dataset dataset = datasetView.getData();
			if (dataset == null) {
				return null;
			}

			return createImageContextItem(dataset);
		}
		catch (RuntimeException e) {
			if (statusService != null) {
				statusService.warn("Could not create context item from active image: " +
					e.getMessage());
			}
			return null;
		}
	}

	/**
	 * Creates an {@link ImageMetaContextItem} from a Dataset. Extracts metadata
	 * and creates a descriptive text for the LLM.
	 */
	private ImageMetaContextItem createImageContextItem(final Dataset dataset) {
		if (dataset == null) {
			return null;
		}

		final String imageName = dataset.getName();
		if (imageName == null || imageName.isEmpty()) {
			return null;
		}

		// Extract all dimensions with their types and lengths
		final List<ImageMetaContextItem.Dimension> dimensions = extractDimensions(
			dataset);
		final String pixelType = dataset.getType().getClass().getSimpleName();

		return new ImageMetaContextItem(imageName, dimensions, pixelType);
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
