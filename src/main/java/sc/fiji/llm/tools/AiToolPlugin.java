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

import java.util.Map;

import org.scijava.plugin.SingletonPlugin;

import dev.langchain4j.agent.tool.ToolSpecification;
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
	 * @return the {@link ToolContext} where the tools provided by this plugin should be available.
	 */
	default ToolContext getToolContext() {
		return ToolContext.ANY;
	}
}
