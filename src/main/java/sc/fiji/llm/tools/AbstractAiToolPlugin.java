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
		return jsonError(errorMessage, null);
	}

	/**
	 * @param errorMessage Base error message
	 * @param recommendedTool Optional tool name to recommend for resolving the error
	 * @return A Json-formated version of the error message
	 */
	public String jsonError(String errorMessage, String recommendedTool) {
		JsonObject err = new JsonObject();
		err.addProperty("error", errorMessage);
		if (recommendedTool != null && !recommendedTool.trim().isEmpty()) {
			err.addProperty("recommended_tool", recommendedTool);
		}
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
