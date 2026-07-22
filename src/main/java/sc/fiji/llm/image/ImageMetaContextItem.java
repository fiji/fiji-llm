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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sc.fiji.llm.context.AbstractContextItem;

/**
 * Represents an image context item that can be added to the chat. Contains
 * metadata about an image/dataset along with a description for the LLM. NB: we
 * are not converting to dev.langchain4j.data.image.Image due to the requirement
 * of using vision-capable models
 */
public class ImageMetaContextItem extends AbstractContextItem {

	private final String imageTitle;
	private final int imageId;
	private final List<Dimension> dimensions;
	private final String pixelType;

	/**
	 * Creates an image context item with detailed metadata.
	 *
	 * @param imageName the name of the image/dataset
	 * @param imageId the id of this image
	 * @param dimensions list of dimensions with their types, lengths, and
	 *          ordering
	 * @param pixelType the pixel type (e.g., "uint8", "uint16", "float32")
	 */
	public ImageMetaContextItem(String imageName, int imageId, List<Dimension> dimensions,
		String pixelType)
	{
		super("Image", imageName);
		this.imageTitle = imageName;
		this.imageId = imageId;
		this.dimensions = dimensions != null ? Collections.unmodifiableList(dimensions)
			: Collections.emptyList();
		this.pixelType = pixelType != null ? pixelType : "";
	}

	public String getTitle() {
		return imageTitle;
	}

	public int getId() {
		return imageId;
	}

	public List<Dimension> getDimensions() {
		return dimensions;
	}

	public String getPixelType() {
		return pixelType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ImageMetaContextItem other = (ImageMetaContextItem) obj;
		return Objects.equals(imageTitle, other.imageTitle) && (imageId == other.imageId)
			&& Objects.equals( getType(), other.getType());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getType(), imageTitle);
	}

	@Override
	public JsonElement toJson() {
		final JsonObject obj = new JsonObject();
		obj.addProperty("type", getType());
		obj.addProperty("title", imageTitle);
		obj.addProperty("id", imageId);

		if (!dimensions.isEmpty()) {
			final JsonArray dimensionsArray = new JsonArray();
			for (final Dimension dim : dimensions) {
				final JsonObject dimObj = new JsonObject();
				dimObj.addProperty("type", dim.getType());
				dimObj.addProperty("length", dim.getLength());
				dimensionsArray.add(dimObj);
			}
			obj.add("dimensions", dimensionsArray);
		}

		if (!pixelType.isEmpty()) {
			obj.addProperty("pixel_type", pixelType);
		}

		return obj;
	}

	/**
	 * Represents a single dimension of a dataset with its type and length.
	 */
	public static class Dimension {

		private final String type;
		private final long length;

		public Dimension(String type, long length) {
			this.type = type;
			this.length = length;
		}

		public String getType() {
			return type;
		}

		public long getLength() {
			return length;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			final Dimension other = (Dimension) obj;
			return Objects.equals(type, other.type) && length == other.length;
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, length);
		}

		@Override
		public String toString() {
			return String.format("%s[%d]", type, length);
		}
	}
}
