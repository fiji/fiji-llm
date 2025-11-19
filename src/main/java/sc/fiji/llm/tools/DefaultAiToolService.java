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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;

/**
 * Default implementation of AiToolService.
 */
@Plugin(type = Service.class, priority = Priority.LAST)
public class DefaultAiToolService extends AbstractSingletonService<AiToolPlugin>
	implements AiToolService
{
	private Map<ToolContext, List<ToolSpecification>> toolsByContext;
	private Map<ToolSpecification, ToolExecutor> toolsWithExecutors;

	@Parameter
	private LogService logService;

	@Override
	public Class<AiToolPlugin> getPluginType() {
		return AiToolPlugin.class;
	}

	@Override
	public Map<ToolSpecification, ToolExecutor> getToolsWithExecutors() {
		if (toolsWithExecutors == null) {
			initMaps();
		}
		return toolsWithExecutors;
	}

	@Override
	public List<ToolSpecification> getToolsForContext(ToolContext toolContext) {
		if (toolsByContext == null) {
			initMaps();
		}
		return toolsByContext.get(toolContext);
	}

	private synchronized void initMaps() {
		if (toolsWithExecutors == null || toolsByContext == null) {
			//Use interim maps to collect tool specifications
			Map<ToolContext, List<ToolSpecification>> interimContextMap = new HashMap<>();
			Map<ToolSpecification, ToolExecutor> interimExecutorMap = new HashMap<>();
			List<ToolSpecification> anyContextList = new ArrayList<>();
			interimContextMap.put(ToolContext.ANY, anyContextList);
			Set<String> toolNames = new HashSet<>();

			for (AiToolPlugin plugin : getInstances()) {
				ToolContext context = plugin.getToolContext();
				Map<ToolSpecification, ToolExecutor> specs = plugin.getTools();
				if (specs == null) continue;

				// Add to context-specific list
				List<ToolSpecification> contextList = interimContextMap.computeIfAbsent(context, k -> new ArrayList<>());
				for (Entry<ToolSpecification, ToolExecutor> entry : specs.entrySet()) {
					ToolSpecification spec = entry.getKey();
					String name = spec.name();
					if (toolNames.contains(name)) {
						logService.error("Skipping duplicate tool: " + name + " in " + plugin.getClass());
						continue;
					}
					toolNames.add(name);
					interimExecutorMap.put(spec, entry.getValue());

					// Always add to ANY list
					anyContextList.add(spec);

					// Add to context-specific list if needed
					if (!contextList.equals(anyContextList)) {
						contextList.add(spec);
					}
				}
			}

			// Convert all lists to immutable
			Map<ToolContext, List<ToolSpecification>> finalMap = new HashMap<>();
			for (Map.Entry<ToolContext, List<ToolSpecification>> entry : interimContextMap.entrySet()) {
				finalMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
			}
			toolsByContext = Collections.unmodifiableMap(finalMap);
			toolsWithExecutors = Collections.unmodifiableMap(interimExecutorMap);
		}
	}
}
