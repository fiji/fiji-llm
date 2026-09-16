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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataToolPluginTest {

	private Context context;

	@Before
	public void setUp() {
		context = new Context();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testResultsTableToolReadsCurrentTableState() throws Exception {
		createResultsTable();
		final ResultsTableToolPlugin plugin = new ResultsTableToolPlugin();
		setImageJ1HelperService(plugin, context.getService(ImageJ1HelperService.class));
		final JsonObject json = JsonParser.parseString(plugin.readResultsTable())
			.getAsJsonObject();

		assertTrue(json.get("present").getAsBoolean());
		assertEquals(1, json.get("row_count").getAsInt());
		assertTrue(json.get("columns").getAsJsonArray().size() >= 2);
	}

	@Test
	public void testRoiManagerToolReadsCurrentState() throws Exception {
		final Object manager = invokeStatic("ij.plugin.frame.RoiManager", "getInstance");
		if (manager != null) {
			invokeMethod(manager, "reset");
		}

		final RoiManagerToolPlugin plugin = new RoiManagerToolPlugin();
		setImageJ1HelperService(plugin, context.getService(ImageJ1HelperService.class));
		final JsonObject json = JsonParser.parseString(plugin.readRoiManager())
			.getAsJsonObject();

		assertTrue(json.has("present"));
		assertTrue(json.has("count"));
	}

	private static void setImageJ1HelperService(final Object target,
		final ImageJ1HelperService helperService) throws Exception
	{
		final Field field = target.getClass().getDeclaredField("imageJ1HelperService");
		field.setAccessible(true);
		field.set(target, helperService);
	}

	private static Object createResultsTable() throws Exception {
		final Object table = invokeStatic("ij.measure.ResultsTable", "getResultsTable");
		if (table == null) return null;
		invokeMethod(table, "reset");
		invokeMethod(table, "incrementCounter");
		invokeMethod(table, "addValue", new Object[] { "Area", 42.0 });
		invokeMethod(table, "addValue", new Object[] { "Mean", 11.0 });
		return table;
	}

	private static Object invokeStatic(final String className,
		final String methodName) throws Exception
	{
		final Class<?> type = Class.forName(className);
		final Method method = type.getDeclaredMethod(methodName);
		method.setAccessible(true);
		return method.invoke(null);
	}

	private static Object invokeMethod(final Object target,
		final String methodName) throws Exception
	{
		return invokeMethod(target, methodName, new Object[0]);
	}

	private static Object invokeMethod(final Object target,
		final String methodName, final Object[] args) throws Exception
	{
		final Class<?>[] parameterTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				parameterTypes[i] = Object.class;
			}
			else if (args[i] instanceof Integer) {
				parameterTypes[i] = int.class;
			}
			else if (args[i] instanceof Double) {
				parameterTypes[i] = double.class;
			}
			else if (args[i] instanceof Float) {
				parameterTypes[i] = float.class;
			}
			else if (args[i] instanceof Long) {
				parameterTypes[i] = long.class;
			}
			else if (args[i] instanceof Boolean) {
				parameterTypes[i] = boolean.class;
			}
			else {
				parameterTypes[i] = args[i].getClass();
			}
		}
		final Method method = target.getClass().getDeclaredMethod(methodName,
			parameterTypes);
		method.setAccessible(true);
		return method.invoke(target, args);
	}
}