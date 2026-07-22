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

import java.util.Map;

import org.scijava.plugin.SingletonPlugin;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutor;

/**
 * Plugin interface for AI tools that can be used by LLM assistants.
 * <p>
 * Tool plugins should have methods annotated with {@code @Tool} from
 * LangChain4j to define the capabilities available to the AI assistant.
 * </p>
 */
public interface AiToolPlugin extends SingletonPlugin {

	/**
	 * Get the name of this tool.
	 *
	 * @return the tool name
	 */
	String getName();

	/**
	 * Get a description of this tool's capabilities.
	 *
	 * @return a description of how this tool should be used by an LLM
	 */
	String getUsage();

	/**
	 * @return The tools provided by this plugin with their executors
	 */
	Map<ToolSpecification, ToolExecutor> getTools();

	/**
	 * Handler for when plugins from this tool encounter errors
	 *
	 * @return A {@link ToolErrorHandlerResult} tailored to this failure, or {@code null}
	 */
	default ToolErrorHandlerResult handleToolError(Throwable error, ToolErrorContext context, ToolErrorType type) {
		return null;
	}

	/**
	 * @return Tool scopes allow filtering of available tools.
	 */
	default String getToolScope() {
		return ToolScope.ANY;
	}
}
