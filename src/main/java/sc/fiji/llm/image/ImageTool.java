/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
@Plugin(type = AiToolPlugin.class)
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
