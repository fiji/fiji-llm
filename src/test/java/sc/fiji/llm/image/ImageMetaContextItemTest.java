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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import dev.langchain4j.data.message.ImageContent;

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
	public void testPlainAndAnnotatedItemsAreDistinct() {
		final ImageMetaContextItem plain = new ImageMetaContextItem("image", 7,
			Collections.emptyList(), "UnsignedByteType", null, null, false);
		final ImageMetaContextItem annotated = new ImageMetaContextItem("image", 7,
			Collections.emptyList(), "UnsignedByteType", null, null, true);

		assertFalse(plain.equals(annotated));
		assertEquals("plain", plain.toJson().getAsJsonObject().get("render_mode")
			.getAsString());
		assertEquals("annotated", annotated.toJson().getAsJsonObject().get(
			"render_mode").getAsString());
	}

	@Test
	public void testRenderMetadataReportsAnnotations() {
		final ImageRenderMetadata metadata = new ImageRenderMetadata("image", 7, 10,
			20, 10, 20, Map.of("Z", 2L), 0, 1, "COMPOSITE", Collections.emptyList(),
			true, "ij.gui.Roi", true, 3, "annotated");

		assertTrue(metadata.toJson().get("roi_included").getAsBoolean());
		assertTrue(metadata.toJson().get("overlay_included").getAsBoolean());
		assertEquals(3, metadata.toJson().get("overlay_count").getAsInt());
	}
}
