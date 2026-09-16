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
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.data;

import java.lang.reflect.Method;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/** Read-only tool for inspecting the ImageJ ROI Manager state. */
@Plugin(type = AiToolPlugin.class)
public class RoiManagerToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	public RoiManagerToolPlugin() {
		super(RoiManagerToolPlugin.class);
	}

	@Override
	public String getName() {
		return "ROI Tools";
	}

	@Override
	public String getUsage() {
		return """
The fiji_rois_* tools enable interaction with the ImageJ ROI Manager.
""";
	}

	@Tool(value = { "Read the current ROI Manager state, including whether it is open, and if so how many ROIs are present, and summary data for each ROI." }, name = "fiji_rois_read")
	public String readRoiManager() {
		try {
			final Object manager = invokeLegacyRoiManager();
			final JsonObject result = new JsonObject();
			result.addProperty("present", manager != null);
			if (manager == null) {
				result.addProperty("count", 0);
				result.add("rois", new JsonArray());
				return result.toString();
			}

			final int count = (int) invoke(manager, "size");
			result.addProperty("count", count);

			final JsonArray rois = new JsonArray();
			for (int i = 0; i < count; i++) {
				final JsonObject roi = new JsonObject();
				roi.addProperty("index", i);
				roi.addProperty("name", safeString(invoke(manager, "getName", i)));
				roi.addProperty("selected", isSelected(manager, i));
				roi.addProperty("type", safeString(invoke(manager, "getRoi", i, "getType")));
				rois.add(roi);
			}
			result.add("rois", rois);
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_rois_read: " + e.getMessage());
		}
		catch (ReflectiveOperationException e) {
			return jsonError("Failed to run fiji_rois_read: " + e.getMessage());
		}
	}

	private Object invokeLegacyRoiManager() {
		if (imageJ1HelperService == null) {
			return null;
		}
		return imageJ1HelperService.getRoiManager();
	}

	private static boolean isSelected(final Object manager, final int index)
		throws ReflectiveOperationException
	{
		final Object roi = invoke(manager, "getRoi", index);
		if (roi == null) return false;
		return (boolean) invoke(roi, "isSelected");
	}

	private static Object invoke(final Object target, final String methodName,
		final Object... args) throws ReflectiveOperationException
	{
		final Class<?>[] parameterTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Integer) {
				parameterTypes[i] = int.class;
			}
			else {
				parameterTypes[i] = args[i].getClass();
			}
		}
		final Method method = target.getClass().getMethod(methodName, parameterTypes);
		return method.invoke(target, args);
	}

	private static String safeString(final Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
