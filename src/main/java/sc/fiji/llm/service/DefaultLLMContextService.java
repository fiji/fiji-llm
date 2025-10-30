package sc.fiji.llm.service;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import net.imagej.Dataset;

/**
 * Default implementation of LLMContextService.
 */
@Plugin(type = Service.class)
public class DefaultLLMContextService extends AbstractService implements LLMContextService {

	@Parameter
	private PluginService pluginService;

	@Parameter
	private CommandService commandService;

	@Override
	public String buildPluginContext() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Available ImageJ/Fiji Commands:\n");

		final java.util.List<CommandInfo> commands = commandService.getCommands();
		int count = 0;
		for (CommandInfo info : commands) {
			if (count >= 50) { // Limit to avoid token overflow
				sb.append("... and ").append(commands.size() - count).append(" more commands\n");
				break;
			}
			sb.append("- ").append(info.getMenuPath()).append("\n");
			if (info.getDescription() != null && !info.getDescription().isEmpty()) {
				sb.append("  Description: ").append(info.getDescription()).append("\n");
			}
			count++;
		}

		return sb.toString();
	}

	@Override
	public String buildImageContext(final Dataset dataset) {
		if (dataset == null) {
			return "No image currently selected.";
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("Active Image Information:\n");
		sb.append("- Name: ").append(dataset.getName()).append("\n");
		sb.append("- Dimensions: ");
		for (int i = 0; i < dataset.numDimensions(); i++) {
			if (i > 0) sb.append(" × ");
			sb.append(dataset.dimension(i));
		}
		sb.append("\n");
		sb.append("- Type: ").append(dataset.getTypeLabelLong()).append("\n");
		
		// Add axis information
		sb.append("- Axes: ");
		for (int i = 0; i < dataset.numDimensions(); i++) {
			if (i > 0) sb.append(", ");
			sb.append(dataset.axis(i).type().getLabel());
		}
		sb.append("\n");

		return sb.toString();
	}

	@Override
	public String buildScriptContext(final String scriptContent, final String language) {
		final StringBuilder sb = new StringBuilder();
		sb.append("Active Script Information:\n");
		sb.append("- Language: ").append(language).append("\n");
		sb.append("- Content:\n```").append(language.toLowerCase()).append("\n");
		sb.append(scriptContent);
		sb.append("\n```\n");
		return sb.toString();
	}

	@Override
	public String buildErrorContext(final String error, final String stackTrace) {
		final StringBuilder sb = new StringBuilder();
		sb.append("Error Information:\n");
		sb.append("- Message: ").append(error).append("\n");
		if (stackTrace != null && !stackTrace.isEmpty()) {
			sb.append("- Stack Trace:\n```\n");
			sb.append(stackTrace);
			sb.append("\n```\n");
		}
		return sb.toString();
	}

	@Override
	public String buildCompleteContext(final boolean includePlugins, final boolean includeImages,
		final boolean includeScript)
	{
		final StringBuilder sb = new StringBuilder();
		
		if (includePlugins) {
			sb.append(buildPluginContext()).append("\n");
		}
		
		// TODO: Get active image from ImageJ context
		if (includeImages) {
			sb.append("(Image context would be included here)\n");
		}
		
		// TODO: Get active script from script editor
		if (includeScript) {
			sb.append("(Script context would be included here)\n");
		}
		
		return sb.toString();
	}
}
