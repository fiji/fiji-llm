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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import org.scijava.Priority;
import org.scijava.console.ConsoleService;
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
import net.imagej.ImgPlus;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyService;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
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

	private static final long MAX_BULK_HASH_BYTES = 128L * 1024 * 1024;
	private static final long MAX_CURSOR_HASH_PIXELS = 1_000_000;
	private static final String PIXEL_HASH_ALGORITHM = "SHA-256";

	public enum PixelChangeTracking {
		NONE,
		FINAL_SHA256
	}

	private enum PixelSnapshotMode {
		NOT_REQUESTED("not_requested", PixelHashStatus.NOT_REQUESTED),
		DEFERRED("deferred", PixelHashStatus.DEFERRED),
		CAPTURED("captured", PixelHashStatus.UNAVAILABLE);

		private final String value;
		private final PixelHashStatus hashStatus;

		PixelSnapshotMode(final String value, final PixelHashStatus hashStatus) {
			this.value = value;
			this.hashStatus = hashStatus;
		}

		private PixelHashStatus hashStatus() {
			return hashStatus;
		}
	}

	private enum PixelHashStatus {
		NOT_REQUESTED("not_requested"),
		DEFERRED("deferred"),
		CAPTURED("captured"),
		SAMPLED("sampled"),
		SKIPPED("skipped"),
		UNAVAILABLE("unavailable");

		private final String value;

		PixelHashStatus(final String value) {
			this.value = value;
		}
	}

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private LogService logService;

	@Parameter
	private ConsoleService consoleService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	/** Starts a capture without changing Fiji state. */
	public EnvironmentCapture capture() {
		return capture(PixelChangeTracking.NONE);
	}

	/** Starts a capture with optional final pixel-content comparison. */
	public EnvironmentCapture capture(final PixelChangeTracking pixelChangeTracking) {
		Objects.requireNonNull(pixelChangeTracking, "pixelChangeTracking");
		final PixelSnapshotMode snapshotMode = pixelChangeTracking ==
			PixelChangeTracking.FINAL_SHA256 ? PixelSnapshotMode.CAPTURED :
			PixelSnapshotMode.NOT_REQUESTED;
		return new EnvironmentCapture(snapshot(snapshotMode), pixelChangeTracking,
			SciJavaLogUtils.capture(logService, consoleService));
	}

	private EnvironmentSnapshot snapshot(final PixelSnapshotMode pixelSnapshotMode) {
		final List<ImageState> images = snapshotImages(pixelSnapshotMode);
		final ImageState activeImage = snapshotActiveImage(images);
		return new EnvironmentSnapshot(images, activeImage, snapshotResultsTable(),
			AWTDialogUtils.getVisibleDialogs(), ImageJLogUtils.getLog(legacyService),
			pixelSnapshotMode);
	}

	private List<ImageState> snapshotImages(final PixelSnapshotMode pixelSnapshotMode) {
		final List<ImageState> images = new ArrayList<>();
		final List<ImageDisplay> displays = imageDisplayService.getImageDisplays();
		if (displays == null) return images;
		for (final ImageDisplay display : displays) {
			final ImageState image = snapshotImage(display, pixelSnapshotMode);
			if (image != null) images.add(image);
		}
		return images;
	}

	private ImageState snapshotActiveImage(final List<ImageState> images) {
		final ImageState activeImage = snapshotImage(imageDisplayService
			.getActiveImageDisplay(), PixelSnapshotMode.NOT_REQUESTED);
		return activeImage == null ? null : findImage(images, activeImage.key());
	}

	private ImageState snapshotImage(final ImageDisplay display,
		final PixelSnapshotMode pixelSnapshotMode)
	{
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
			String pixelHash = null;
			String pixelHashEncoding = null;
			String pixelHashReason = null;
			PixelHashStatus pixelHashStatus = pixelSnapshotMode.hashStatus();
			if (pixelSnapshotMode == PixelSnapshotMode.CAPTURED) {
				try {
					final PixelHashResult result = pixelHash(dataset);
					pixelHash = result.hash;
					pixelHashEncoding = result.encoding;
					pixelHashReason = result.reason;
					pixelHashStatus = result.status;
				}
				catch (RuntimeException e) {
					pixelHashStatus = PixelHashStatus.UNAVAILABLE;
					pixelHashReason = "runtime_error";
				}
			}
			return new ImageState(id, title, pixelType, dimensions, pixelHash,
				pixelHashEncoding, pixelHashReason, pixelHashStatus);
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	private static PixelHashResult pixelHash(final Dataset dataset) {
		// TODO: Investigate whether ImageJ Ops provides a canonical bounded content digest.
		final ImgPlus<? extends RealType<?>> imgPlus = dataset.getImgPlus();
		if (imgPlus == null || imgPlus.getImg() == null) return PixelHashResult.skipped(
			"no_img");
		final Img<? extends RealType<?>> img = imgPlus.getImg();
		if (img instanceof AbstractCellImg<?, ?, ?, ?>) return PixelHashResult.skipped(
			"lazy_container");
		if (img instanceof ArrayImg<?, ?> arrayImg) {
			final ArrayDataAccess<?> access = (ArrayDataAccess<?>) arrayImg.update(null);
			return nativeStorageHash(dataset, List.of(access.getCurrentStorageArray()));
		}
		if (img instanceof PlanarImg<?, ?> planarImg) {
			final List<Object> storage = new ArrayList<>();
			for (int plane = 0; plane < planarImg.numSlices(); plane++) {
				storage.add(planarImg.getPlane(plane).getCurrentStorageArray());
			}
			return nativeStorageHash(dataset, storage);
		}
		return cursorHash(dataset, img);
	}

	private static PixelHashResult nativeStorageHash(final Dataset dataset,
		final List<Object> storage)
	{
		long totalBytes = 0;
		for (final Object array : storage) {
			final long bytes = nativeStorageBytes(array);
			if (bytes < 0) return PixelHashResult.skipped("unsupported_storage");
			try {
				totalBytes = Math.addExact(totalBytes, bytes);
			}
			catch (final ArithmeticException e) {
				return PixelHashResult.skipped("storage_size_overflow");
			}
		}

		final MessageDigest digest = newDigest();
		updateDigestHeader(digest, dataset, "imglib2-native-storage-v1");
		long remaining = MAX_BULK_HASH_BYTES;
		for (final Object array : storage) {
			if (remaining == 0) break;
			remaining -= updateNativeStorage(digest, array, remaining);
		}
		final PixelHashStatus status = totalBytes > MAX_BULK_HASH_BYTES ?
			PixelHashStatus.SAMPLED : PixelHashStatus.CAPTURED;
		final String reason = status == PixelHashStatus.SAMPLED ? "bulk_size_limit" : null;
		return PixelHashResult.of(HexFormat.of().formatHex(digest.digest()), status,
			"imglib2-native-storage-v1", reason);
	}

	private static PixelHashResult cursorHash(final Dataset dataset,
		final Img<? extends RealType<?>> img)
	{
		final MessageDigest digest = newDigest();
		updateDigestHeader(digest, dataset, "imglib2-real-values-v1");
		final byte[] longBytes = new byte[Long.BYTES];
		final Cursor<? extends RealType<?>> cursor = img.cursor();
		long pixels = 0;
		while (pixels < MAX_CURSOR_HASH_PIXELS && cursor.hasNext()) {
			final RealType<?> value = cursor.next();
			final long bits = value instanceof IntegerType<?> integer ? integer
				.getIntegerLong() : Double.doubleToLongBits(value.getRealDouble());
			updateLong(digest, bits, longBytes);
			pixels++;
		}
		final boolean sampled = cursor.hasNext();
		return PixelHashResult.of(HexFormat.of().formatHex(digest.digest()), sampled ?
			PixelHashStatus.SAMPLED : PixelHashStatus.CAPTURED, "imglib2-real-values-v1",
			sampled ? "cursor_pixel_limit" : null);
	}

	private static MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance(PIXEL_HASH_ALGORITHM);
		}
		catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException(PIXEL_HASH_ALGORITHM + " is not available", e);
		}
	}

	private static void updateDigestHeader(final MessageDigest digest,
		final Dataset dataset, final String encoding)
	{
		updateString(digest, "fiji-llm-pixel-hash-v2");
		updateString(digest, encoding);
		final byte[] longBytes = new byte[Long.BYTES];
		updateLong(digest, dataset.numDimensions(), longBytes);
		for (int dimension = 0; dimension < dataset.numDimensions(); dimension++) {
			updateLong(digest, dataset.dimension(dimension), longBytes);
		}
		final String pixelType = dataset.getType() == null ? "" : dataset.getType()
			.getClass().getName();
		updateString(digest, pixelType);
	}

	private static void updateString(final MessageDigest digest, final String value) {
		final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		final byte[] length = new byte[Long.BYTES];
		updateLong(digest, bytes.length, length);
		digest.update(bytes);
	}

	private static long nativeStorageBytes(final Object array) {
		if (array instanceof byte[] values) return values.length;
		if (array instanceof boolean[] values) return values.length;
		if (array instanceof char[] values) return bytesFor(values.length, Character.BYTES);
		if (array instanceof short[] values) return bytesFor(values.length, Short.BYTES);
		if (array instanceof int[] values) return bytesFor(values.length, Integer.BYTES);
		if (array instanceof float[] values) return bytesFor(values.length, Float.BYTES);
		if (array instanceof long[] values) return bytesFor(values.length, Long.BYTES);
		if (array instanceof double[] values) return bytesFor(values.length, Double.BYTES);
		return -1;
	}

	private static long bytesFor(final int elements, final int bytesPerElement) {
		try {
			return Math.multiplyExact((long) elements, bytesPerElement);
		}
		catch (final ArithmeticException e) {
			return -1;
		}
	}

	private static long updateNativeStorage(final MessageDigest digest,
		final Object array, final long maxBytes)
	{
		if (array instanceof byte[] values) {
			final int count = (int) Math.min(values.length, maxBytes);
			digest.update(values, 0, count);
			return count;
		}
		if (array instanceof boolean[] values) {
			final int count = (int) Math.min(values.length, maxBytes);
			final byte[] bytes = new byte[count];
			for (int i = 0; i < count; i++) bytes[i] = (byte) (values[i] ? 1 : 0);
			digest.update(bytes);
			return count;
		}
		if (array instanceof char[] values) return updatePrimitiveStorage(digest, values,
			maxBytes, Character.BYTES, (buffer, offset, length) -> buffer.asCharBuffer().put(
				values, offset, length));
		if (array instanceof short[] values) return updatePrimitiveStorage(digest, values,
			maxBytes, Short.BYTES, (buffer, offset, length) -> buffer.asShortBuffer().put(
				values, offset, length));
		if (array instanceof int[] values) return updatePrimitiveStorage(digest, values,
			maxBytes, Integer.BYTES, (buffer, offset, length) -> buffer.asIntBuffer().put(
				values, offset, length));
		if (array instanceof float[] values) return updatePrimitiveStorage(digest, values,
			maxBytes, Float.BYTES, (buffer, offset, length) -> buffer.asFloatBuffer().put(
				values, offset, length));
		if (array instanceof long[] values) return updatePrimitiveStorage(digest, values,
			maxBytes, Long.BYTES, (buffer, offset, length) -> buffer.asLongBuffer().put(
				values, offset, length));
		if (array instanceof double[] values) return updatePrimitiveStorage(digest, values,
			maxBytes, Double.BYTES, (buffer, offset, length) -> buffer.asDoubleBuffer().put(
				values, offset, length));
		return 0;
	}

	private static long updatePrimitiveStorage(final MessageDigest digest,
		final Object values, final long maxBytes, final int bytesPerElement,
		final PrimitiveArrayWriter writer)
	{
		final int length = java.lang.reflect.Array.getLength(values);
		final long maxElements = Math.min(length, maxBytes / bytesPerElement);
		final int elementsPerChunk = Math.max(1, 1024 * 1024 / bytesPerElement);
		final byte[] bytes = new byte[Math.min((int) maxElements, elementsPerChunk) *
			bytesPerElement];
		long offset = 0;
		while (offset < maxElements) {
			final int count = (int) Math.min(maxElements - offset, elementsPerChunk);
			final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
			writer.write(buffer, (int) offset, count);
			digest.update(bytes, 0, count * bytesPerElement);
			offset += count;
		}
		return maxElements * bytesPerElement;
	}

	@FunctionalInterface
	private interface PrimitiveArrayWriter {
		void write(ByteBuffer buffer, int offset, int length);
	}

	private static final class PixelHashResult {

		private final String hash;
		private final String encoding;
		private final String reason;
		private final PixelHashStatus status;

		private PixelHashResult(final String hash, final String encoding,
			final String reason, final PixelHashStatus status)
		{
			this.hash = hash;
			this.encoding = encoding;
			this.reason = reason;
			this.status = status;
		}

		private static PixelHashResult of(final String hash,
			final PixelHashStatus status, final String encoding, final String reason)
		{
			return new PixelHashResult(hash, encoding, reason, status);
		}

		private static PixelHashResult skipped(final String reason) {
			return new PixelHashResult(null, null, reason, PixelHashStatus.SKIPPED);
		}
	}

	private static void updateLong(final MessageDigest digest, final long value,
		final byte[] bytes)
	{
		for (int index = 0; index < Long.BYTES; index++) {
			bytes[index] = (byte) (value >>> (Long.BYTES - 1 - index) * Byte.SIZE);
		}
		digest.update(bytes);
	}

	private ResultsState snapshotResultsTable() {
		final ResultsTableState table = imageJ1HelperService.getResultsTableState();
		return new ResultsState(table.isPresent(), table.getRowCount(), table
			.getColumnHeadings());
	}

	public final class EnvironmentCapture implements AutoCloseable {

		private final EnvironmentSnapshot before;
		private final PixelChangeTracking pixelChangeTracking;
		private final SciJavaLogUtils.LogCapture scijavaCapture;
		private boolean closed;

		private EnvironmentCapture(final EnvironmentSnapshot before,
			final PixelChangeTracking pixelChangeTracking,
			final SciJavaLogUtils.LogCapture scijavaCapture)
		{
			this.before = before;
			this.pixelChangeTracking = pixelChangeTracking;
			this.scijavaCapture = scijavaCapture;
		}

		/** Returns the current impact without stopping log capture. */
		public synchronized EnvironmentImpact current() {
			final SciJavaLogUtils.LogMessages logs = scijavaCapture.getLogs();
			final PixelSnapshotMode mode = pixelChangeTracking ==
				PixelChangeTracking.FINAL_SHA256 ? PixelSnapshotMode.DEFERRED :
				PixelSnapshotMode.NOT_REQUESTED;
			return impact(snapshot(mode), logs.getText(), logs.getStdout(), logs.getStderr());
		}

		/** Returns the final impact and stops SciJava log capture. */
		public synchronized EnvironmentImpact finish() {
			try {
				final SciJavaLogUtils.LogMessages logs = scijavaCapture.getLogs();
				final PixelSnapshotMode mode = pixelChangeTracking ==
					PixelChangeTracking.FINAL_SHA256 ? PixelSnapshotMode.CAPTURED :
					PixelSnapshotMode.NOT_REQUESTED;
				return impact(snapshot(mode), logs.getText(), logs.getStdout(), logs
					.getStderr());
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
			final String scijavaLog, final String consoleStdout,
			final String consoleStderr)
		{
			return new EnvironmentImpact(before, after, scijavaLog, consoleStdout,
				consoleStderr);
		}
	}

	private static final class EnvironmentSnapshot {

		private final List<ImageState> images;
		private final ImageState activeImage;
		private final ResultsState resultsTable;
		private final List<AWTDialogUtils.DialogInfo> dialogs;
		private final ImageJLogUtils.ImageJLog imageJLog;
		private final PixelSnapshotMode pixelSnapshotMode;

		private EnvironmentSnapshot(final List<ImageState> images,
			final ImageState activeImage, final ResultsState resultsTable,
			final List<AWTDialogUtils.DialogInfo> dialogs,
			final ImageJLogUtils.ImageJLog imageJLog,
			final PixelSnapshotMode pixelSnapshotMode)
		{
			this.images = Collections.unmodifiableList(new ArrayList<>(images));
			this.activeImage = activeImage;
			this.resultsTable = resultsTable;
			this.dialogs = Collections.unmodifiableList(new ArrayList<>(dialogs));
			this.imageJLog = imageJLog;
			this.pixelSnapshotMode = pixelSnapshotMode;
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
			result.addProperty("pixel_change_tracking", pixelSnapshotMode.value);
			return result;
		}
	}

	public static final class EnvironmentImpact {

		private final EnvironmentSnapshot before;
		private final EnvironmentSnapshot after;
		private final String scijavaLog;
		private final String consoleStdout;
		private final String consoleStderr;

		private EnvironmentImpact(final EnvironmentSnapshot before,
			final EnvironmentSnapshot after, final String scijavaLog,
			final String consoleStdout, final String consoleStderr)
		{
			this.before = before;
			this.after = after;
			this.scijavaLog = scijavaLog == null ? "" : scijavaLog;
			this.consoleStdout = consoleStdout == null ? "" : consoleStdout;
			this.consoleStderr = consoleStderr == null ? "" : consoleStderr;
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

		public String getConsoleStdout() {
			return consoleStdout;
		}

		public String getConsoleStderr() {
			return consoleStderr;
		}

		public JsonObject toJson() {
			final JsonObject result = new JsonObject();
			result.add("before", before.toJson());
			result.add("after", after.toJson());
			result.add("changes", changesJson());
			result.addProperty("imagej_log", getImageJLog());
			result.addProperty("scijava_log", scijavaLog);
			result.addProperty("console_stdout", consoleStdout);
			result.addProperty("console_stderr", consoleStderr);
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
			changes.addProperty("pixel_changes", pixelChangesStatus());
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

		private String pixelChangesStatus() {
			if (after.pixelSnapshotMode == PixelSnapshotMode.NOT_REQUESTED) return
				"not_requested";
			if (after.pixelSnapshotMode == PixelSnapshotMode.DEFERRED) return "deferred";
			if (before.pixelSnapshotMode != PixelSnapshotMode.CAPTURED) return
				"unavailable";

			boolean inconclusive = false;
			for (final ImageState image : after.images) {
				final ImageState previous = findImage(before.images, image.key());
				if (previous == null) continue;
				if (!previous.hasPixelHash() || !image.hasPixelHash()) {
					inconclusive = true;
					continue;
				}
				if (!previous.pixelHash.equals(image.pixelHash)) return "changed";
				if (previous.pixelHashStatus == PixelHashStatus.SAMPLED || image
					.pixelHashStatus == PixelHashStatus.SAMPLED) inconclusive = true;
			}
		return inconclusive ? "inconclusive" : "unchanged";
		}
	}

	private static final class ImageState {

		private final int id;
		private final String title;
		private final String pixelType;
		private final List<Long> dimensions;
		private final String pixelHash;
		private final String pixelHashEncoding;
		private final String pixelHashReason;
		private final PixelHashStatus pixelHashStatus;

		private ImageState(final int id, final String title, final String pixelType,
			final List<Long> dimensions, final String pixelHash,
			final String pixelHashEncoding, final String pixelHashReason,
			final PixelHashStatus pixelHashStatus)
		{
			this.id = id;
			this.title = title == null ? "" : title;
			this.pixelType = pixelType == null ? "" : pixelType;
			this.dimensions = Collections.unmodifiableList(new ArrayList<>(dimensions));
			this.pixelHash = pixelHash;
			this.pixelHashEncoding = pixelHashEncoding;
			this.pixelHashReason = pixelHashReason;
			this.pixelHashStatus = pixelHashStatus;
		}

		private String key() {
			return id >= 0 ? "id:" + id : "title:" + title;
		}

		private boolean sameAs(final ImageState other) {
			return key().equals(other.key()) && title.equals(other.title) && pixelType.equals(
				other.pixelType) && dimensions.equals(other.dimensions) &&
				!hasDifferentPixelHash(other);
		}

		private boolean hasPixelHash() {
			return (pixelHashStatus == PixelHashStatus.CAPTURED || pixelHashStatus ==
				PixelHashStatus.SAMPLED) && pixelHash != null;
		}

		private boolean hasDifferentPixelHash(final ImageState other) {
			return hasPixelHash() && other.hasPixelHash() && !pixelHash.equals(
				other.pixelHash);
		}

		private JsonObject toJson() {
			final JsonObject result = new JsonObject();
			if (id >= 0) result.addProperty("id", id);
			result.addProperty("title", title);
			result.addProperty("pixel_type", pixelType);
			result.addProperty("pixel_hash_status", pixelHashStatus.value);
			if (pixelHash != null) {
				result.addProperty("pixel_hash_algorithm", PIXEL_HASH_ALGORITHM);
				result.addProperty("pixel_hash_encoding", pixelHashEncoding);
				result.addProperty("pixel_hash", pixelHash);
			}
			if (pixelHashReason != null) result.addProperty("pixel_hash_reason",
				pixelHashReason);
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
