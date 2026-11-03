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
package sc.fiji.llm.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import javax.swing.JPanel;

import org.junit.Test;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import sc.fiji.llm.image.ImageRenderingService;

public class UiToolPluginTest {

	@Test
	public void renderComponentRepaintsCanvasChildren() {
		final Color background = new Color(17, 34, 51);
		final Color canvasColor = new Color(203, 41, 79);
		final JPanel component = new JPanel(null) {

			@Override
			public void paint(final Graphics graphics) {
				graphics.setColor(background);
				graphics.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		component.setSize(12, 8);
		final Canvas canvas = new Canvas() {

			@Override
			public void paint(final Graphics graphics) {
				graphics.setColor(canvasColor);
				graphics.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		canvas.setBounds(4, 2, 5, 4);
		component.add(canvas);

		final BufferedImage image = AWTDialogUtils.renderComponent(component);

		assertEquals(12, image.getWidth());
		assertEquals(8, image.getHeight());
		assertEquals(background.getRGB(), image.getRGB(1, 1));
		assertEquals(canvasColor.getRGB(), image.getRGB(5, 3));
	}

	@Test
	public void screenshotToolsRespectPixelSharingPrivacy() throws ReflectiveOperationException {
		final UiToolPlugin plugin = new UiToolPlugin();
		final Field imageRenderingService = UiToolPlugin.class.getDeclaredField(
			"imageRenderingService");
		imageRenderingService.setAccessible(true);
		imageRenderingService.set(plugin, disabledImageRenderingService());

		assertPrivacyDisabled(plugin.screenshot("Missing", null));
		assertPrivacyDisabled(plugin.screenCapture("Missing", null));
	}

	private static void assertPrivacyDisabled(final List<Content> result) {
		assertEquals(1, result.size());
		assertTrue(result.get(0) instanceof TextContent);
		assertEquals("{\"error\":\"" + ImageRenderingService
			.PIXEL_SHARING_DISABLED_MESSAGE + "\"}", ((TextContent) result.get(0)).text());
	}

	private static ImageRenderingService disabledImageRenderingService() {
		return (ImageRenderingService) Proxy.newProxyInstance(ImageRenderingService.class
			.getClassLoader(), new Class<?>[] { ImageRenderingService.class }, (proxy, method,
				arguments) -> {
					if ("isPixelSharingAllowed".equals(method.getName())) return false;
					throw new UnsupportedOperationException(method.getName());
				});
	}
}
