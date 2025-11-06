package sc.fiji.llm.tools;

import org.scijava.plugin.SingletonService;

/**
 * SciJava service for managing AI tools.
 * This service is used to discover and list all available tool plugins.
 */
public interface AiToolService extends SingletonService<AiToolPlugin> {

    /**
     * @return A system message fragment indicating language-specific
     * considerations for executing tools. This is necessary when using {@link
     * dev.langchain4j.agent.tool.P} parameter annotations. These can make
     * all tools appear as Python methods with kwargs and result in incorrect
     * API usage by some models.
     */
    default String toolEnvironmentMessage() {
        return "IMPORTANT: All tools are implemented in Java. Positional ordering MUST be respected.";
    }
}
