package sc.fiji.llm.tools;

import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Default implementation of AiToolService.
 */
@Plugin(type = Service.class)
public class DefaultAiToolService extends AbstractService implements AiToolService {

	@Parameter
	private PluginService pluginService;

	@Override
	public List<AiToolPlugin> getAvailableTools() {
		return pluginService.createInstancesOfType(AiToolPlugin.class);
	}

	@Override
	public AiToolPlugin getTool(final String toolName) {
		return getAvailableTools().stream()
			.filter(t -> t.getName().equals(toolName))
			.findFirst()
			.orElse(null);
	}
}
