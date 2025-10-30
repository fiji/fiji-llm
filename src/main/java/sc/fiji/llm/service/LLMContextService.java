package sc.fiji.llm.service;

import org.scijava.service.SciJavaService;

import net.imagej.Dataset;

/**
 * SciJava service for building LLM context from Fiji/ImageJ components.
 * This service gathers information about available plugins, images, scripts,
 * and other relevant data to provide context to the LLM.
 */
public interface LLMContextService extends SciJavaService {

	/**
	 * Build context information about available plugins and commands.
	 *
	 * @return a formatted string describing available plugins and commands
	 */
	String buildPluginContext();

	/**
	 * Build context information about an image/dataset.
	 *
	 * @param dataset the dataset to describe
	 * @return a formatted string with image metadata and properties
	 */
	String buildImageContext(Dataset dataset);

	/**
	 * Build context information about a script.
	 *
	 * @param scriptContent the content of the script
	 * @param language the scripting language (e.g., "Groovy", "Python")
	 * @return a formatted string with script information
	 */
	String buildScriptContext(String scriptContent, String language);

	/**
	 * Build context information about an error or exception.
	 *
	 * @param error the error message
	 * @param stackTrace the stack trace (optional)
	 * @return a formatted string with error information
	 */
	String buildErrorContext(String error, String stackTrace);

	/**
	 * Build a complete context string combining multiple sources.
	 * 
	 * @param includePlugins whether to include plugin context
	 * @param includeImages whether to include active image context
	 * @param includeScript whether to include active script context
	 * @return a complete context string
	 */
	String buildCompleteContext(boolean includePlugins, boolean includeImages, boolean includeScript);
}
