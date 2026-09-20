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

package sc.fiji.llm.data;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import net.imagej.ImageJService;
import net.imagej.display.ImageDisplay;
import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

/** Provides a consistent ImageJ 1.x helper entry point for other services/tools. */
@Plugin(type = Service.class, priority = Priority.VERY_HIGH)
public final class ImageJ1HelperService extends AbstractService implements
	ImageJService
{

	@Parameter
	private LegacyService legacyService;

	public Optional<IJ1Helper> getIJ1Helper() {
		if (legacyService == null || !legacyService.isActive()) return Optional.empty();
		return Optional.ofNullable(legacyService.getIJ1Helper());
	}

	public String getImageJ1Version() {
		return getIJ1Helper().map(IJ1Helper::getVersion).orElse("Unknown");
	}

	public List<Integer> getImageIds() {
		final List<Integer> ids = new ArrayList<>();
		getIJ1Helper().ifPresent(helper -> {
			try {
				final int[] imageIds = helper.getIDList();
				if (imageIds != null) for (final int id : imageIds) ids.add(id);
			}
			catch (final RuntimeException e) {
				// No ImageJ 1.x images are available.
			}
		});
		return Collections.unmodifiableList(ids);
	}

	public boolean isImageVisible(final int id) {
		try {
			return getIJ1Helper().map(helper -> helper.getImage(id)).map(image -> image
				.isVisible()).orElse(false);
		}
		catch (final RuntimeException e) {
			return false;
		}
	}

	public String getImageTitle(final int id) {
		try {
			return getIJ1Helper().map(helper -> helper.getImage(id)).map(image -> image
				.getTitle()).orElse("");
		}
		catch (final RuntimeException e) {
			return "";
		}
	}

	public int getImageId(final ImageDisplay display) {
		if (display == null || legacyService == null || !legacyService.isActive()) {
			return -1;
		}
		try {
			final var imageMap = legacyService.getImageMap();
			if (imageMap == null) return -1;
			final var imagePlus = imageMap.lookupImagePlus(display);
			return imagePlus == null ? -1 : imagePlus.getID();
		}
		catch (final RuntimeException e) {
			return -1;
		}
	}

	public Optional<Object> getRoi(final ImageDisplay display) {
		if (display == null || legacyService == null || !legacyService.isActive()) {
			return Optional.empty();
		}
		try {
			final var imageMap = legacyService.getImageMap();
			if (imageMap == null) return Optional.empty();
			final var imagePlus = imageMap.lookupImagePlus(display);
			return imagePlus == null ? Optional.empty() : Optional.ofNullable(invoke(
				imagePlus, "getRoi"));
		}
		catch (final RuntimeException e) {
			return Optional.empty();
		}
	}

	public Optional<BufferedImage> getFlattenedImage(final ImageDisplay display) {
		if (display == null || legacyService == null || !legacyService.isActive()) {
			return Optional.empty();
		}
		try {
			final var imageMap = legacyService.getImageMap();
			if (imageMap == null) return Optional.empty();
			final var imagePlus = imageMap.lookupImagePlus(display);
			if (imagePlus == null) return Optional.empty();
			final Object flattened = invoke(imagePlus, "flatten");
			final Object bufferedImage = invoke(flattened, "getBufferedImage");
			return bufferedImage instanceof BufferedImage ? Optional.of((BufferedImage)
				bufferedImage) : Optional.empty();
		}
		catch (final RuntimeException e) {
			return Optional.empty();
		}
	}

	public int getOverlayCount(final ImageDisplay display) {
		final Object overlay = getLegacyImageValue(display, "getOverlay");
		final Object size = invoke(overlay, "size");
		return size instanceof Number ? ((Number) size).intValue() : 0;
	}

	public boolean isOverlayHidden(final ImageDisplay display) {
		return Boolean.TRUE.equals(getLegacyImageValue(display, "getHideOverlay"));
	}

	public Object getResultsTable() {
		return invokeStatic("ij.measure.ResultsTable", "getResultsTable");
	}

	public ResultsTableState getResultsTableState() {
		final Object table = getResultsTable();
		if (table == null) return ResultsTableState.empty();

		final int rows = asInt(invoke(table, "getCounter"));
		final String headings = asString(invoke(table, "getColumnHeadings"));
		final boolean present = invokeStatic("ij.WindowManager", "getFrame", "Results") !=
			null || rows > 0 || !headings.trim().isEmpty();
		return new ResultsTableState(present, rows, headings);
	}

	public Object getRoiManager() {
		return invokeStatic("ij.plugin.frame.RoiManager", "getInstance");
	}

	public MacroRecorderState getMacroRecorderState() {
		if (getIJ1Helper().isEmpty()) return MacroRecorderState.closed();

		final Object recorder = invokeStatic("ij.plugin.frame.Recorder", "getInstance");
		if (recorder == null) return MacroRecorderState.closed();

		return new MacroRecorderState(true, getStaticBoolean("ij.plugin.frame.Recorder",
			"record"), asBoolean(invokeStatic("ij.plugin.frame.Recorder", "scriptMode")),
			asString(invoke(recorder, "getText")));
	}

	private static Object invokeStatic(final String className,
		final String methodName, final Object... args)
	{
		try {
			final Class<?> type = Class.forName(className);
			final Class<?>[] parameterTypes = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) parameterTypes[i] = args[i].getClass();
			final Method method = type.getMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return method.invoke(null, args);
		}
		catch (final ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	private static Object invoke(final Object target, final String methodName,
		final Object... args)
	{
		if (target == null) return null;
		try {
			final Class<?>[] parameterTypes = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) parameterTypes[i] = args[i].getClass();
			final Method method = target.getClass().getMethod(methodName, parameterTypes);
			return method.invoke(target, args);
		}
		catch (final ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	private Object getLegacyImageValue(final ImageDisplay display,
		final String methodName)
	{
		if (display == null || legacyService == null || !legacyService.isActive()) {
			return null;
		}
		try {
			final var imageMap = legacyService.getImageMap();
			if (imageMap == null) return null;
			return invoke(imageMap.lookupImagePlus(display), methodName);
		}
		catch (final RuntimeException e) {
			return null;
		}
	}

	private static int asInt(final Object value) {
		return value instanceof Number ? ((Number) value).intValue() : 0;
	}

	private static String asString(final Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static boolean asBoolean(final Object value) {
		return value instanceof Boolean && (Boolean) value;
	}

	private static boolean getStaticBoolean(final String className,
		final String fieldName)
	{
		try {
			return Class.forName(className).getField(fieldName).getBoolean(null);
		}
		catch (final ReflectiveOperationException | LinkageError e) {
			return false;
		}
	}

	public static final class ResultsTableState {

		private final boolean present;
		private final int rowCount;
		private final String columnHeadings;

		private ResultsTableState(final boolean present, final int rowCount,
			final String columnHeadings)
		{
			this.present = present;
			this.rowCount = rowCount;
			this.columnHeadings = columnHeadings == null ? "" : columnHeadings;
		}

		private static ResultsTableState empty() {
			return new ResultsTableState(false, 0, "");
		}

		public boolean isPresent() {
			return present;
		}

		public int getRowCount() {
			return rowCount;
		}

		public String getColumnHeadings() {
			return columnHeadings;
		}
	}

	public static final class MacroRecorderState {

		private final boolean open;
		private final boolean recording;
		private final boolean scriptMode;
		private final String buffer;

		private MacroRecorderState(final boolean open, final boolean recording,
			final boolean scriptMode, final String buffer)
		{
			this.open = open;
			this.recording = recording;
			this.scriptMode = scriptMode;
			this.buffer = buffer == null ? "" : buffer;
		}

		public static MacroRecorderState closed() {
			return new MacroRecorderState(false, false, false, "");
		}

		public boolean isOpen() {
			return open;
		}

		public boolean isRecording() {
			return recording;
		}

		public boolean isScriptMode() {
			return scriptMode;
		}

		public String getBuffer() {
			return buffer;
		}
	}
}
