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

import java.util.List;
import java.util.Map;

import org.scijava.plugin.SingletonService;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import net.imagej.ImageJService;

/**
 * SciJava service for managing AI tools. This service is used to discover and
 * list all available tool plugins.
 */
public interface AiToolService extends SingletonService<AiToolPlugin>, ImageJService {

	/**
	 * @return A system message fragment indicating language-specific
	 *         considerations for executing tools. This is necessary when using
	 *         {@link dev.langchain4j.agent.tool.P} parameter annotations. These
	 *         can make all tools appear as Python methods with kwargs and result
	 *         in incorrect API usage by some models.
	 */
	default String toolEnvironmentMessage() {
		return """
IMPORTANT: All tools are implemented in Java. Argument ordering MUST be respected.
Remember: Tool methods are ONLY available to you, not to the user.
""";
	}

	/**
	 * This method allows for global definition of tools, e.g. when building an
	 * {@code AiService}
	 *
	 * @return The global map of available tools and their executors
	 */
	Map<ToolSpecification, ToolExecutor> getToolsWithExecutors();

	/**
	 * This method allows filtering of available tools in a particular
	 * {@code ChatRequest}
	 *
	 * See {@link ToolScope} for built-in contexts available.
	 *
	 * @param toolContext The desired context
	 * @return All {@link ToolSpecifications} compatible with the given context
	 */
	List<ToolSpecification> getToolsForContext(String toolContext);

	/**
	 * Handler for {@link BeforeToolExecution} events
	 */
	void processToolRequest(BeforeToolExecution beforeToolExecutionEvent);

	/**
	 * Handler for {@link ToolExecution} events
	 */
	void processToolExecution(ToolExecution toolExecutionEvent);

	/**
	 * Handler for tool execution errors
	 */
	ToolErrorHandlerResult handleExecutionError(Throwable error, ToolErrorContext context);

	/**
	 * Handler for tool argument errors
	 */
	ToolErrorHandlerResult handleArgumentError(Throwable error, ToolErrorContext context);
}
