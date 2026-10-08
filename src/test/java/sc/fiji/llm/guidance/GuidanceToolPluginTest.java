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
package sc.fiji.llm.guidance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sc.fiji.llm.Setup;

public class GuidanceToolPluginTest {

	private static Context context;

	@BeforeClass
	public static void setUp() {
		context = Setup.context();
	}

	@AfterClass
	public static void disposeContext() {
		Setup.dispose(context);
	}

	@Test
	public void exposesGuidanceCatalogAndDocuments() throws Exception {
		final GuidanceToolPlugin plugin = plugin();

		final JsonObject list = JsonParser.parseString(plugin.list()).getAsJsonObject();
		assertTrue(list.get("count").getAsInt() > 0);
		final JsonObject metadata = list.getAsJsonArray("guides").get(0)
			.getAsJsonObject();
		assertTrue(metadata.has("guide_id"));
		assertFalse(metadata.get("summary").getAsString().isEmpty());
		assertFalse(metadata.has("content"));

		final JsonObject document = JsonParser.parseString(plugin.read("scripts-and-macros"))
			.getAsJsonObject();
		assertEquals("scripts-and-macros", document.get("guide_id").getAsString());
		assertFalse(document.get("summary").getAsString().isEmpty());
		assertFalse(document.get("content").getAsString().isEmpty());
	}

	@Test
	public void reportsUnknownDocuments() throws Exception {
		final JsonObject result = JsonParser.parseString(plugin().read("missing"))
			.getAsJsonObject();

		assertTrue(result.get("error").getAsString().contains("No guide found"));
		assertEquals("fiji_guide_list", result.get("recommended_tool").getAsString());
	}

	private GuidanceToolPlugin plugin() throws Exception {
		final GuidanceToolPlugin plugin = new GuidanceToolPlugin();
		final AgentGuidanceService guidanceService = context.getService(
			AgentGuidanceService.class);
		final Field field = GuidanceToolPlugin.class.getDeclaredField("agentGuidanceService");
		field.setAccessible(true);
		field.set(plugin, guidanceService);
		return plugin;
	}
}
