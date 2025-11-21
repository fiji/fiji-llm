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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import sc.fiji.llm.context.AbstractContextItem;

/**
 * Represents an image context item that can be added to the chat. Contains
 * metadata about an image/dataset along with a description for the LLM. NB: we
 * are not converting to dev.langchain4j.data.image.Image due to the requirement
 * of using vision-capable models
 */
public class ImageMetaContextItem extends AbstractContextItem {

	private final String imageName;
	private final List<Dimension> dimensions;
	private final String pixelType;

	/**
	 * Creates an image context item with basic metadata.
	 *
	 * @param imageName the name of the image/dataset
	 */
	public ImageMetaContextItem(String imageName) {
		this(imageName, Collections.emptyList(), "");
	}

	/**
	 * Creates an image context item with detailed metadata.
	 *
	 * @param imageName the name of the image/dataset
	 * @param dimensions list of dimensions with their types, lengths, and
	 *          ordering
	 * @param pixelType the pixel type (e.g., "uint8", "uint16", "float32")
	 */
	public ImageMetaContextItem(String imageName, List<Dimension> dimensions,
		String pixelType)
	{
		super("Image", imageName);
		this.imageName = imageName;
		this.dimensions = dimensions != null ? new ArrayList<>(dimensions)
			: new ArrayList<>();
		this.pixelType = pixelType != null ? pixelType : "";
	}

	public String getImageName() {
		return imageName;
	}

	public List<Dimension> getDimensions() {
		return Collections.unmodifiableList(dimensions);
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
		return Objects.equals(imageName, other.imageName) && Objects.equals(
			getType(), other.getType());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getType(), imageName);
	}

	@Override
	public String toString() {
		final JsonObject obj = new JsonObject();
		obj.addProperty("type", getType());
		obj.addProperty("name", imageName);

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
			obj.addProperty("pixelType", pixelType);
		}

		return new Gson().toJson(obj);
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
