package sc.fiji.llm.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import sc.fiji.llm.chat.AbstractContextItem;

/**
 * Represents an image context item that can be added to the chat.
 * Contains metadata about an image/dataset along with a description for the LLM.
 *
 * NB: we are not converting to dev.langchain4j.data.image.Image due to the
 * requirement of using vision-capable models
 */
public class ImageContextItem extends AbstractContextItem {

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

	private final String imageName;
	private final List<Dimension> dimensions;
	private final String pixelType;

	/**
	 * Creates an image context item with basic metadata.
	 *
	 * @param imageName the name of the image/dataset
	 */
	public ImageContextItem(String imageName) {
		this(imageName, Collections.emptyList(), "");
	}

	/**
	 * Creates an image context item with detailed metadata.
	 *
	 * @param imageName the name of the image/dataset
	 * @param dimensions list of dimensions with their types, lengths, and ordering
	 * @param pixelType the pixel type (e.g., "uint8", "uint16", "float32")
	 */
	public ImageContextItem(String imageName, List<Dimension> dimensions,
			String pixelType) {
		super("Image", imageName);
		this.imageName = imageName;
		this.dimensions = dimensions != null ? new ArrayList<>(dimensions) : new ArrayList<>();
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
		final ImageContextItem other = (ImageContextItem) obj;
		return Objects.equals(imageName, other.imageName) &&
				Objects.equals(getType(), other.getType());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getType(), imageName);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Image: ").append(imageName).append("\n");

		if (!dimensions.isEmpty()) {
			sb.append("Dimensions: ");
			for (int i = 0; i < dimensions.size(); i++) {
				if (i > 0) {
					sb.append(" × ");
				}
				final Dimension dim = dimensions.get(i);
				sb.append(dim.getLength()).append(" (").append(dim.getType()).append(")");
			}
			sb.append("\n");
		}

		if (pixelType != null && !pixelType.isEmpty()) {
			sb.append("Pixel Type: ").append(pixelType).append("\n");
		}

		return sb.toString();
	}
}
