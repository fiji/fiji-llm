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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.image;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sc.fiji.llm.Setup;
import sc.fiji.llm.tools.AiToolService;

public class ImageToolPluginTest {

	private Context context;
	private ImageToolPlugin plugin;

	@Before
	public void setUp() throws Exception {
		context = Setup.context();
		plugin = context.getService(AiToolService.class).getInstance(
			ImageToolPlugin.class);
		closeImages();
	}

	@After
	public void tearDown() throws Exception {
		closeImages();
		context.dispose();
	}

	@Test
	public void testUnknownImageIdWhenNoImagesAreOpen() {
		assertUnknownImageError(plugin.getImageDetails(7));
	}

	@Test
	public void testUnknownImageIdWhenAnotherImageIsOpen() throws Exception {
		final Class<?> imagePlusClass = Class.forName("ij.ImagePlus");
		final Class<?> processorClass = Class.forName("ij.process.ImageProcessor");
		final Object processor = Class.forName("ij.process.ByteProcessor").getConstructor(
			int.class, int.class).newInstance(2, 2);
		final Object image = imagePlusClass.getConstructor(String.class, processorClass)
			.newInstance("open", processor);
		imagePlusClass.getMethod("show").invoke(image);
		try {
			assertUnknownImageError(plugin.getImageDetails(7));
		}
		finally {
			imagePlusClass.getMethod("close").invoke(image);
		}
	}

	private static void assertUnknownImageError(final String response) {
		final JsonObject error = JsonParser.parseString(response).getAsJsonObject();
		assertEquals("No open image found with id: 7", error.get("error")
			.getAsString());
		assertEquals("fiji_image_list", error.get("recommended_tool").getAsString());
	}

	private static void closeImages() throws Exception {
		final Class<?> windowManagerClass = Class.forName("ij.WindowManager");
		final Class<?> imagePlusClass = Class.forName("ij.ImagePlus");
		final Method getImage = windowManagerClass.getMethod("getImage", int.class);
		final Method close = imagePlusClass.getMethod("close");
		final Field changes = imagePlusClass.getField("changes");
		final int[] ids = (int[]) windowManagerClass.getMethod("getIDList").invoke(null);
		if (ids == null) return;
		for (final int id : ids) {
			final Object image = getImage.invoke(null, id);
			if (image != null) {
				changes.setBoolean(image, false);
				close.invoke(image);
			}
		}
	}
}
