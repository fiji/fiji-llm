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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import sc.fiji.llm.data.ImageJ1HelperService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/**
 * AI tool collection for querying open images in Fiji.
 */
@Plugin(type = AiToolPlugin.class)
public class ImageToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ImageRenderingService imageRenderingService;

	public ImageToolPlugin() {
		super(ImageToolPlugin.class);
	}

	@Override
	public String getName() {
		return "Image Tools";
	}

	@Override
	public String getUsage() {
		return """
The fiji_image_* tools query images currently open in Fiji and can return a
rendered image when requested. The fiji_image_view_annotated tool includes
the visible active ROI and image overlays in the rendered image when available.
""";
	}

	@Tool(value = { "List all currently open and visible images, including each image's id and title." }, name = "fiji_image_list")
	public String listImages() {
		try {
			JsonArray images = new JsonArray();
			List<Integer> ids = imageJ1HelperService.getImageIds();
			for (Integer id : ids) {
				if (imageJ1HelperService.isImageVisible(id)) {
					JsonObject imageJson = new JsonObject();
					imageJson.addProperty("id", id);
					imageJson.addProperty("title", imageJ1HelperService.getImageTitle(id));
					images.add(imageJson);
				}
			}
			return jsonProp("open_images", images).toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_image_list: " + e.getMessage());
		}
	}

	@Tool(value = { "For an open image specified by image id, return metadata including title, pixel type, and dimensions. fiji_image_list can be used to find image id's." }, name = "fiji_image_details")
	public String getImageDetails(@P("image_id") int imageId) {
		try {
			final List<ImageDisplay> displays = imageDisplayService.getImageDisplays();
			final Optional<ImageDisplay> display = findImageDisplay(displays, imageId);
			if (display.isPresent()) {
				final DatasetView datasetView = imageDisplayService.getActiveDatasetView(display
					.get());
				if (datasetView == null) return jsonError("No open image found with id: " +
					imageId);
				final Dataset dataset = datasetView.getData();
				if (dataset == null) return jsonError("No open image found with id: " +
					imageId);

				JsonObject result = new JsonObject();
				result.addProperty("id", imageId);
				result.addProperty("title", imageJ1HelperService.getImageTitle(imageId));
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
			if (displays == null || displays.isEmpty()) return jsonError(
				"No images are currently open");
			return jsonError("No open image found with id: " + imageId);
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_image_details: " + e.getMessage());
		}
	}

	@Tool(value = { "For an open image specified by image id, return its rendered image content. fiji_image_list can be used to find image ids." }, name = "fiji_image_view")
	public Content viewImage(@P("image_id") int imageId) {
		return renderImage(imageId, new ImageRenderOptions(), false,
			"fiji_image_view").get(0);
	}

	@Tool(value = { "For an open image specified by image id, return its rendered image content with any visible annotations (e.g. ROIs) included. fiji_image_list can be used to find image ids." }, name = "fiji_image_view_annotated")
	public List<Content> viewImageAnnotated(@P("image_id") int imageId) {
		return renderImage(imageId, new ImageRenderOptions(
			ImageRenderOptions.DEFAULT_MAX_DIMENSION, true, true), true,
			"fiji_image_view_annotated");
	}

	private List<Content> renderImage(final int imageId,
		final ImageRenderOptions options, final boolean includeMetadata,
		final String toolName)
	{
		try {
			final List<ImageDisplay> displays = imageDisplayService.getImageDisplays();
			final Optional<ImageDisplay> display = findImageDisplay(displays, imageId);
			if (display.isPresent()) {
				if (imageRenderingService == null) return textContents(
					jsonError("Image rendering is not available"));
				final Optional<RenderedImageResult> rendered = imageRenderingService
					.render(display.get(), options);
				if (rendered.isEmpty()) return textContents(jsonError(
					"Could not render image with id: " + imageId));
				if (!includeMetadata) return List.of(rendered.get().getImageContent());
				final JsonObject metadata = new JsonObject();
				metadata.add("render_metadata", rendered.get().getMetadata().toJson());
				return List.of(TextContent.from(metadata.toString()), rendered.get()
					.getImageContent());
			}
			if (displays == null || displays.isEmpty()) return textContents(
				jsonError("No images are currently open"));
			return textContents(jsonError("No open image found with id: " + imageId));
		}
		catch (IOException | RuntimeException e) {
			return textContents(jsonError("Failed to run " + toolName + ": " + e
				.getMessage()));
		}
	}

	private Optional<ImageDisplay> findImageDisplay(final List<ImageDisplay> displays,
		final int imageId)
	{
		if (displays != null) for (final ImageDisplay display : displays) {
			if (imageJ1HelperService.getImageId(display) == imageId) return Optional.of(
				display);
		}
		return imageJ1HelperService.getOrCreateImageDisplay(imageId);
	}

	private static List<Content> textContents(final String text) {
		return List.of(TextContent.from(text));
	}
}
