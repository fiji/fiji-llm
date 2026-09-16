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

import java.lang.reflect.Method;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;

/** Read-only tool for inspecting the active ImageJ Results Table. */
@Plugin(type = AiToolPlugin.class)
public class ResultsTableToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private ImageJ1HelperService imageJ1HelperService;

	public ResultsTableToolPlugin() {
		super(ResultsTableToolPlugin.class);
	}

	@Override
	public String getName() {
		return "Results Tools";
	}

	@Override
	public String getUsage() {
		return """
The fiji_results_* tools enable interaction with the ImageJ Results Table.
""";
	}

	@Tool(value = { "Read the current Results Table state, including whether a table is present, and if so the headers, row count, and row values." }, name = "fiji_results_read")
	public String readResultsTable() {
		try {
			final Object table = imageJ1HelperService == null ? null : imageJ1HelperService.getResultsTable();
			if (table == null) {
				final JsonObject empty = new JsonObject();
				empty.addProperty("present", false);
				empty.addProperty("row_count", 0);
				empty.addProperty("column_count", 0);
				empty.add("columns", new JsonArray());
				empty.add("rows", new JsonArray());
				return empty.toString();
			}

			final JsonObject result = new JsonObject();
			result.addProperty("present", true);
			result.addProperty("title", "Results");
			result.addProperty("row_count", asInt(invoke(table, "getCounter")));

			final String headings = asString(invoke(table, "getColumnHeadings"));
			final JsonArray columns = new JsonArray();
			if (headings != null && !headings.isBlank()) {
				for (final String heading : headings.split("\\s+")) {
					if (!heading.isBlank()) columns.add(heading);
				}
			}
			result.add("columns", columns);
			result.addProperty("column_count", columns.size());

			final JsonArray rows = new JsonArray();
			final int rowCount = asInt(invoke(table, "getCounter"));
			for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
				final JsonObject row = new JsonObject();
				for (int i = 0; i < columns.size(); i++) {
					final String key = columns.get(i).getAsString();
					final double value = asDouble(invoke(table, "getValue", key, rowIndex));
					row.addProperty(key, value);
				}
				rows.add(row);
			}
			result.add("rows", rows);
			return result.toString();
		}
		catch (ReflectiveOperationException e) {
			return jsonError("Failed to run fiji_results_read: " + e.getMessage());
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_results_read: " + e.getMessage());
		}
	}

	private static Object invoke(final Object target, final String methodName,
		final Object... args) throws ReflectiveOperationException
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
		final Method method = target.getClass().getMethod(methodName, parameterTypes);
		return method.invoke(target, args);
	}

	private static int asInt(final Object value) {
		return value instanceof Number ? ((Number) value).intValue() : 0;
	}

	private static double asDouble(final Object value) {
		return value instanceof Number ? ((Number) value).doubleValue() : Double.NaN;
	}

	private static String asString(final Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
