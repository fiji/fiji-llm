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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

	@Test
	public void blankTitleWindowCanBeReadAndInteractedWithWhenUnique() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless());
		final BlankTitleDialog dialog = createVisibleBlankTitleDialog();
		try {
			final UiToolPlugin plugin = new UiToolPlugin();
			final String className = BlankTitleDialog.class.getName();
			final JsonObject controls = JsonParser.parseString(plugin.readControls("", className))
				.getAsJsonObject();
			assertEquals("", controls.get("window_title").getAsString());
			assertEquals(className, controls.get("window_class_name").getAsString());

			final String buttonPath = controlPath(controls, "Acknowledge");
			final JsonObject started = JsonParser.parseString(plugin.interactWithControl("",
				className, buttonPath, "click", null, null, null)).getAsJsonObject();
			assertTrue(started.has("run_id"));
			final JsonObject status = awaitInteraction(plugin, started.get("run_id")
				.getAsString());
			assertEquals("completed", status.get("status").getAsString());
			assertFalse(dialog.isShowing());
		}
		finally {
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	@Test
	public void blankTitleWindowMustBeUniqueForReadAndInteraction() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless());
		final BlankTitleDialog first = createVisibleBlankTitleDialog();
		final BlankTitleDialog second = createVisibleBlankTitleDialog();
		try {
			final UiToolPlugin plugin = new UiToolPlugin();
			final String className = BlankTitleDialog.class.getName();
			final JsonObject read = JsonParser.parseString(plugin.readControls("", className))
				.getAsJsonObject();
			assertTrue(read.get("error").getAsString().contains(
				"Multiple visible windows"));

			final JsonObject started = JsonParser.parseString(plugin.interactWithControl("",
				className, "root", "click", null, null, null)).getAsJsonObject();
			assertTrue(started.has("run_id"));
			final JsonObject status = awaitInteraction(plugin, started.get("run_id")
				.getAsString());
			assertEquals("failed", status.get("status").getAsString());
			assertTrue(status.get("error").getAsString().contains(
				"Multiple visible windows"));
			assertTrue(first.isShowing());
			assertTrue(second.isShowing());
		}
		finally {
			SwingUtilities.invokeAndWait(() -> {
				first.dispose();
				second.dispose();
			});
		}
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

		private static String controlPath(final JsonObject controls, final String label) {
			for (final JsonElement element : controls.getAsJsonArray("controls")) {
				final JsonObject control = element.getAsJsonObject();
				if (label.equals(control.get("text").getAsString())) return control.get(
					"component_path").getAsString();
			}
			throw new AssertionError("No control found with label: " + label);
		}

		private static JsonObject awaitInteraction(final UiToolPlugin plugin, final String runID)
			throws InterruptedException
		{
			final long deadline = System.nanoTime() + 5_000_000_000L;
			JsonObject status;
			do {
				status = JsonParser.parseString(plugin.interactWithControlStatus(runID)).getAsJsonObject();
				if (!"running".equals(status.get("status").getAsString())) return status;
				Thread.sleep(10);
			}
			while (System.nanoTime() < deadline);
			return status;
		}

		private static BlankTitleDialog createVisibleBlankTitleDialog() throws Exception {
			final BlankTitleDialog[] result = new BlankTitleDialog[1];
			SwingUtilities.invokeAndWait(() -> {
				result[0] = new BlankTitleDialog();
				result[0].setVisible(true);
			});
			return result[0];
		}

		private static final class BlankTitleDialog extends JDialog {

			private static final long serialVersionUID = 1L;

			BlankTitleDialog() {
				setTitle("");
				final JButton button = new JButton("Acknowledge");
				button.addActionListener(event -> dispose());
				add(button);
				pack();
			}
		}
}
