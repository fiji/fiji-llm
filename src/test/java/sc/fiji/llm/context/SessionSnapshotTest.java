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

package sc.fiji.llm.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scijava.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sc.fiji.llm.Setup;
import sc.fiji.llm.image.ImageToolPlugin;
import sc.fiji.llm.script.ScriptEditorToolPlugin;
import sc.fiji.llm.tools.AiToolService;

public class SessionSnapshotTest {

	private static final String SCRIPTS =
		"{\"editors\":[{\"editor_id\":0,\"scripts\":[{\"script_name\":\"count.groovy\"," +
			"\"script_id\":\"0:0\",\"is_active\":true}],\"is_active\":true}]}";
	private static final String IMAGES =
		"{\"open_images\":[{\"id\":1,\"title\":\"blobs.gif\",\"active\":true}]}";

	@Test
	public void testIncludesBothListings() {
		final String snapshot = SessionSnapshot.format(SCRIPTS, IMAGES);
		assertTrue(snapshot, snapshot.startsWith("=== FIJI SESSION SNAPSHOT"));
		assertTrue(snapshot, snapshot.contains("fiji_script_list: " + SCRIPTS));
		assertTrue(snapshot, snapshot.contains("fiji_image_list: " + IMAGES));
		assertTrue(snapshot, snapshot.endsWith("=== END FIJI SESSION SNAPSHOT ==="));
	}

	@Test
	public void testSkipsErrorsAndMissingListings() {
		final String snapshot = SessionSnapshot.format("{\"error\":\"boom\"}",
			IMAGES);
		assertFalse(snapshot, snapshot.contains("fiji_script_list"));
		assertTrue(snapshot, snapshot.contains("fiji_image_list"));
		assertEquals("", SessionSnapshot.format(null, "not json"));
	}

	@Test
	public void testCapsImagesKeepingActive() {
		final JsonArray images = new JsonArray();
		for (int id = 1; id <= SessionSnapshot.MAX_IMAGES + 5; id++) {
			final JsonObject image = new JsonObject();
			image.addProperty("id", id);
			image.addProperty("active", id == SessionSnapshot.MAX_IMAGES + 5);
			images.add(image);
		}
		final JsonObject listing = new JsonObject();
		listing.add("open_images", images);

		final String snapshot = SessionSnapshot.format(null, listing.toString());
		final String json = snapshot.substring(snapshot.indexOf('{'), snapshot
			.lastIndexOf('}') + 1);
		final JsonObject capped = JsonParser.parseString(json).getAsJsonObject();
		final JsonArray kept = capped.getAsJsonArray("open_images");
		assertEquals(SessionSnapshot.MAX_IMAGES, kept.size());
		assertEquals(5, capped.get("omitted_images").getAsInt());
		assertTrue(kept.get(0).getAsJsonObject().get("active").getAsBoolean());
	}

	@Test
	public void testFormatsLiveToolListings() {
		final Context context = Setup.context();
		try {
			final AiToolService tools = context.getService(AiToolService.class);
			final String snapshot = SessionSnapshot.format(tools.getInstance(
				ScriptEditorToolPlugin.class).listOpenScripts(), tools.getInstance(
					ImageToolPlugin.class).listImages());
			assertTrue(snapshot, snapshot.contains("fiji_script_list: {\"editors\":"));
			assertTrue(snapshot, snapshot.contains(
				"fiji_image_list: {\"open_images\":"));
		}
		finally {
			context.dispose();
		}
	}
}
