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

package sc.fiji.llm.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

public abstract class AbstractAiToolPlugin implements AiToolPlugin {

	private final Class<? extends AiToolPlugin> implementingClass;
	private Map<ToolSpecification, ToolExecutor> tools;

	public AbstractAiToolPlugin(Class<? extends AiToolPlugin> myClass) {
		implementingClass = myClass;
	}

	@Override
	public Map<ToolSpecification, ToolExecutor> getTools() {
		if (tools == null) {
			buildTools();
		}
		return tools;
	}

	/**
	 * @param errorMessage Base error message
	 * @return A Json-formated version of the error message
	 */
	public String jsonError(String errorMessage) {
		JsonObject err = new JsonObject();
		err.addProperty("error", errorMessage);
		return err.toString();
	}

	private synchronized void buildTools() {
		if (tools == null) {
			Map<ToolSpecification, ToolExecutor> interimTools = new HashMap<>();

			Arrays.stream(implementingClass.getDeclaredMethods()).filter(
				method -> method.isAnnotationPresent(Tool.class)).forEach(method -> {
					ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(
						method);
					ToolExecutor executor = DefaultToolExecutor.builder().object(this)
						.originalMethod(method).methodToInvoke(method)
						.wrapToolArgumentsExceptions(true).propagateToolExecutionExceptions(
							true).build();
					interimTools.put(spec, executor);
				});

			tools = Collections.unmodifiableMap(interimTools);
		}
	}
}
