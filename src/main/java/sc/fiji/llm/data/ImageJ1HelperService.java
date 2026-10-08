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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import net.imagej.ImgPlus;
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

	/**
	 * Returns the ImageJ 1.x image with the given ID. Callers should read image
	 * state from the opaque legacy image rather than asking ImageJ2 for a display:
	 * building a display autoscales every channel, which reads one plane per
	 * channel and can take hours for a lazily loaded image with many channels.
	 *
	 * @param id the ImageJ 1.x image ID
	 * @return the image when the legacy helper is available and the ID is open
	 */
	public Optional<Object> getImage(final int id) {
		try {
			return getIJ1Helper().map(helper -> (Object) helper.getImage(id));
		}
		catch (final RuntimeException e) {
			return Optional.empty();
		}
	}

	/**
	 * Returns the active ImageJ 1.x image without linking this service to IJ1 UI
	 * classes.
	 *
	 * @return the active image when ImageJ Legacy is available
	 */
	public Optional<Object> getActiveImage() {
		if (getIJ1Helper().isEmpty()) return Optional.empty();
		return Optional.ofNullable(invokeStatic("ij.WindowManager", "getCurrentImage"));
	}

	/**
	 * Returns the legacy image ID, or {@code -1} when it cannot be read.
	 *
	 * @param image opaque ImageJ 1.x image
	 * @return image ID, or {@code -1} when unavailable
	 */
	public int getImageId(final Object image) {
		final Object id = invoke(image, "getID");
		return id instanceof Number number ? number.intValue() : -1;
	}

	public boolean isActiveImage(final int id) {
		return getActiveImage().map(image -> getImageId(image) == id).orElse(false);
	}

	/** Makes the image's window the current ImageJ window; call on the EDT. */
	public boolean activateImage(final int id) {
		final Object image = getImage(id).orElse(null);
		final Object window = invoke(image, "getWindow");
		if (window == null) return false;
		invokeStatic("ij.WindowManager", "setCurrentWindow", window);
		invoke(window, "toFront");
		return isActiveImage(id);
	}

	public boolean isImageVisible(final int id) {
		return getImage(id).map(image -> Boolean.TRUE.equals(invoke(image, "isVisible")))
			.orElse(false);
	}

	public String getImageTitle(final int id) {
		return getImage(id).map(this::getImageTitle).orElse("");
	}

	/**
	 * Returns the title of an opaque legacy image, or an empty string.
	 *
	 * @param image opaque ImageJ 1.x image
	 * @return image title, or an empty string when unavailable
	 */
	public String getImageTitle(final Object image) {
		final Object title = invoke(image, "getTitle");
		return title == null ? "" : String.valueOf(title);
	}

	/**
	 * Wraps an image as an ImageJ2 {@link ImgPlus} without creating a display.
	 * Planes are loaded only when accessed. This is the same wrapping imagej-legacy
	 * uses for its datasets, so axes and pixel types match what ImageJ2 reports.
	 *
	 * @param image opaque ImageJ 1.x image returned by {@link #getImage(int)}
	 * @return a lazy ImageJ2 view of the image
	 */
	public static ImgPlus<?> wrap(final Object image) {
		final Object wrapped = invokeStatic("net.imglib2.img.VirtualStackAdapter",
			"wrap", image);
		if (wrapped instanceof ImgPlus<?> imgPlus) return imgPlus;
		throw new IllegalStateException("Could not wrap legacy image as ImgPlus");
	}

	/**
	 * Returns whether an image has a multi-plane virtual stack.
	 *
	 * @param image opaque ImageJ 1.x image
	 * @return whether its virtual stack has more than one plane
	 */
	public boolean hasVirtualStack(final Object image) {
		if (asInt(invoke(image, "getStackSize")) <= 1) return false;
		return Boolean.TRUE.equals(invoke(invoke(image, "getStack"), "isVirtual"));
	}

	/**
	 * Invokes a public method on an opaque legacy object.
	 *
	 * @param target object on which to invoke the method
	 * @param methodName public method name
	 * @param args method arguments
	 * @return method result, or {@code null} if invocation fails
	 */
	public Object invokeLegacyMethod(final Object target, final String methodName,
		final Object... args)
	{
		return invoke(target, methodName, args);
	}

	/**
	 * Reads a public field from an opaque legacy object.
	 *
	 * @param target object containing the field
	 * @param fieldName public field name
	 * @return field value, or {@code null} if unavailable
	 */
	public Object getLegacyField(final Object target, final String fieldName) {
		return getField(target, fieldName);
	}

	/**
	 * Reads a public static field by class name.
	 *
	 * @param className fully qualified class name
	 * @param fieldName public static field name
	 * @return field value, or {@code null} if unavailable
	 */
	public Object getLegacyStaticField(final String className,
		final String fieldName)
	{
		return getStaticField(className, fieldName);
	}

	/**
	 * Checks whether an opaque object is an instance of a class by name.
	 *
	 * @param target object to check
	 * @param className fully qualified class name
	 * @return whether the object is an instance of that class
	 */
	public boolean isLegacyInstance(final Object target, final String className) {
		if (target == null) return false;
		try {
			return Class.forName(className).isInstance(target);
		}
		catch (final ReflectiveOperationException | LinkageError e) {
			return false;
		}
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
			final Method method = findMethod(type, methodName, args);
			if (!Modifier.isStatic(method.getModifiers())) return null;
			return method.invoke(null, args);
		}
		catch (final ReflectiveOperationException | LinkageError |
			IllegalArgumentException e)
		{
			return null;
		}
	}

	private static Object invoke(final Object target, final String methodName,
		final Object... args)
	{
		if (target == null) return null;
		try {
			final Method method = findMethod(target.getClass(), methodName, args);
			return method.invoke(target, args);
		}
		catch (final ReflectiveOperationException | LinkageError |
			IllegalArgumentException e)
		{
			return null;
		}
	}

	private static Method findMethod(final Class<?> type, final String methodName,
		final Object... args) throws NoSuchMethodException
	{
		for (final Method method : type.getMethods()) {
			if (!method.getName().equals(methodName) || method.getParameterCount() !=
				args.length) continue;
			final Class<?>[] parameterTypes = method.getParameterTypes();
			boolean matches = true;
			for (int i = 0; i < args.length; i++) {
				if (!isCompatible(parameterTypes[i], args[i])) {
					matches = false;
					break;
				}
			}
			if (matches) return method;
		}
		throw new NoSuchMethodException(type.getName() + "." + methodName);
	}

	private static boolean isCompatible(final Class<?> parameterType,
		final Object argument)
	{
		if (argument == null) return !parameterType.isPrimitive();
		final Class<?> referenceType = parameterType.isPrimitive() ?
			boxedType(parameterType) : parameterType;
		return referenceType.isAssignableFrom(argument.getClass());
	}

	private static Class<?> boxedType(final Class<?> primitiveType) {
		if (primitiveType == boolean.class) return Boolean.class;
		if (primitiveType == byte.class) return Byte.class;
		if (primitiveType == char.class) return Character.class;
		if (primitiveType == short.class) return Short.class;
		if (primitiveType == int.class) return Integer.class;
		if (primitiveType == long.class) return Long.class;
		if (primitiveType == float.class) return Float.class;
		if (primitiveType == double.class) return Double.class;
		return primitiveType;
	}

	private static Object getField(final Object target, final String fieldName) {
		if (target == null) return null;
		try {
			return target.getClass().getField(fieldName).get(target);
		}
		catch (final ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	private static Object getStaticField(final String className,
		final String fieldName)
	{
		try {
			final Class<?> type = Class.forName(className);
			final Field field = type.getField(fieldName);
			return Modifier.isStatic(field.getModifiers()) ? field.get(null) : null;
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
		return Boolean.TRUE.equals(getStaticField(className, fieldName));
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
