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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.app.AppService;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sc.fiji.llm.data.ImageJ1HelperService;

public class SystemInfoToolPluginTest {

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
	public void testReadsSystemInformation() throws Exception {
		final SystemInfoToolPlugin plugin = new SystemInfoToolPlugin();
		setField(plugin, "appService", context.getService(AppService.class));
		setField(plugin, "imageJ1HelperService", context.getService(
			ImageJ1HelperService.class));

		final JsonObject json = JsonParser.parseString(plugin.readSystemInfo())
			.getAsJsonObject();

		assertNotNull(json.get("imagej1_version").getAsString());
		assertNotNull(json.get("application_version").getAsString());
		assertEquals(System.getProperty("java.version"), json.get("java_version")
			.getAsString());
		assertTrue(json.get("active_update_sites").isJsonArray());
		assertTrue(json.getAsJsonObject("memory").get("available_jvm_memory_bytes")
			.getAsLong() >= 0);
		assertNotNull(json.getAsJsonObject("operating_system").get("name")
			.getAsString());
	}

	@Test
	public void testListsUpdateSites() {
		final SystemInfoToolPlugin plugin = new SystemInfoToolPlugin();
		final JsonObject json = JsonParser.parseString(plugin.readUpdateSites())
			.getAsJsonObject();

		assertTrue(json.has("update_sites"));
		for (final JsonElement element : json.getAsJsonArray("update_sites")) {
			final JsonObject site = element.getAsJsonObject();
			assertTrue(site.has("active"));
			assertTrue(site.has("name"));
			assertTrue(site.has("url"));
		}
	}

	private static void setField(final Object target, final String name,
		final Object value) throws Exception
	{
		final Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
