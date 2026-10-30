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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.imglib2.display.ColorTable;

/** Metadata describing the display state used to render an image. */
public final class ImageRenderMetadata {

	public static final String SCREEN_RENDER_MODE = "screen";
	public static final String FLATTENED_RENDER_MODE = "flattened";

	private final String title;
	private final int imageId;
	private final int sourceWidth;
	private final int sourceHeight;
	private final int renderedWidth;
	private final int renderedHeight;
	private final Map<String, Long> planePosition;
	private final long channelIndex;
	private final int channelCount;
	private final String colorMode;
	private final List<ChannelMetadata> channels;
	private final boolean roiIncluded;
	private final String roiType;
	private final boolean overlayIncluded;
	private final int overlayCount;
	private final String renderMode;
	private final boolean active;
	private final String pixelType;
	private final List<AxisMetadata> dimensions;
	private final JsonObject spatialCalibration;
	private final boolean composite;

	public ImageRenderMetadata(final String title, final int imageId,
		final int sourceWidth, final int sourceHeight, final int renderedWidth,
		final int renderedHeight, final Map<String, Long> planePosition,
		final long channelIndex, final int channelCount, final String colorMode,
		final List<ChannelMetadata> channels, final boolean roiIncluded,
		final String roiType)
	{
		this(title, imageId, sourceWidth, sourceHeight, renderedWidth,
			renderedHeight, planePosition, channelIndex, channelCount, colorMode,
			channels, roiIncluded, roiType, false, 0);
	}

	public ImageRenderMetadata(final String title, final int imageId,
		final int sourceWidth, final int sourceHeight, final int renderedWidth,
		final int renderedHeight, final Map<String, Long> planePosition,
		final long channelIndex, final int channelCount, final String colorMode,
		final List<ChannelMetadata> channels, final boolean roiIncluded,
		final String roiType, final boolean overlayIncluded, final int overlayCount)
	{
		this(title, imageId, sourceWidth, sourceHeight, renderedWidth,
			renderedHeight, planePosition, channelIndex, channelCount, colorMode,
			channels, roiIncluded, roiType, overlayIncluded, overlayCount,
			SCREEN_RENDER_MODE);
	}

	public ImageRenderMetadata(final String title, final int imageId,
		final int sourceWidth, final int sourceHeight, final int renderedWidth,
		final int renderedHeight, final Map<String, Long> planePosition,
		final long channelIndex, final int channelCount, final String colorMode,
		final List<ChannelMetadata> channels, final boolean roiIncluded,
		final String roiType, final boolean overlayIncluded, final int overlayCount,
		final String renderMode)
	{
		this(title, imageId, sourceWidth, sourceHeight, renderedWidth, renderedHeight,
			planePosition, channelIndex, channelCount, colorMode, channels, roiIncluded,
			roiType, overlayIncluded, overlayCount, renderMode, true, "",
			Collections.emptyList(), new JsonObject(), "COMPOSITE".equals(colorMode));
	}

	ImageRenderMetadata(final String title, final int imageId,
		final int sourceWidth, final int sourceHeight, final int renderedWidth,
		final int renderedHeight, final Map<String, Long> planePosition,
		final long channelIndex, final int channelCount, final String colorMode,
		final List<ChannelMetadata> channels, final boolean roiIncluded,
		final String roiType, final boolean overlayIncluded, final int overlayCount,
		final String renderMode, final boolean active, final String pixelType,
		final List<AxisMetadata> dimensions, final JsonObject spatialCalibration,
		final boolean composite)
	{
		this.title = title == null ? "" : title;
		this.imageId = imageId;
		this.sourceWidth = sourceWidth;
		this.sourceHeight = sourceHeight;
		this.renderedWidth = renderedWidth;
		this.renderedHeight = renderedHeight;
		this.planePosition = Collections.unmodifiableMap(new LinkedHashMap<>(
			planePosition));
		this.channelIndex = channelIndex;
		this.channelCount = channelCount;
		this.colorMode = colorMode == null ? "" : colorMode;
		this.channels = Collections.unmodifiableList(new ArrayList<>(channels));
		this.roiIncluded = roiIncluded;
		this.roiType = roiType == null ? "" : roiType;
		this.overlayIncluded = overlayIncluded;
		this.overlayCount = overlayCount;
		this.renderMode = renderMode == null ? SCREEN_RENDER_MODE : renderMode;
		this.active = active;
		this.pixelType = pixelType == null ? "" : pixelType;
		this.dimensions = Collections.unmodifiableList(new ArrayList<>(dimensions));
		this.spatialCalibration = spatialCalibration == null ? new JsonObject() :
			spatialCalibration.deepCopy();
		this.composite = composite;
	}

	public String getTitle() {
		return title;
	}

	public int getImageId() {
		return imageId;
	}

	public int getSourceWidth() {
		return sourceWidth;
	}

	public int getSourceHeight() {
		return sourceHeight;
	}

	public int getRenderedWidth() {
		return renderedWidth;
	}

	public int getRenderedHeight() {
		return renderedHeight;
	}

	public Map<String, Long> getPlanePosition() {
		return planePosition;
	}

	public long getChannelIndex() {
		return channelIndex;
	}

	public int getChannelCount() {
		return channelCount;
	}

	public String getColorMode() {
		return colorMode;
	}

	public List<ChannelMetadata> getChannels() {
		return channels;
	}

	public boolean isRoiIncluded() {
		return roiIncluded;
	}

	public String getRoiType() {
		return roiType;
	}

	public boolean isOverlayIncluded() {
		return overlayIncluded;
	}

	public int getOverlayCount() {
		return overlayCount;
	}

	public String getRenderMode() {
		return renderMode;
	}

	/** Returns a concise JSON description of the rendered display state. */
	public JsonObject toJson() {
		final JsonObject result = toDetailsJson();
		result.addProperty("source_width", sourceWidth);
		result.addProperty("source_height", sourceHeight);
		result.addProperty("rendered_width", renderedWidth);
		result.addProperty("rendered_height", renderedHeight);
		result.addProperty("render_mode", renderMode);
		if (roiIncluded) result.addProperty("roi_included", true);
		if (!roiType.isEmpty()) result.addProperty("roi_type", roiType);
		if (overlayIncluded) result.addProperty("overlay_included", true);
		if (overlayCount > 0) result.addProperty("overlay_count", overlayCount);
		return result;
	}

	JsonObject toDetailsJson() {
		final JsonObject result = new JsonObject();
		result.addProperty("title", title);
		result.addProperty("image_id", imageId);
		result.addProperty("active", active);
		if (!pixelType.isEmpty()) result.addProperty("pixel_type", pixelType);
		final JsonArray axes = new JsonArray();
		for (final AxisMetadata dimension : dimensions) axes.add(dimension.toJson());
		result.add("dimensions", axes);
		result.addProperty("index_base", 0);
		final JsonObject position = new JsonObject();
		for (final Map.Entry<String, Long> entry : planePosition.entrySet()) {
			position.addProperty(entry.getKey(), entry.getValue());
		}
		if (position.size() > 0) result.add("plane_position", position);
		result.addProperty("channel_index", channelIndex);
		result.addProperty("channel_count", channelCount);
		result.addProperty("is_composite", composite);
		if (!colorMode.isEmpty()) {
			result.addProperty("display_mode", colorMode);
			result.addProperty("color_mode", colorMode);
		}
		final JsonArray channelMetadata = new JsonArray();
		for (final ChannelMetadata channel : channels) channelMetadata.add(channel.toJson());
		result.add("channels", channelMetadata);
		result.add("spatial_calibration", spatialCalibration.deepCopy());
		return result;
	}

	static final class AxisMetadata {

		private final int index;
		private final String type;
		private final long length;

		AxisMetadata(final int index, final String type, final long length) {
			this.index = index;
			this.type = type == null ? "Unknown" : type;
			this.length = length;
		}

		private JsonObject toJson() {
			final JsonObject result = new JsonObject();
			result.addProperty("axis_index", index);
			result.addProperty("type", type);
			result.addProperty("length", length);
			return result;
		}
	}

	/** Display range and LUT metadata for one displayed channel. */
	public static final class ChannelMetadata {

		private final int index;
		private final String label;
		private final boolean active;
		private final double displayMinimum;
		private final double displayMaximum;
		private final LutMetadata lut;

		public ChannelMetadata(final int index, final double displayMinimum,
			final double displayMaximum, final LutMetadata lut)
		{
			this(index, "Channel " + (index + 1), true, displayMinimum,
				displayMaximum, lut);
		}

		ChannelMetadata(final int index, final String label, final boolean active,
			final double displayMinimum, final double displayMaximum,
			final LutMetadata lut)
		{
			this.index = index;
			this.label = label == null ? "Channel " + (index + 1) : label;
			this.active = active;
			this.displayMinimum = displayMinimum;
			this.displayMaximum = displayMaximum;
			this.lut = lut;
		}

		public int getIndex() {
			return index;
		}

		public double getDisplayMinimum() {
			return displayMinimum;
		}

		public double getDisplayMaximum() {
			return displayMaximum;
		}

		public LutMetadata getLut() {
			return lut;
		}

		private JsonObject toJson() {
			final JsonObject result = new JsonObject();
			result.addProperty("channel_index", index);
			result.addProperty("label", label);
			result.addProperty("active", active);
			final JsonObject displayRange = new JsonObject();
			displayRange.addProperty("min", displayMinimum);
			displayRange.addProperty("max", displayMaximum);
			result.add("display_range", displayRange);
			if (lut != null) {
				final JsonObject lutJson = new JsonObject();
				lutJson.addProperty("type", lut.getType());
				lutJson.addProperty("length", lut.getLength());
				final String color = lut.colorAtMaximum();
				if (color != null) lutJson.addProperty("color_at_max", color);
				result.add("lut", lutJson);
			}
			return result;
		}
	}

	/** A serializable snapshot of one ImageJ color table. */
	public static final class LutMetadata {

		private final String type;
		private final int length;
		private final int componentCount;
		private final List<List<Integer>> values;

		public LutMetadata(final ColorTable colorTable) {
			type = colorTable.getClass().getName();
			length = colorTable.getLength();
			componentCount = colorTable.getComponentCount();
			final List<List<Integer>> components = new ArrayList<>();
			for (int component = 0; component < componentCount; component++) {
				final List<Integer> entries = new ArrayList<>();
				for (int index = 0; index < length; index++) {
					entries.add(colorTable.get(component, index));
				}
				components.add(Collections.unmodifiableList(entries));
			}
			values = Collections.unmodifiableList(components);
		}

		public String getType() {
			return type;
		}

		public int getLength() {
			return length;
		}

		public int getComponentCount() {
			return componentCount;
		}

		public List<List<Integer>> getValues() {
			return values;
		}

		private String colorAtMaximum() {
			if (length == 0 || componentCount < 3) return null;
			return String.format(Locale.ROOT, "#%02X%02X%02X", values.get(0).get(
				length - 1), values.get(1).get(length - 1), values.get(2).get(
				length - 1));
		}
	}
}
