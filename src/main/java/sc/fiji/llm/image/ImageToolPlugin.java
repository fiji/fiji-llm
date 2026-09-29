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

package sc.fiji.llm.image;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.swing.SwingUtilities;

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

	@Tool(value = { "List all currently open and visible images, including each image's id, title, and whether it is the active image." }, name = "fiji_image_list")
	public String listImages() {
		try {
			JsonArray images = new JsonArray();
			List<Integer> ids = imageJ1HelperService.getImageIds();
			for (Integer id : ids) {
				if (imageJ1HelperService.isImageVisible(id)) {
					JsonObject imageJson = new JsonObject();
					imageJson.addProperty("image_id", id);
					imageJson.addProperty("title", imageJ1HelperService.getImageTitle(id));
					imageJson.addProperty("active", isActiveImage(id));
					images.add(imageJson);
				}
			}
			return jsonProp("open_images", images).toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_image_list: " + e.getMessage());
		}
	}

	@Tool(value = { "Select a specific image as active. Commands and tools operating on an unspecified image use the active image." }, name = "fiji_image_activate")
	public String activateImage(@P(name = "image_id", value = "Image ID from fiji_image_list") final int imageId) {
		try {
			final String[] result = new String[1];
			final Runnable activate = () -> result[0] = performActivateImage(imageId);
			if (SwingUtilities.isEventDispatchThread()) {
				activate.run();
			}
			else {
				SwingUtilities.invokeAndWait(activate);
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_image_activate: " + e.getMessage());
		}
	}

	private String performActivateImage(final int imageId) {
		if (!imageJ1HelperService.isImageVisible(imageId)) {
			return jsonError("No visible open image found with id: " + imageId,
				ErrorOptions.withTool("fiji_image_list"));
		}

		final Optional<ImageDisplay> display = findImageDisplay(imageDisplayService
			.getImageDisplays(), imageId);
		if (display.isEmpty()) {
			return jsonError("No open image found with id: " + imageId,
				ErrorOptions.withTool("fiji_image_list"));
		}

		final ImageDisplay target = display.get();
		imageDisplayService.getDisplayService().setActiveDisplay(target);
		imageJ1HelperService.getIJ1Helper().ifPresent(helper -> helper
			.syncActiveImage(target));

		if (!isActiveImage(imageId)) {
			return jsonError("Unable to activate image with id: " + imageId);
		}

		final JsonObject activatedImage = new JsonObject();
		activatedImage.addProperty("image_id", imageId);
		activatedImage.addProperty("title", imageJ1HelperService.getImageTitle(
			imageId));
		activatedImage.addProperty("active", true);
		return jsonProp("activated_image", activatedImage).toString();
	}

	@Tool(value = { "Return metadata for an open image, including title, pixel type, dimensions, and whether it is the active image." }, name = "fiji_image_details")
	public String getImageDetails(@P(name = "image_id", value = "Image ID from fiji_image_list") int imageId) {
		try {
			final List<ImageDisplay> displays = imageDisplayService.getImageDisplays();
			final Optional<ImageDisplay> display = findImageDisplay(displays, imageId);
			if (display.isPresent()) {
				final DatasetView datasetView = imageDisplayService.getActiveDatasetView(display
					.get());
				if (datasetView == null) return imageNotFoundError(imageId);
				final Dataset dataset = datasetView.getData();
				if (dataset == null) return imageNotFoundError(imageId);

				JsonObject result = new JsonObject();
				result.addProperty("image_id", imageId);
				result.addProperty("title", imageJ1HelperService.getImageTitle(imageId));
				result.addProperty("active", isActiveImage(imageId));
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
			return imageNotFoundError(imageId);
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_image_details: " + e.getMessage());
		}
	}

	@Tool(value = { "Return a minimal rendering of an open image for a quick visual peek without ROIs, overlays, or render metadata. Use this only when Fiji display adjustments and annotation context are not important." }, name = "fiji_image_preview")
	public Content viewImagePreview(@P(name = "image_id", value = "Image ID from fiji_image_list") int imageId) {
		return renderImage(imageId, new ImageRenderOptions(), false,
			"fiji_image_preview").get(0);
	}

	@Tool(value = { "Return a faithful rendering of an open image for analysis, preserving Fiji display adjustments such as LUTs and including visible ROIs and overlays. Use this by default for image interpretation or whenever display context matters; the result also includes render metadata." }, name = "fiji_image_view")
	public List<Content> viewImage(@P(name = "image_id", value = "Image ID from fiji_image_list") int imageId) {
		return renderImage(imageId, new ImageRenderOptions(
			ImageRenderOptions.DEFAULT_MAX_DIMENSION, true, true), true,
			"fiji_image_view");
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
			return textContents(imageNotFoundError(imageId));
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

	private String imageNotFoundError(final int imageId) {
		return jsonError("No open image found with id: " + imageId,
			ErrorOptions.withTool("fiji_image_list"));
	}

	private boolean isActiveImage(final int imageId) {
		final ImageDisplay activeDisplay = imageDisplayService.getActiveImageDisplay();
		return activeDisplay != null && imageJ1HelperService.getImageId(activeDisplay) ==
			imageId;
	}

	private static List<Content> textContents(final String text) {
		return List.of(TextContent.from(text));
	}
}
