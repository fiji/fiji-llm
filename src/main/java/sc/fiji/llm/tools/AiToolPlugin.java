package sc.fiji.llm.tools;

import org.scijava.plugin.SingletonPlugin;

/**
 * Plugin interface for AI tools that can be used by LLM assistants.
 * <p>
 * Tool plugins should have methods annotated with {@code @Tool} from LangChain4j
 * to define the capabilities available to the AI assistant.
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
}
