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

package sc.fiji.llm.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.data.ImageJ1HelperService;
import sc.fiji.llm.data.ImageJ1HelperService.ResultsTableState;
import sc.fiji.llm.log.ImageJLogUtils;
import sc.fiji.llm.log.SciJavaLogUtils;
import sc.fiji.llm.ui.AWTDialogUtils;

/** Captures lightweight before/after state around an application operation. */
@Plugin(type = Service.class, priority = Priority.HIGH)
public final class ExecutionEnvironmentSnapshotService extends AbstractService
	implements ImageJService
{

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private LogService logService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	/** Starts a capture without changing Fiji state. */
	public EnvironmentCapture capture() {
		return new EnvironmentCapture(snapshot(), SciJavaLogUtils.capture(logService));
	}

	private EnvironmentSnapshot snapshot() {
		final List<ImageState> images = snapshotImages();
		final ImageState activeImage = snapshotActiveImage();
		return new EnvironmentSnapshot(images, activeImage, snapshotResultsTable(),
			AWTDialogUtils.getVisibleDialogs(), ImageJLogUtils.getLog(legacyService));
	}

	private List<ImageState> snapshotImages() {
		final List<ImageState> images = new ArrayList<>();
		final List<ImageDisplay> displays = imageDisplayService.getImageDisplays();
		if (displays == null) return images;
		for (final ImageDisplay display : displays) {
			final ImageState image = snapshotImage(display);
			if (image != null) images.add(image);
		}
		return images;
	}

	private ImageState snapshotActiveImage() {
		return snapshotImage(imageDisplayService.getActiveImageDisplay());
	}

	private ImageState snapshotImage(final ImageDisplay display) {
		if (display == null) return null;
		try {
			final DatasetView view = imageDisplayService.getActiveDatasetView(display);
			final Dataset dataset = view == null ? null : view.getData();
			if (dataset == null) return null;

			final int id = imageJ1HelperService.getImageId(display);
			final String title = id >= 0 ? imageJ1HelperService.getImageTitle(id) : dataset
				.getName();
			final List<Long> dimensions = new ArrayList<>();
			for (int i = 0; i < dataset.numDimensions(); i++) {
				dimensions.add(dataset.dimension(i));
			}
			final String pixelType = dataset.getType() == null ? "" : dataset.getType()
				.getClass().getSimpleName();
			return new ImageState(id, title, pixelType, dimensions);
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	private ResultsState snapshotResultsTable() {
		final ResultsTableState table = imageJ1HelperService.getResultsTableState();
		return new ResultsState(table.isPresent(), table.getRowCount(), table
			.getColumnHeadings());
	}

	public final class EnvironmentCapture implements AutoCloseable {

		private final EnvironmentSnapshot before;
		private final SciJavaLogUtils.LogCapture scijavaCapture;
		private boolean closed;

		private EnvironmentCapture(final EnvironmentSnapshot before,
			final SciJavaLogUtils.LogCapture scijavaCapture)
		{
			this.before = before;
			this.scijavaCapture = scijavaCapture;
		}

		/** Returns the current impact without stopping log capture. */
		public synchronized EnvironmentImpact current() {
			return impact(snapshot(), scijavaCapture.getLogs().getText());
		}

		/** Returns the final impact and stops SciJava log capture. */
		public synchronized EnvironmentImpact finish() {
			try {
				return current();
			}
			finally {
				close();
			}
		}

		@Override
		public synchronized void close() {
			if (!closed) {
				closed = true;
				scijavaCapture.close();
			}
		}

		private EnvironmentImpact impact(final EnvironmentSnapshot after,
			final String scijavaLog)
		{
			return new EnvironmentImpact(before, after, scijavaLog);
		}
	}

	private static final class EnvironmentSnapshot {

		private final List<ImageState> images;
		private final ImageState activeImage;
		private final ResultsState resultsTable;
		private final List<AWTDialogUtils.DialogInfo> dialogs;
		private final ImageJLogUtils.ImageJLog imageJLog;

		private EnvironmentSnapshot(final List<ImageState> images,
			final ImageState activeImage, final ResultsState resultsTable,
			final List<AWTDialogUtils.DialogInfo> dialogs,
			final ImageJLogUtils.ImageJLog imageJLog)
		{
			this.images = Collections.unmodifiableList(new ArrayList<>(images));
			this.activeImage = activeImage;
			this.resultsTable = resultsTable;
			this.dialogs = Collections.unmodifiableList(new ArrayList<>(dialogs));
			this.imageJLog = imageJLog;
		}

		private JsonObject toJson() {
			final JsonObject result = new JsonObject();
			final JsonArray imagesJson = new JsonArray();
			for (final ImageState image : images) imagesJson.add(image.toJson());
			result.add("images", imagesJson);
			result.add("active_image", activeImage == null ? JsonNull.INSTANCE : activeImage
				.toJson());
			result.add("results_table", resultsTable.toJson());
			result.add("dialogs", dialogsJson(dialogs));
			result.addProperty("imagej_log_open", imageJLog.isOpen());
			return result;
		}
	}

	public static final class EnvironmentImpact {

		private final EnvironmentSnapshot before;
		private final EnvironmentSnapshot after;
		private final String scijavaLog;

		private EnvironmentImpact(final EnvironmentSnapshot before,
			final EnvironmentSnapshot after, final String scijavaLog)
		{
			this.before = before;
			this.after = after;
			this.scijavaLog = scijavaLog == null ? "" : scijavaLog;
		}

		public List<AWTDialogUtils.DialogInfo> getNewModalDialogs() {
			final List<AWTDialogUtils.DialogInfo> result = new ArrayList<>();
			for (final AWTDialogUtils.DialogInfo dialog : after.dialogs) {
				if (dialog.isModal() && !containsDialog(before.dialogs, dialog)) result.add(
					dialog);
			}
			return result;
		}

		public String getImageJLog() {
			return after.imageJLog.deltaFrom(before.imageJLog).getText();
		}

		public String getSciJavaLog() {
			return scijavaLog;
		}

		public JsonObject toJson() {
			final JsonObject result = new JsonObject();
			result.add("before", before.toJson());
			result.add("after", after.toJson());
			result.add("changes", changesJson());
			result.addProperty("imagej_log", getImageJLog());
			result.addProperty("scijava_log", scijavaLog);
			return result;
		}

		private JsonObject changesJson() {
			final JsonObject changes = new JsonObject();
			final JsonArray opened = new JsonArray();
			final JsonArray closed = new JsonArray();
			final JsonArray changed = new JsonArray();
			for (final ImageState image : after.images) {
				final ImageState previous = findImage(before.images, image.key());
				if (previous == null) opened.add(image.toJson());
				else if (!previous.sameAs(image)) changed.add(image.toJson());
			}
			for (final ImageState image : before.images) {
				if (findImage(after.images, image.key()) == null) closed.add(image.toJson());
			}
			changes.add("images_opened", opened);
			changes.add("images_closed", closed);
			changes.add("images_changed", changed);
			changes.addProperty("active_image_changed", !sameImageKey(before.activeImage,
				after.activeImage));
			changes.addProperty("results_table_changed", !before.resultsTable.equals(after
				.resultsTable));
			changes.add("dialogs_opened", dialogsJson(newDialogs(before.dialogs, after
				.dialogs)));
			changes.add("dialogs_closed", dialogsJson(newDialogs(after.dialogs, before
				.dialogs)));
			return changes;
		}
	}

	private static final class ImageState {

		private final int id;
		private final String title;
		private final String pixelType;
		private final List<Long> dimensions;

		private ImageState(final int id, final String title, final String pixelType,
			final List<Long> dimensions)
		{
			this.id = id;
			this.title = title == null ? "" : title;
			this.pixelType = pixelType == null ? "" : pixelType;
			this.dimensions = Collections.unmodifiableList(new ArrayList<>(dimensions));
		}

		private String key() {
			return id >= 0 ? "id:" + id : "title:" + title;
		}

		private boolean sameAs(final ImageState other) {
			return key().equals(other.key()) && title.equals(other.title) && pixelType.equals(
				other.pixelType) && dimensions.equals(other.dimensions);
		}

		private JsonObject toJson() {
			final JsonObject result = new JsonObject();
			if (id >= 0) result.addProperty("id", id);
			result.addProperty("title", title);
			result.addProperty("pixel_type", pixelType);
			final JsonArray dimensionsJson = new JsonArray();
			for (final long dimension : dimensions) dimensionsJson.add(dimension);
			result.add("dimensions", dimensionsJson);
			return result;
		}
	}

	private static final class ResultsState {

		private final boolean present;
		private final int rows;
		private final String headings;

		private ResultsState(final boolean present, final int rows,
			final String headings)
		{
			this.present = present;
			this.rows = rows;
			this.headings = headings == null ? "" : headings;
		}

		private JsonObject toJson() {
			final JsonObject result = new JsonObject();
			result.addProperty("present", present);
			result.addProperty("rows", rows);
			result.addProperty("column_headings", headings);
			return result;
		}

		@Override
		public boolean equals(final Object object) {
			if (!(object instanceof ResultsState other)) return false;
			return present == other.present && rows == other.rows && headings.equals(other
				.headings);
		}

		@Override
		public int hashCode() {
			return Objects.hash(present, rows, headings);
		}
	}

	private static ImageState findImage(final List<ImageState> images,
		final String key)
	{
		for (final ImageState image : images) if (image.key().equals(key)) return image;
		return null;
	}

	private static boolean sameImageKey(final ImageState first,
		final ImageState second)
	{
		if (first == null || second == null) return first == second;
		return first.key().equals(second.key());
	}

	private static boolean containsDialog(
		final List<AWTDialogUtils.DialogInfo> dialogs,
		final AWTDialogUtils.DialogInfo candidate)
	{
		for (final AWTDialogUtils.DialogInfo dialog : dialogs) {
			if (dialogKey(dialog).equals(dialogKey(candidate))) return true;
		}
		return false;
	}

	private static List<AWTDialogUtils.DialogInfo> newDialogs(
		final List<AWTDialogUtils.DialogInfo> baseline,
		final List<AWTDialogUtils.DialogInfo> current)
	{
		final List<AWTDialogUtils.DialogInfo> result = new ArrayList<>();
		for (final AWTDialogUtils.DialogInfo dialog : current) {
			if (!containsDialog(baseline, dialog)) result.add(dialog);
		}
		return result;
	}

	private static String dialogKey(final AWTDialogUtils.DialogInfo dialog) {
		return dialog.getTitle() + "\u0000" + dialog.getClassName() + "\u0000" + dialog
			.getModalityType();
	}

	private static JsonArray dialogsJson(
		final List<AWTDialogUtils.DialogInfo> dialogs)
	{
		final JsonArray result = new JsonArray();
		for (final AWTDialogUtils.DialogInfo dialog : dialogs) {
			final JsonObject dialogJson = new JsonObject();
			dialogJson.addProperty("title", dialog.getTitle());
			dialogJson.addProperty("class_name", dialog.getClassName());
			dialogJson.addProperty("visible", dialog.isVisible());
			dialogJson.addProperty("active", dialog.isActive());
			dialogJson.addProperty("modal", dialog.isModal());
			dialogJson.addProperty("modality_type", dialog.getModalityType());
			dialogJson.addProperty("owner_name", dialog.getOwnerName());
			final JsonArray messages = new JsonArray();
			for (final String message : dialog.getMessages()) messages.add(message);
			dialogJson.add("messages", messages);
			final JsonArray buttons = new JsonArray();
			for (final AWTDialogUtils.ButtonInfo button : dialog.getButtons()) {
				final JsonObject buttonJson = new JsonObject();
				buttonJson.addProperty("text", button.getText());
				buttonJson.addProperty("action_command", button.getActionCommand());
				buttonJson.addProperty("enabled", button.isEnabled());
				buttonJson.addProperty("visible", button.isVisible());
				buttons.add(buttonJson);
			}
			dialogJson.add("buttons", buttons);
			result.add(dialogJson);
		}
		return result;
	}
}
