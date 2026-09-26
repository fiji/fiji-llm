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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

import javax.imageio.ImageIO;

import org.junit.Test;

public class DefaultImageRenderingServiceTest {

	@Test
	public void testBoundedPng() throws IOException {
		final BufferedImage source = new BufferedImage(4000, 1000,
			BufferedImage.TYPE_INT_ARGB);
		final BufferedImage bounded = DefaultImageRenderingService.bound(source, 512);

		assertEquals(512, bounded.getWidth());
		assertEquals(128, bounded.getHeight());
		final byte[] png = DefaultImageRenderingService.encodePng(bounded);
		assertNotNull(ImageIO.read(new ByteArrayInputStream(png)));
	}

	@Test
	public void testRenderedImageResultCreatesImageContent() {
		final byte[] png = { 1, 2, 3 };
		final ImageRenderMetadata metadata = new ImageRenderMetadata("image", 7, 10,
			20, 10, 20, Collections.emptyMap(), -1, 1, "COMPOSITE",
			Collections.emptyList(), false, "");
		final RenderedImageResult result = new RenderedImageResult(png, metadata);

		assertNotNull(result.getImageContent());
		assertArrayEquals(png, result.getPngBytes());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOptionsRejectNonPositiveBound() {
		new ImageRenderOptions(0, false);
	}
}
