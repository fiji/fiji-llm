package sc.fiji.llm.tools;

import java.util.List;

import org.scijava.service.SciJavaService;

/**
 * SciJava service for managing AI tools.
 * This service is used to discover and list all available tool plugins.
 */
public interface AiToolService extends SciJavaService {

	/**
	 * Get all available AI tool plugins discovered in the SciJava context.
	 *
	 * @return list of available AI tools
	 */
	List<AiToolPlugin> getAvailableTools();

	/**
	 * Get a specific tool plugin by name.
	 *
	 * @param toolName the name of the tool
	 * @return the tool plugin, or null if not found
	 */
	AiToolPlugin getTool(String toolName);
}
