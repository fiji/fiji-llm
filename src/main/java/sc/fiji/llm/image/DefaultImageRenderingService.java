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
import org.scijava.prefs.PrefService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import com.google.gson.JsonObject;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.display.ColorTable8;
import sc.fiji.llm.data.ImageJ1HelperService;

/** Default ImageJ-backed implementation of {@link ImageRenderingService}. */
@Plugin(type = Service.class, priority = Priority.VERY_HIGH)
public final class DefaultImageRenderingService extends AbstractService implements
	ImageRenderingService
{

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	@Parameter
	private PrefService prefService;

	private volatile boolean localModel;

	@Override
	public boolean isPixelSharingAllowed() {
		return localModel || prefService.getBoolean(ImageRenderingService.class,
			ImageRenderingService.HOSTED_IMAGE_ACCESS_KEY,
			ImageRenderingService.DEFAULT_HOSTED_IMAGE_ACCESS);
	}

	@Override
	public void setLocalModel(final boolean localModel) {
		this.localModel = localModel;
	}

	@Override
	public Optional<ImageRenderMetadata> getImageMetadata(final int imageId) {
		final Optional<Object> image = imageJ1HelperService.getImage(imageId);
		return image.map(this::createMetadataOnly);
	}

	@Override
	public Optional<RenderedImageResult> renderActiveImage(
		final ImageRenderOptions options) throws IOException
	{
		final Optional<Object> image = imageJ1HelperService.getActiveImage();
		return image.isEmpty() ? Optional.empty() : render(image.get(), options);
	}

	@Override
	public Optional<RenderedImageResult> render(final int imageId,
		final ImageRenderOptions options) throws IOException
	{
		final Optional<Object> image = imageJ1HelperService.getImage(imageId);
		return image.isEmpty() ? Optional.empty() : render(image.get(), options);
	}

	private Optional<RenderedImageResult> render(final Object image,
		final ImageRenderOptions requestedOptions) throws IOException
	{
		requirePixelSharing(isPixelSharingAllowed());
		final ImageRenderOptions options = requestedOptions == null ?
			new ImageRenderOptions() : requestedOptions;
		final Optional<BufferedImage> flattened = options.isIncludeOverlays() ?
			flatten(image) : Optional.empty();
		BufferedImage rendered = flattened.orElse(null);
		if (rendered == null) rendered = asBufferedImage(imageJ1HelperService
			.invokeLegacyMethod(image, "getBufferedImage"));
		if (rendered == null) return Optional.empty();
		final BufferedImage source = copyImage(rendered);
		final String renderMode = flattened.isPresent() ? ImageRenderMetadata
			.FLATTENED_RENDER_MODE : ImageRenderMetadata.SCREEN_RENDER_MODE;
		final Object roi = options.isIncludeRoi() ? imageJ1HelperService
			.invokeLegacyMethod(image, "getRoi") : null;
		final boolean roiIncluded = roi != null;
		if (roiIncluded && flattened.isEmpty()) drawRoi(source, roi);
		final Object overlay = imageJ1HelperService.invokeLegacyMethod(image,
			"getOverlay");
		final int overlayCount = asInt(imageJ1HelperService.invokeLegacyMethod(overlay,
			"size"));
		final boolean overlayIncluded = options.isIncludeOverlays() && flattened
			.isPresent() && overlayCount > 0 && !asBoolean(imageJ1HelperService
				.invokeLegacyMethod(image, "getHideOverlay"));
		final String roiType = roiIncluded ? roi.getClass().getName() : "";

		final BufferedImage bounded = bound(source, options.getMaxDimension());
		final byte[] pngBytes = encodePng(bounded);
		final ImageRenderMetadata metadata = createMetadata(image, source.getWidth(),
			source.getHeight(), bounded.getWidth(), bounded.getHeight(), roiIncluded,
			renderMode, roiType, overlayIncluded, overlayCount);
		return Optional.of(new RenderedImageResult(pngBytes, metadata));
	}

	static void requirePixelSharing(final boolean allowed) throws IOException {
		if (!allowed) throw new IOException(ImageRenderingService
			.PIXEL_SHARING_DISABLED_MESSAGE);
	}

	private Optional<BufferedImage> flatten(final Object image) {
		try {
			final Object flattened = imageJ1HelperService.invokeLegacyMethod(image,
				"flatten");
			return Optional.ofNullable(asBufferedImage(imageJ1HelperService
				.invokeLegacyMethod(flattened, "getBufferedImage")));
		}
		catch (final RuntimeException e) {
			return Optional.empty();
		}
	}

	private static BufferedImage asBufferedImage(final Object image) {
		return image instanceof BufferedImage bufferedImage ? bufferedImage : null;
	}

	private static int asInt(final Object value) {
		return value instanceof Number number ? number.intValue() : 0;
	}

	private static double asDouble(final Object value) {
		return value instanceof Number number ? number.doubleValue() : 0;
	}

	private static boolean asBoolean(final Object value) {
		return Boolean.TRUE.equals(value);
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

	private void drawRoi(final BufferedImage image, final Object roi) {
		final Graphics2D graphics = image.createGraphics();
		try {
			imageJ1HelperService.invokeLegacyMethod(roi, "draw", graphics);
		}
		finally {
			graphics.dispose();
		}
	}

	private ImageRenderMetadata createMetadata(final Object image,
		final int sourceWidth, final int sourceHeight, final int renderedWidth,
		final int renderedHeight, final boolean roiIncluded, final String renderMode,
		final String roiType, final boolean overlayIncluded, final int overlayCount)
	{
		final int channelCount = asInt(imageJ1HelperService.invokeLegacyMethod(image,
			"getNChannels"));
		final long channelIndex = Math.max(0, asInt(imageJ1HelperService
			.invokeLegacyMethod(image, "getC")) - 1);
		final int imageId = imageJ1HelperService.getImageId(image);
		final ImgPlus<?> imgPlus = ImageJ1HelperService.wrap(image);
		return new ImageRenderMetadata(imageJ1HelperService.getImageTitle(image),
			imageId, sourceWidth,
			sourceHeight, renderedWidth, renderedHeight, createPlanePosition(image),
			channelIndex, channelCount, colorMode(image), createChannelMetadata(
				image), roiIncluded, roiType, overlayIncluded, overlayCount, renderMode,
			imageJ1HelperService.isActiveImage(imageId), imgPlus.getImg().getType()
				.getClass().getSimpleName(), createDimensions(imgPlus),
			createSpatialCalibration(image), imageJ1HelperService.isLegacyInstance(
				image, "ij.CompositeImage"));
	}

	private ImageRenderMetadata createMetadataOnly(final Object image) {
		final int width = asInt(imageJ1HelperService.invokeLegacyMethod(image, "getWidth"));
		final int height = asInt(imageJ1HelperService.invokeLegacyMethod(image,
			"getHeight"));
		return createMetadata(image, width, height, width, height, false,
			ImageRenderMetadata.SCREEN_RENDER_MODE, "", false, 0);
	}

	private List<ImageRenderMetadata.AxisMetadata> createDimensions(
		final ImgPlus<?> imgPlus)
	{
		final List<ImageRenderMetadata.AxisMetadata> dimensions = new ArrayList<>();
		for (int index = 0; index < imgPlus.numDimensions(); index++) {
			String type = "Unknown";
			try {
				final AxisType axisType = imgPlus.axis(index).type();
				if (axisType != null) type = axisType.getLabel();
			}
			catch (final RuntimeException ignored) {
				type = "Dim" + index;
			}
			dimensions.add(new ImageRenderMetadata.AxisMetadata(index, type,
				imgPlus.dimension(index)));
		}
		return dimensions;
	}

	private JsonObject createSpatialCalibration(final Object image) {
		final Object calibration = imageJ1HelperService.invokeLegacyMethod(image,
			"getCalibration");
		final JsonObject result = new JsonObject();
		addNumberProperty(result, "pixel_width", imageJ1HelperService.getLegacyField(
			calibration, "pixelWidth"));
		addNumberProperty(result, "pixel_height", imageJ1HelperService.getLegacyField(
			calibration, "pixelHeight"));
		addNumberProperty(result, "pixel_depth", imageJ1HelperService.getLegacyField(
			calibration, "pixelDepth"));
		addNumberProperty(result, "x_origin", imageJ1HelperService.getLegacyField(
			calibration, "xOrigin"));
		addNumberProperty(result, "y_origin", imageJ1HelperService.getLegacyField(
			calibration, "yOrigin"));
		addNumberProperty(result, "z_origin", imageJ1HelperService.getLegacyField(
			calibration, "zOrigin"));
		addTextProperty(result, "unit", imageJ1HelperService.invokeLegacyMethod(
			calibration, "getUnit"));
		addTextProperty(result, "x_unit", imageJ1HelperService.invokeLegacyMethod(
			calibration, "getXUnit"));
		addTextProperty(result, "y_unit", imageJ1HelperService.invokeLegacyMethod(
			calibration, "getYUnit"));
		addTextProperty(result, "z_unit", imageJ1HelperService.invokeLegacyMethod(
			calibration, "getZUnit"));
		return result;
	}

	private static void addNumberProperty(final JsonObject result, final String name,
		final Object value)
	{
		if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
			result.addProperty(name, number);
		}
	}

	private static void addTextProperty(final JsonObject result, final String name,
		final Object value)
	{
		if (value instanceof String text && !text.isBlank()) result.addProperty(name, text);
	}

	private Map<String, Long> createPlanePosition(final Object image) {
		final Map<String, Long> result = new LinkedHashMap<>();
		if (asInt(imageJ1HelperService.invokeLegacyMethod(image, "getNChannels")) > 1) {
			result.put(Axes.CHANNEL.getLabel(), (long) asInt(imageJ1HelperService
				.invokeLegacyMethod(image, "getC")) - 1);
		}
		if (asInt(imageJ1HelperService.invokeLegacyMethod(image, "getNSlices")) > 1) {
			result.put(Axes.Z.getLabel(), (long) asInt(imageJ1HelperService
				.invokeLegacyMethod(image, "getZ")) - 1);
		}
		if (asInt(imageJ1HelperService.invokeLegacyMethod(image, "getNFrames")) > 1) {
			result.put(Axes.TIME.getLabel(), (long) asInt(imageJ1HelperService
				.invokeLegacyMethod(image, "getT")) - 1);
		}
		return result;
	}

	private String colorMode(final Object image) {
		if (!imageJ1HelperService.isLegacyInstance(image, "ij.CompositeImage")) {
			return "COLOR";
		}
		final int mode = asInt(imageJ1HelperService.invokeLegacyMethod(image,
			"getMode"));
		if (mode == asInt(imageJ1HelperService.getLegacyStaticField("ij.IJ",
			"COMPOSITE"))) return "COMPOSITE";
		if (mode == asInt(imageJ1HelperService.getLegacyStaticField("ij.IJ",
			"GRAYSCALE"))) return "GRAYSCALE";
		return "COLOR";
	}

	/** Describes only the displayed channels, which are few even for huge stacks. */
	private List<ImageRenderMetadata.ChannelMetadata> createChannelMetadata(
		final Object image)
	{
		final List<ImageRenderMetadata.ChannelMetadata> channels = new ArrayList<>();
		final boolean composite = imageJ1HelperService.isLegacyInstance(image,
			"ij.CompositeImage");
		final int channelCount = asInt(imageJ1HelperService.invokeLegacyMethod(image,
			"getNChannels"));
		final int currentChannel = Math.max(0, asInt(imageJ1HelperService
			.invokeLegacyMethod(image, "getC")) - 1);
		final int currentSlice = asInt(imageJ1HelperService.invokeLegacyMethod(image,
			"getZ"));
		final int currentFrame = asInt(imageJ1HelperService.invokeLegacyMethod(image,
			"getT"));
		final Object stack = imageJ1HelperService.invokeLegacyMethod(image, "getStack");
		final boolean[] activeChannels = composite ? (boolean[]) imageJ1HelperService
			.invokeLegacyMethod(image, "getActiveChannels") : null;
		for (int channel = 0; channel < channelCount; channel++) {
			final int stackIndex = asInt(imageJ1HelperService.invokeLegacyMethod(image,
				"getStackIndex", channel + 1, currentSlice, currentFrame));
			final String label = channelLabel(stack, stackIndex, channel);
			final Object processor = imageJ1HelperService.invokeLegacyMethod(stack,
				"getProcessor", stackIndex);
			final Object lut = composite ? imageJ1HelperService.invokeLegacyMethod(image,
				"getChannelLut", channel + 1) : imageJ1HelperService.invokeLegacyMethod(
					processor, "getLut");
			final double minimum = composite ? asDouble(imageJ1HelperService
				.getLegacyField(lut, "min")) : channel == currentChannel ? asDouble(
					imageJ1HelperService.invokeLegacyMethod(image, "getDisplayRangeMin")) :
					asDouble(imageJ1HelperService.invokeLegacyMethod(processor, "getMin"));
			final double maximum = composite ? asDouble(imageJ1HelperService
				.getLegacyField(lut, "max")) : channel == currentChannel ? asDouble(
					imageJ1HelperService.invokeLegacyMethod(image, "getDisplayRangeMax")) :
					asDouble(imageJ1HelperService.invokeLegacyMethod(processor, "getMax"));
			final boolean active = composite ? channel < activeChannels.length &&
				activeChannels[channel] : channel == currentChannel;
			channels.add(new ImageRenderMetadata.ChannelMetadata(channel, label, active,
				minimum, maximum, lut == null ? null : lutMetadata(lut)));
		}
		return channels;
	}

	private String channelLabel(final Object stack, final int stackIndex,
		final int channel)
	{
		final Object stackLabel = imageJ1HelperService.invokeLegacyMethod(stack,
			"getSliceLabel", stackIndex);
		if (stackLabel instanceof String label && !label.isBlank()) {
			final String firstLine = label.lines().findFirst().orElse("").trim();
			if (!firstLine.isEmpty()) return firstLine;
		}
		return "Channel " + (channel + 1);
	}

	private ImageRenderMetadata.LutMetadata lutMetadata(final Object lut) {
		final int size = asInt(imageJ1HelperService.invokeLegacyMethod(lut,
			"getMapSize"));
		final byte[] reds = new byte[size];
		final byte[] greens = new byte[size];
		final byte[] blues = new byte[size];
		imageJ1HelperService.invokeLegacyMethod(lut, "getReds", reds);
		imageJ1HelperService.invokeLegacyMethod(lut, "getGreens", greens);
		imageJ1HelperService.invokeLegacyMethod(lut, "getBlues", blues);
		return new ImageRenderMetadata.LutMetadata(new ColorTable8(reds, greens,
			blues));
	}
}
