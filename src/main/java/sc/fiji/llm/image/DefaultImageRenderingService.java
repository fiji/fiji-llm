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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.display.ColorTable;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import sc.fiji.llm.data.ImageJ1HelperService;

/** Default ImageJ-backed implementation of {@link ImageRenderingService}. */
@Plugin(type = Service.class, priority = Priority.VERY_HIGH)
public final class DefaultImageRenderingService extends AbstractService implements
	ImageRenderingService
{

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	@Override
	public Optional<RenderedImageResult> renderActiveImage(
		final ImageRenderOptions options) throws IOException
	{
		return render(imageDisplayService.getActiveImageDisplay(), options);
	}

	@Override
	public Optional<RenderedImageResult> render(final ImageDisplay display,
		final ImageRenderOptions requestedOptions) throws IOException
	{
		if (display == null) return Optional.empty();
		final ImageRenderOptions options = requestedOptions == null ?
			new ImageRenderOptions() : requestedOptions;
		final DatasetView view = imageDisplayService.getActiveDatasetView(display);
		if (view == null) return Optional.empty();
		final ARGBScreenImage screenImage = view.getScreenImage();
		if (screenImage == null || screenImage.image() == null) return Optional.empty();

		final Optional<BufferedImage> flattened = options.isIncludeOverlays() ?
			imageJ1HelperService.getFlattenedImage(display) : Optional.empty();
		final BufferedImage source = flattened.map(DefaultImageRenderingService::copyImage)
			.orElseGet(() -> copyImage(screenImage.image()));
		final Optional<Object> roi = options.isIncludeRoi() ? imageJ1HelperService
			.getRoi(display) : Optional.empty();
		final boolean roiIncluded = roi.isPresent();
		if (roiIncluded && flattened.isEmpty()) drawRoi(source, roi.get());
		final int overlayCount = imageJ1HelperService.getOverlayCount(display);
		final boolean overlayIncluded = options.isIncludeOverlays() && flattened
			.isPresent() && overlayCount > 0 && !imageJ1HelperService.isOverlayHidden(
				display);
		final String roiType = roiIncluded ? roi.get().getClass().getName() : "";

		final BufferedImage bounded = bound(source, options.getMaxDimension());
		final byte[] pngBytes = encodePng(bounded);
		final ImageRenderMetadata metadata = createMetadata(display, view,
			source.getWidth(), source.getHeight(), bounded.getWidth(), bounded.getHeight(),
			roiIncluded, roiType, overlayIncluded, overlayCount, options);
		return Optional.of(new RenderedImageResult(pngBytes, metadata));
	}

	static BufferedImage copyImage(final BufferedImage source) {
		final BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(),
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = copy.createGraphics();
		try {
			graphics.drawImage(source, 0, 0, null);
		}
		finally {
			graphics.dispose();
		}
		return copy;
	}

	static BufferedImage bound(final BufferedImage source, final int maxDimension) {
		if (maxDimension < 1) {
			throw new IllegalArgumentException("maxDimension must be positive");
		}
		final int largestDimension = Math.max(source.getWidth(), source.getHeight());
		if (largestDimension <= maxDimension) return source;

		final double scale = maxDimension / (double) largestDimension;
		final int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
		final int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
		final BufferedImage bounded = new BufferedImage(width, height,
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = bounded.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			graphics.drawImage(source, 0, 0, width, height, null);
		}
		finally {
			graphics.dispose();
		}
		return bounded;
	}

	static byte[] encodePng(final BufferedImage image) throws IOException {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (!ImageIO.write(image, "png", output)) {
			throw new IOException("No PNG writer is available");
		}
		return output.toByteArray();
	}

	private static void drawRoi(final BufferedImage image, final Object roi) {
		final Graphics2D graphics = image.createGraphics();
		try {
			final Method draw = roi.getClass().getMethod("draw", Graphics.class);
			draw.invoke(roi, graphics);
		}
		catch (final ReflectiveOperationException e) {
			throw new IllegalStateException("Could not draw ROI", e);
		}
		finally {
			graphics.dispose();
		}
	}

	private ImageRenderMetadata createMetadata(final ImageDisplay display,
		final DatasetView view, final int sourceWidth, final int sourceHeight,
		final int renderedWidth, final int renderedHeight, final boolean roiIncluded,
		final String roiType, final boolean overlayIncluded, final int overlayCount,
		final ImageRenderOptions options)
	{
		final Dataset dataset = view.getData();
		final Map<String, Long> planePosition = createPlanePosition(view, dataset);
		final long channelIndex = planePosition.getOrDefault(Axes.CHANNEL.getLabel(), -1L);
		final List<ImageRenderMetadata.ChannelMetadata> channels = createChannelMetadata(
			view);
		final String colorMode = view.getColorMode() == null ? "" : view.getColorMode()
			.name();
		final int imageId = imageJ1HelperService.getImageId(display);
		return new ImageRenderMetadata(imageJ1HelperService.getImageTitle(imageId),
			imageId, sourceWidth, sourceHeight, renderedWidth, renderedHeight,
			planePosition, channelIndex, view.getChannelCount(), colorMode, channels,
			roiIncluded, roiType, overlayIncluded, overlayCount, options
				.isIncludeOverlays() ? "annotated" : "plain");
	}

	private static Map<String, Long> createPlanePosition(final DatasetView view,
		final Dataset dataset)
	{
		final Position position = view.getPlanePosition();
		if (position == null || dataset == null) return Collections.emptyMap();
		final long[] values = new long[position.numDimensions()];
		position.localize(values);
		final Map<String, Long> result = new LinkedHashMap<>();
		int positionDimension = 0;
		for (int datasetDimension = 0; datasetDimension < dataset.numDimensions();
			datasetDimension++)
		{
			final AxisType axisType = dataset.axis(datasetDimension).type();
			if (Axes.X.equals(axisType) || Axes.Y.equals(axisType)) continue;
			if (positionDimension >= values.length) break;
			final String label = axisType == null ? "Dim" + datasetDimension : axisType
				.getLabel();
			result.put(label, values[positionDimension++]);
		}
		return result;
	}

	private static List<ImageRenderMetadata.ChannelMetadata> createChannelMetadata(
		final DatasetView view)
	{
		final List<ColorTable> colorTables = view.getColorTables();
		final List<ImageRenderMetadata.ChannelMetadata> channels = new ArrayList<>();
		for (int channel = 0; channel < view.getChannelCount(); channel++) {
			final ColorTable colorTable = channel < colorTables.size() ? colorTables.get(
				channel) : null;
			final ImageRenderMetadata.LutMetadata lut = colorTable == null ? null
				: new ImageRenderMetadata.LutMetadata(colorTable);
			channels.add(new ImageRenderMetadata.ChannelMetadata(channel, view
				.getChannelMin(channel), view.getChannelMax(channel), lut));
		}
		return channels;
	}
}
