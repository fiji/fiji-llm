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

package sc.fiji.llm.data;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
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

	@Tool(value = { "Read the current ROI Manager state, including whether it is open, and if so how many ROIs are present, and summary data for each ROI." }, name = "fiji_rois_read")
	public String readRoiManager() {
		try {
			final Object manager = invokeLegacyRoiManager();
			final JsonObject result = new JsonObject();
			result.addProperty("present", manager != null);
			if (manager == null) {
				return result.toString();
			}

			final int count = (int) invoke(manager, "getCount");
			result.addProperty("count", count);

			final JsonArray rois = new JsonArray();
			for (int i = 0; i < count; i++) {
				final Object roiObject = invoke(manager, "getRoi", i);
				final JsonObject roi = roiSummary(manager, i, roiObject);
				if (roiObject == null) roi.add("bounds", JsonNull.INSTANCE);
				else roi.add("bounds", boundsJson(invoke(roiObject, "getBounds")));
				rois.add(roi);
			}
			if (rois.size() > 0) result.add("rois", rois);
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_rois_read: " + e.getMessage());
		}
		catch (ReflectiveOperationException e) {
			return jsonError("Failed to run fiji_rois_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Select one ROI Manager entry by zero-based index and restore it to the active ImageJ image. This may change the active stack position." }, name = "fiji_rois_select")
	public String selectRoi(@P(name = "roi_index", value = "0-based ROI index from fiji_rois_read") final int roiIndex) {
		try {
			final Object manager = invokeLegacyRoiManager();
			if (manager == null) return jsonError("ROI Manager is not open",
				ErrorOptions.withTool("fiji_rois_read"));

			final int count = (int) invoke(manager, "getCount");
			if (roiIndex < 0 || roiIndex >= count) return jsonError("ROI index is out of range: " +
				roiIndex + " (count: " + count + ")", ErrorOptions.withTool(
					"fiji_rois_read"));

			selectOnEventDispatchThread(manager, roiIndex);
			return selectionJson(manager, roiIndex).toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_rois_select: " + e.getMessage());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return jsonError("Failed to run fiji_rois_select: " + e.getMessage());
		}
		catch (ReflectiveOperationException e) {
			return jsonError("Failed to run fiji_rois_select: " + e.getMessage());
		}
	}

	@Tool(value = { "Read the exact geometry for one ROI Manager entry, including its shape, bounding box, and polygon coordinates." }, name = "fiji_rois_read_details")
	public String readRoiDetails(@P(name = "roi_index", value = "0-based ROI index from fiji_rois_read") final int roiIndex) {
		try {
			final Object manager = invokeLegacyRoiManager();
			if (manager == null) return jsonError("ROI Manager is not open",
				ErrorOptions.withTool("fiji_rois_read"));

			final int count = (int) invoke(manager, "getCount");
			if (roiIndex < 0 || roiIndex >= count) return jsonError("ROI index must be between 0 and " +
				(count - 1) + ": " + roiIndex, ErrorOptions.withTool("fiji_rois_read"));

			final Object roiObject = invoke(manager, "getRoi", roiIndex);
			if (roiObject == null) return jsonError("ROI Manager entry has no ROI object: " +
				roiIndex);

			final JsonObject result = roiSummary(manager, roiIndex, roiObject);
			result.addProperty("shape", roiObject.getClass().getSimpleName());
			result.add("bounds", boundsJson(invoke(roiObject, "getBounds")));
			result.add("coordinates", coordinatesJson(invoke(roiObject,
				"getFloatPolygon")));
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_rois_read_details: " + e.getMessage());
		}
		catch (ReflectiveOperationException e) {
			return jsonError("Failed to run fiji_rois_read_details: " + e.getMessage());
		}
	}

	private static JsonObject roiSummary(final Object manager, final int index,
		final Object roiObject) throws ReflectiveOperationException
	{
		final JsonObject roi = new JsonObject();
		roi.addProperty("roi_index", index);
		final String name = safeString(invoke(manager, "getName", index));
		if (!name.isBlank()) roi.addProperty("name", name);
		roi.addProperty("selected", isSelected(manager, index));
		if (roiObject != null) {
			final String type = safeString(invoke(roiObject, "getType"));
			if (!type.isBlank()) roi.addProperty("type", type);
		}
		return roi;
	}

	private static JsonObject boundsJson(final Object bounds)
		throws ReflectiveOperationException
	{
		if (bounds == null) return null;

		final JsonObject result = new JsonObject();
		result.addProperty("x", ((Number) readField(bounds, "x")).intValue());
		result.addProperty("y", ((Number) readField(bounds, "y")).intValue());
		result.addProperty("width", ((Number) readField(bounds, "width"))
			.intValue());
		result.addProperty("height", ((Number) readField(bounds, "height"))
			.intValue());
		return result;
	}

	private static JsonArray coordinatesJson(final Object floatPolygon)
		throws ReflectiveOperationException
	{
		final JsonArray coordinates = new JsonArray();
		if (floatPolygon == null) return coordinates;

		final int pointCount = ((Number) readField(floatPolygon, "npoints"))
			.intValue();
		final float[] xPoints = (float[]) readField(floatPolygon, "xpoints");
		final float[] yPoints = (float[]) readField(floatPolygon, "ypoints");
		for (int i = 0; i < pointCount; i++) {
			if (!Float.isFinite(xPoints[i]) || !Float.isFinite(yPoints[i])) {
				coordinates.add(JsonNull.INSTANCE);
				continue;
			}

			final JsonObject point = new JsonObject();
			point.addProperty("x", xPoints[i]);
			point.addProperty("y", yPoints[i]);
			coordinates.add(point);
		}
		return coordinates;
	}

	private static Object readField(final Object target, final String fieldName)
		throws ReflectiveOperationException
	{
		final Field field = target.getClass().getField(fieldName);
		return field.get(target);
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
		return (boolean) invoke(manager, "isSelected", index);
	}

	private static void selectOnEventDispatchThread(final Object manager,
		final int roiIndex) throws ReflectiveOperationException, InterruptedException,
		InvocationTargetException
	{
		final ReflectiveOperationException[] failure = new ReflectiveOperationException[1];
		final Runnable select = () -> {
			try {
				invoke(manager, "select", roiIndex);
			}
			catch (ReflectiveOperationException e) {
				failure[0] = e;
			}
		};
		if (SwingUtilities.isEventDispatchThread()) select.run();
		else SwingUtilities.invokeAndWait(select);
		if (failure[0] != null) throw failure[0];
	}

	private static JsonObject selectionJson(final Object manager, final int roiIndex)
		throws ReflectiveOperationException
	{
		final JsonObject result = new JsonObject();
		result.addProperty("roi_index", roiIndex);
		result.addProperty("selected", isSelected(manager, roiIndex));

		final Object image = invokeStatic("ij.WindowManager", "getCurrentImage");
		result.addProperty("applied_to_active_image", image != null && invoke(image,
			"getRoi") != null);
		if (image != null) {
			final Object imageId = invoke(image, "getID");
			if (imageId instanceof Number) result.addProperty("image_id", ((Number) imageId)
				.intValue());
			final String title = safeString(invoke(image, "getTitle"));
			if (!title.isBlank()) result.addProperty("title", title);
			final Object slice = invoke(image, "getCurrentSlice");
			if (slice instanceof Number) result.addProperty("slice", ((Number) slice)
				.intValue());
		}
		return result;
	}

	private static Object invokeStatic(final String className, final String methodName)
		throws ReflectiveOperationException
	{
		return Class.forName(className).getMethod(methodName).invoke(null);
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
