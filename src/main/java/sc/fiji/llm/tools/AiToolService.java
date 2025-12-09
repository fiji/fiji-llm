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
	 * See {@link ToolContext} for built-in contexts available.
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
