package sc.fiji.llm.image;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

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
 * ContextItemSupplier implementation for image context items.
 * Provides available images/datasets from the Fiji application and creates ImageContextItem objects.
 *
 * Uses ImageDisplayService which properly handles both ImageJ1 and ImageJ2 images.
 */
@Plugin(type = ContextItemSupplier.class)
public class ImageContextSupplier implements ContextItemSupplier {

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter(required = false)
	private StatusService statusService;	@Override
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
	public List<ContextItem> listAvailable() {
		final List<ContextItem> items = new ArrayList<>();

		try {
			if (imageDisplayService == null) {
				return items;
			}

			// Get all image displays (handles both ImageJ1 and ImageJ2)
			final List<ImageDisplay> imageDisplays = imageDisplayService.getImageDisplays();

			if (imageDisplays == null || imageDisplays.isEmpty()) {
				return items;
			}

			for (final ImageDisplay imageDisplay : imageDisplays) {
				try {
					// Get the active DatasetView from the ImageDisplay
					final DatasetView datasetView = imageDisplayService.getActiveDatasetView(imageDisplay);
					if (datasetView != null) {
						final Dataset dataset = datasetView.getData();
						if (dataset != null) {
							final ContextItem item = createImageContextItem(dataset);
							if (item != null) {
								items.add(item);
							}
						}
					}
				} catch (Exception e) {
					// Skip this display if we can't create a context item for it
					if (statusService != null) {
						statusService.warn("Could not create image context item: " + e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			// If we can't access images, return empty list
			if (statusService != null) {
				statusService.warn("Could not list available images: " + e.getMessage());
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

			// Get the active dataset view (automatically handles ImageJ1 to ImageJ2 conversion)
			final DatasetView datasetView = imageDisplayService.getActiveDatasetView();
			if (datasetView == null) {
				return null;
			}

			final Dataset dataset = datasetView.getData();
			if (dataset == null) {
				return null;
			}

			return createImageContextItem(dataset);
		} catch (RuntimeException e) {
			if (statusService != null) {
				statusService.warn("Could not create active image context item: " + e.getMessage());
			}
			return null;
		}
	}

	/**
	 * Creates an ImageContextItem from a Dataset.
	 * Extracts metadata and creates a descriptive text for the LLM.
	 */
	private ImageContextItem createImageContextItem(final Dataset dataset) {
		if (dataset == null) {
			return null;
		}

		final String imageName = dataset.getName();
		if (imageName == null || imageName.isEmpty()) {
			return null;
		}

		// Extract all dimensions with their types and lengths
		final List<ImageContextItem.Dimension> dimensions = extractDimensions(dataset);
		final String pixelType = dataset.getType().getClass().getSimpleName();

		return new ImageContextItem(imageName, dimensions, pixelType);
	}

	/**
	 * Extracts all dimensions from a dataset with their types and lengths.
	 */
	private List<ImageContextItem.Dimension> extractDimensions(final Dataset dataset) {
		final List<ImageContextItem.Dimension> dimensions = new ArrayList<>();

		final int numDims = dataset.numDimensions();
		for (int i = 0; i < numDims; i++) {
			try {
				final AxisType axisType = dataset.axis(i).type();
				final String type = axisType != null ? axisType.getLabel() : "Unknown";
				final long length = dataset.dimension(i);
				dimensions.add(new ImageContextItem.Dimension(type, length));
			} catch (Exception e) {
				// If we can't get axis type, use a generic label
				final long length = dataset.dimension(i);
				dimensions.add(new ImageContextItem.Dimension("Dim" + i, length));
			}
		}

		return dimensions;
	}
}
