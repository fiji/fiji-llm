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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.LUT;
import net.imagej.axis.Axes;
import net.imglib2.display.ColorTable8;
import sc.fiji.llm.data.ImageJ1HelperService;

/** Default ImageJ-backed implementation of {@link ImageRenderingService}. */
@Plugin(type = Service.class, priority = Priority.VERY_HIGH)
public final class DefaultImageRenderingService extends AbstractService implements
	ImageRenderingService
{

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	@Override
	public Optional<RenderedImageResult> renderActiveImage(
		final ImageRenderOptions options) throws IOException
	{
		final Optional<ImagePlus> image = imageJ1HelperService.getActiveImage();
		return image.isEmpty() ? Optional.empty() : render(image.get(), options);
	}

	@Override
	public Optional<RenderedImageResult> render(final int imageId,
		final ImageRenderOptions options) throws IOException
	{
		final Optional<ImagePlus> image = imageJ1HelperService.getImage(imageId);
		return image.isEmpty() ? Optional.empty() : render(image.get(), options);
	}

	private Optional<RenderedImageResult> render(final ImagePlus image,
		final ImageRenderOptions requestedOptions) throws IOException
	{
		final ImageRenderOptions options = requestedOptions == null ?
			new ImageRenderOptions() : requestedOptions;
		final Optional<BufferedImage> flattened = options.isIncludeOverlays() ?
			flatten(image) : Optional.empty();
		final BufferedImage rendered = flattened.orElseGet(image::getBufferedImage);
		if (rendered == null) return Optional.empty();
		final BufferedImage source = copyImage(rendered);
		final String renderMode = flattened.isPresent() ? ImageRenderMetadata
			.FLATTENED_RENDER_MODE : ImageRenderMetadata.SCREEN_RENDER_MODE;
		final Roi roi = options.isIncludeRoi() ? image.getRoi() : null;
		final boolean roiIncluded = roi != null;
		if (roiIncluded && flattened.isEmpty()) drawRoi(source, roi);
		final Overlay overlay = image.getOverlay();
		final int overlayCount = overlay == null ? 0 : overlay.size();
		final boolean overlayIncluded = options.isIncludeOverlays() && flattened
			.isPresent() && overlayCount > 0 && !image.getHideOverlay();
		final String roiType = roiIncluded ? roi.getClass().getName() : "";

		final BufferedImage bounded = bound(source, options.getMaxDimension());
		final byte[] pngBytes = encodePng(bounded);
		final ImageRenderMetadata metadata = createMetadata(image, source.getWidth(),
			source.getHeight(), bounded.getWidth(), bounded.getHeight(), roiIncluded,
			renderMode, roiType, overlayIncluded, overlayCount);
		return Optional.of(new RenderedImageResult(pngBytes, metadata));
	}

	private static Optional<BufferedImage> flatten(final ImagePlus image) {
		try {
			return Optional.ofNullable(image.flatten().getBufferedImage());
		}
		catch (final RuntimeException e) {
			return Optional.empty();
		}
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

	private static void drawRoi(final BufferedImage image, final Roi roi) {
		final Graphics2D graphics = image.createGraphics();
		try {
			roi.draw(graphics);
		}
		finally {
			graphics.dispose();
		}
	}

	private static ImageRenderMetadata createMetadata(final ImagePlus image,
		final int sourceWidth, final int sourceHeight, final int renderedWidth,
		final int renderedHeight, final boolean roiIncluded, final String renderMode,
		final String roiType, final boolean overlayIncluded, final int overlayCount)
	{
		final long channelIndex = image.getNChannels() > 1 ? image.getC() - 1 : -1;
		return new ImageRenderMetadata(image.getTitle(), image.getID(), sourceWidth,
			sourceHeight, renderedWidth, renderedHeight, createPlanePosition(image),
			channelIndex, image.getNChannels(), colorMode(image), createChannelMetadata(
				image), roiIncluded, roiType, overlayIncluded, overlayCount, renderMode);
	}

	private static Map<String, Long> createPlanePosition(final ImagePlus image) {
		final Map<String, Long> result = new LinkedHashMap<>();
		if (image.getNChannels() > 1) result.put(Axes.CHANNEL.getLabel(), (long) image
			.getC() - 1);
		if (image.getNSlices() > 1) result.put(Axes.Z.getLabel(), (long) image.getZ() -
			1);
		if (image.getNFrames() > 1) result.put(Axes.TIME.getLabel(), (long) image
			.getT() - 1);
		return result;
	}

	private static String colorMode(final ImagePlus image) {
		if (!(image instanceof CompositeImage composite)) return "COLOR";
		switch (composite.getMode()) {
			case IJ.COMPOSITE:
				return "COMPOSITE";
			case IJ.GRAYSCALE:
				return "GRAYSCALE";
			default:
				return "COLOR";
		}
	}

	/** Describes only the displayed channels, which are few even for huge stacks. */
	private static List<ImageRenderMetadata.ChannelMetadata> createChannelMetadata(
		final ImagePlus image)
	{
		final List<ImageRenderMetadata.ChannelMetadata> channels = new ArrayList<>();
		if (image instanceof CompositeImage composite && composite
			.getMode() == IJ.COMPOSITE)
		{
			final boolean[] active = composite.getActiveChannels();
			for (int channel = 0; channel < composite.getNChannels(); channel++) {
				if (channel >= active.length || !active[channel]) continue;
				final LUT lut = composite.getChannelLut(channel + 1);
				channels.add(new ImageRenderMetadata.ChannelMetadata(channel, lut.min,
					lut.max, lutMetadata(lut)));
			}
			return channels;
		}
		final LUT lut = image.getProcessor().getLut();
		channels.add(new ImageRenderMetadata.ChannelMetadata(Math.max(0, image.getC() -
			1), image.getDisplayRangeMin(), image.getDisplayRangeMax(), lut == null ? null
				: lutMetadata(lut)));
		return channels;
	}

	private static ImageRenderMetadata.LutMetadata lutMetadata(final LUT lut) {
		final int size = lut.getMapSize();
		final byte[] reds = new byte[size];
		final byte[] greens = new byte[size];
		final byte[] blues = new byte[size];
		lut.getReds(reds);
		lut.getGreens(greens);
		lut.getBlues(blues);
		return new ImageRenderMetadata.LutMetadata(new ColorTable8(reds, greens,
			blues));
	}
}
