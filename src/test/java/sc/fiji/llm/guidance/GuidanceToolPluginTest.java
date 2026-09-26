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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GuidanceToolPluginTest {

	private Context context;

	@Before
	public void setUp() {
		context = new Context();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void exposesGuidanceCatalogAndDocuments() throws Exception {
		final GuidanceToolPlugin plugin = plugin();

		final JsonObject topics = JsonParser.parseString(plugin.listTopics()).getAsJsonObject();
		assertTrue(topics.getAsJsonArray("topics").contains(JsonParser.parseString(
			"\"macros\"")));

		final JsonObject search = JsonParser.parseString(plugin.search("MACROS"))
			.getAsJsonObject();
		assertEquals(2, search.get("count").getAsInt());
		final JsonObject metadata = search.getAsJsonArray("documents").get(0)
			.getAsJsonObject();
		assertEquals("creating-macros", metadata.get("id").getAsString());
		assertFalse(metadata.has("content"));

		final JsonObject document = JsonParser.parseString(plugin.read("scripts-and-macros"))
			.getAsJsonObject();
		assertEquals("scripts-and-macros", document.get("id").getAsString());
		assertFalse(document.get("content").getAsString().isEmpty());

		final JsonObject onboarding = JsonParser.parseString(plugin.readOnboarding())
			.getAsJsonObject();
		assertEquals("onboarding", onboarding.get("id").getAsString());
		assertTrue(onboarding.get("content").getAsString().contains(
			"# Fiji Onboarding"));
	}

	@Test
	public void reportsUnknownDocuments() throws Exception {
		final JsonObject result = JsonParser.parseString(plugin().read("missing"))
			.getAsJsonObject();

		assertTrue(result.get("error").getAsString().contains("No guidance document"));
		assertEquals("fiji_guide_search", result.get("recommended_tool").getAsString());
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
