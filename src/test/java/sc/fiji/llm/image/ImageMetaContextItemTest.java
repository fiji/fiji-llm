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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import dev.langchain4j.data.message.ImageContent;
import net.imglib2.display.ColorTable8;

public class ImageMetaContextItemTest {

	@Test
	public void testJsonRemainsMetadataOnly() {
		final ImageContent imageContent = ImageContent.from("AQID", "image/png");
		final ImageMetaContextItem item = new ImageMetaContextItem("image", 7,
			Collections.singletonList(new ImageMetaContextItem.Dimension("X", 2)),
			"UnsignedByteType", imageContent);

		assertTrue(item.getImageContent().isPresent());
		assertFalse(item.toJson().toString().contains("AQID"));
		assertFalse(item.toJson().getAsJsonObject().has("image_content"));
	}

	@Test
	public void testScreenAndFlattenedItemsAreDistinct() {
		final ImageMetaContextItem screen = new ImageMetaContextItem("image", 7,
			Collections.emptyList(), "UnsignedByteType", null, null, false);
		final ImageMetaContextItem flattened = new ImageMetaContextItem("image", 7,
			Collections.emptyList(), "UnsignedByteType", null, null, true);

		assertFalse(screen.equals(flattened));
		assertEquals("screen", screen.toJson().getAsJsonObject().get("context_mode")
			.getAsString());
		assertEquals("flattened", flattened.toJson().getAsJsonObject().get(
			"context_mode").getAsString());
		assertFalse(screen.toJson().getAsJsonObject().has("render_mode"));
		assertFalse(flattened.toJson().getAsJsonObject().has("render_mode"));
	}

	@Test
	public void testTooltipTextDescribesRenderModes() {
		final ImageMetaContextItem screen = new ImageMetaContextItem("image", 7,
			Collections.emptyList(), "UnsignedByteType", null, null, false);
		final ImageMetaContextItem flattened = new ImageMetaContextItem("image", 7,
			Collections.emptyList(), "UnsignedByteType", null, null, true);

		assertEquals("screen-rendered image", screen.getTooltipText());
		assertEquals("flattened image, including its display LUT, ROIs, and overlays", flattened
			.getTooltipText());
	}

	@Test
	public void testRenderMetadataReportsAnnotations() {
		final ImageRenderMetadata metadata = new ImageRenderMetadata("image", 7, 10,
			20, 10, 20, Map.of("Z", 2L), 0, 1, "COMPOSITE", Collections.emptyList(),
			true, "ij.gui.Roi", true, 3, ImageRenderMetadata.FLATTENED_RENDER_MODE);

		assertTrue(metadata.toJson().get("roi_included").getAsBoolean());
		assertTrue(metadata.toJson().get("overlay_included").getAsBoolean());
		assertEquals(3, metadata.toJson().get("overlay_count").getAsInt());
		assertEquals("flattened", metadata.toJson().get("render_mode").getAsString());
	}

	@Test
	public void testDetailsAndViewShareImageMetadata() {
		final JsonObject calibration = new JsonObject();
		calibration.addProperty("pixel_width", 0.5);
		calibration.addProperty("unit", "micron");
		final ImageRenderMetadata.ChannelMetadata channel = new ImageRenderMetadata
			.ChannelMetadata(0, "Nucleus", true, 0, 255,
				new ImageRenderMetadata.LutMetadata(new ColorTable8(
					new byte[] { 0 }, new byte[] { 0 }, new byte[] { (byte) 255 })));
		final ImageRenderMetadata metadata = new ImageRenderMetadata("image", 7, 186,
			226, 186, 226, Map.of("Z", 19L), 0, 2, "COMPOSITE", List.of(channel),
			false, "", false, 0, ImageRenderMetadata.FLATTENED_RENDER_MODE, true,
			"UnsignedByteType", List.of(new ImageRenderMetadata.AxisMetadata(0, "X",
				186), new ImageRenderMetadata.AxisMetadata(1, "Y", 226),
				new ImageRenderMetadata.AxisMetadata(2, "Z", 27)), calibration, true);

		final JsonObject details = metadata.toDetailsJson();
		final JsonObject view = metadata.toJson();
		assertEquals(2, details.getAsJsonArray("dimensions").get(2).getAsJsonObject()
			.get("axis_index").getAsInt());
		assertEquals(0, details.get("index_base").getAsInt());
		assertEquals(0, details.get("channel_index").getAsInt());
		assertTrue(details.get("is_composite").getAsBoolean());
		assertEquals("COMPOSITE", details.get("display_mode").getAsString());
		final JsonObject channelJson = details.getAsJsonArray("channels").get(0)
			.getAsJsonObject();
		assertEquals(0, channelJson.get("channel_index").getAsInt());
		assertEquals("Nucleus", channelJson.get("label").getAsString());
		assertEquals(0, channelJson.getAsJsonObject("display_range").get("min")
			.getAsInt());
		assertEquals("#0000FF", channelJson.getAsJsonObject("lut").get(
			"color_at_max").getAsString());
		assertEquals("micron", details.getAsJsonObject("spatial_calibration").get(
			"unit").getAsString());
		assertEquals(details.get("dimensions"), view.get("dimensions"));
		assertEquals(details.get("index_base"), view.get("index_base"));
		assertEquals(details.get("plane_position"), view.get("plane_position"));
		assertEquals(details.get("channel_index"), view.get("channel_index"));
		assertEquals(details.get("channels"), view.get("channels"));
		assertEquals(details.get("spatial_calibration"), view.get(
			"spatial_calibration"));
		assertFalse(details.has("render_mode"));
		assertEquals("flattened", view.get("render_mode").getAsString());
	}
}
