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

package sc.fiji.llm.system;

import java.io.IOException;

import org.scijava.app.App;
import org.scijava.app.AppService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.Tool;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import sc.fiji.llm.data.ImageJ1HelperService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/** Read-only tool for inspecting the host system and application versions. */
@Plugin(type = AiToolPlugin.class)
public class SystemInfoToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private AppService appService;

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	public SystemInfoToolPlugin() {
		super(SystemInfoToolPlugin.class);
	}

	@Override
	public String getName() {
		return "System Information";
	}

	@Tool(value = { "Read information about the Fiji installation and its host environment." }, name = "fiji_system_read")
	public String readSystemInfo() {
		try {
			final JsonObject result = new JsonObject();
			result.addProperty("imagej1_version", imageJ1HelperService == null ?
				"Unknown" : imageJ1HelperService.getImageJ1Version());
			result.addProperty("application_version", getApplicationVersion());
			result.addProperty("java_version", property("java.version"));
			result.add("active_update_sites", getUpdateSites(true));

			final Runtime runtime = Runtime.getRuntime();
			final long maxMemory = runtime.maxMemory();
			final long totalMemory = runtime.totalMemory();
			final long freeMemory = runtime.freeMemory();
			final long usedMemory = totalMemory - freeMemory;
			final JsonObject memory = new JsonObject();
			memory.addProperty("available_jvm_memory_bytes", maxMemory - usedMemory);
			memory.addProperty("free_memory_bytes", freeMemory);
			memory.addProperty("max_memory_bytes", maxMemory);
			memory.addProperty("total_memory_bytes", totalMemory);
			memory.addProperty("used_memory_bytes", usedMemory);
			result.add("memory", memory);

			final JsonObject operatingSystem = new JsonObject();
			operatingSystem.addProperty("name", property("os.name"));
			operatingSystem.addProperty("version", property("os.version"));
			operatingSystem.addProperty("architecture", property("os.arch"));
			result.add("operating_system", operatingSystem);
			return result.toString();
		}
		catch (IOException e) {
			return jsonError("Failed to run fiji_system_read: " + e.getMessage());
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_system_read: " + e.getMessage());
		}
	}

	@Tool(value = { "List available ImageJ update sites and their current configuration." }, name = "fiji_system_list_update_sites")
	public String readUpdateSites() {
		try {
			final JsonObject result = new JsonObject();
			result.add("update_sites", getUpdateSites(false));
			return result.toString();
		}
		catch (IOException e) {
			return jsonError("Failed to run fiji_system_list_update_sites: " + e.getMessage());
		}
	}

	private String getApplicationVersion() {
		if (appService == null) return "Unknown";
		final App app = appService.getApp();
		return app == null || app.getVersion() == null ? "Unknown" : app.getVersion();
	}

	private static JsonArray getUpdateSites(final boolean activeOnly)
		throws IOException
	{
		final JsonArray sites = new JsonArray();
		for (final UpdateSite site : AvailableSites.getAvailableSites().values()) {
			if (activeOnly && !site.isActive()) continue;
			final JsonObject entry = new JsonObject();
			entry.addProperty("active", site.isActive());
			entry.addProperty("name", site.getName());
			entry.addProperty("url", site.getURL());
			sites.add(entry);
		}
		return sites;
	}

	private static String property(final String name) {
		return System.getProperty(name, "Unknown");
	}
}
