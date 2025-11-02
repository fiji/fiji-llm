package sc.fiji.llm.tools;

import org.scijava.plugin.SingletonService;

/**
 * SciJava service for managing AI tools.
 * This service is used to discover and list all available tool plugins.
 */
public interface AiToolService extends SingletonService<AiToolPlugin> {

}
