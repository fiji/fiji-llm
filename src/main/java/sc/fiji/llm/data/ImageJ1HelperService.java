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

package sc.fiji.llm.data;

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

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import net.imagej.ImageJService;
import net.imagej.ImgPlus;
import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;
import net.imglib2.img.VirtualStackAdapter;

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

	/**
	 * Returns the ImageJ 1.x image with the given ID. Callers should read image
	 * state from the {@link ImagePlus} rather than asking ImageJ2 for a display:
	 * building a display autoscales every channel, which reads one plane per
	 * channel and can take hours for a lazily loaded image with many channels.
	 */
	public Optional<ImagePlus> getImage(final int id) {
		try {
			return getIJ1Helper().map(helper -> helper.getImage(id));
		}
		catch (final RuntimeException e) {
			return Optional.empty();
		}
	}

	public Optional<ImagePlus> getActiveImage() {
		if (getIJ1Helper().isEmpty()) return Optional.empty();
		return Optional.ofNullable(WindowManager.getCurrentImage());
	}

	public boolean isActiveImage(final int id) {
		return getActiveImage().map(image -> image.getID() == id).orElse(false);
	}

	/** Makes the image's window the current ImageJ window; call on the EDT. */
	public boolean activateImage(final int id) {
		final ImageWindow window = getImage(id).map(ImagePlus::getWindow).orElse(null);
		if (window == null) return false;
		WindowManager.setCurrentWindow(window);
		window.toFront();
		return isActiveImage(id);
	}

	public boolean isImageVisible(final int id) {
		return getImage(id).map(ImagePlus::isVisible).orElse(false);
	}

	public String getImageTitle(final int id) {
		return getImage(id).map(ImagePlus::getTitle).orElse("");
	}

	/**
	 * Wraps an image as an ImageJ2 {@link ImgPlus} without creating a display.
	 * Planes are loaded only when accessed. This is the same wrapping imagej-legacy
	 * uses for its datasets, so axes and pixel types match what ImageJ2 reports.
	 */
	public static ImgPlus<?> wrap(final ImagePlus image) {
		return VirtualStackAdapter.wrap(image);
	}

	public static boolean hasVirtualStack(final ImagePlus image) {
		return image.getStackSize() > 1 && image.getStack().isVirtual();
	}

	public Object getResultsTable() {
		return invokeStatic("ij.measure.ResultsTable", "getResultsTable");
	}

	public ResultsTableState getResultsTableState() {
		final Object table = getResultsTable();
		if (table == null) return ResultsTableState.empty();

		final int rows = asInt(invoke(table, "getCounter"));
		final String headings = asString(invoke(table, "getColumnHeadings"));
		final boolean present = rows > 0;
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
