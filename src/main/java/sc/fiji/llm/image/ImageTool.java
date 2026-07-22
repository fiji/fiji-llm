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

package sc.fiji.llm.image;

import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/**
 * AI tool for querying open images in Fiji.
 */
// @Plugin(type = AiToolPlugin.class)
public class ImageTool extends AbstractAiToolPlugin {

	@Parameter
	private ImagePlusHelper iPlusHelper;

	@Parameter
	private ImageDisplayService imageDisplayService;

	public ImageTool() {
		super(ImageTool.class);
	}

	@Override
	public String getName() {
		return "Image Tools";
	}

	@Override
	public String getUsage() {
		return "Tools for querying open images in Fiji. Use fiji_image_list to see all open images, " +
			"and fiji_image_details to get metadata (dimensions, pixel type) for a specific image by id.";
	}

	@Tool(value = { "List all currently open and visible images with their id and title." }, name = "fiji_image_list")
	public String listImages() {
		JsonArray images = new JsonArray();
		List<Integer> ids = iPlusHelper.getIds();
		for (Integer id : ids) {
			if (iPlusHelper.isVisible(id)) {
				JsonObject imageJson = new JsonObject();
				imageJson.addProperty("id", id);
				imageJson.addProperty("title", iPlusHelper.getTitle(id));
				images.add(imageJson);
			}
		}
		JsonObject result = new JsonObject();
		result.add("open_images", images);
		return result.toString();
	}

	@Tool(value = { "Get detailed metadata for an open image by its id, including dimensions and pixel type." }, name = "fiji_image_details")
	public String getImageDetails(@P("image_id") int imageId) {
		List<ImageDisplay> displays = imageDisplayService.getImageDisplays();
		if (displays == null || displays.isEmpty()) {
			return jsonError("No images are currently open");
		}
		for (ImageDisplay display : displays) {
			if (iPlusHelper.getId(display) != imageId) continue;
			DatasetView datasetView = imageDisplayService.getActiveDatasetView(display);
			if (datasetView == null) continue;
			Dataset dataset = datasetView.getData();
			if (dataset == null) continue;

			JsonObject result = new JsonObject();
			result.addProperty("id", imageId);
			result.addProperty("title", iPlusHelper.getTitle(imageId));
			result.addProperty("pixel_type", dataset.getType().getClass().getSimpleName());

			JsonArray dims = new JsonArray();
			for (int i = 0; i < dataset.numDimensions(); i++) {
				JsonObject dimObj = new JsonObject();
				try {
					AxisType axisType = dataset.axis(i).type();
					dimObj.addProperty("type", axisType != null ? axisType.getLabel() : "Unknown");
				}
				catch (Exception e) {
					dimObj.addProperty("type", "Dim" + i);
				}
				dimObj.addProperty("length", dataset.dimension(i));
				dims.add(dimObj);
			}
			result.add("dimensions", dims);
			return result.toString();
		}
		return jsonError("No open image found with id: " + imageId);
	}
}
