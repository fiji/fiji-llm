package sc.fiji.llm.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The main Fiji/ImageJ assistant interface powered by LangChain4j.
 * This interface defines the capabilities of the LLM assistant.
 * LangChain4j automatically generates an implementation of this interface.
 */
public interface FijiAssistant {

	/**
	 * General chat interaction with context.
	 *
	 * @param context the Fiji/ImageJ context (available plugins, images, etc.)
	 * @param userMessage the user's message
	 * @return the assistant's response
	 */
	@SystemMessage("You are an expert Fiji/ImageJ assistant. You help users with image analysis, " +
		"processing, and scripting in the Fiji/ImageJ environment.\n\n" +
		"Available context:\n{{context}}\n\n" +
		"Provide clear, accurate, and helpful responses. When suggesting code, " +
		"prefer ImageJ2/SciJava APIs over legacy ImageJ1 APIs when possible.\n\n" +
		"When you generate or modify scripts for the user, use the createOrUpdateScript tool " +
		"to place the code directly in the Fiji script editor. This allows users to immediately " +
		"run and modify the code. Always use this tool when providing executable scripts.")
	String chat(@V("context") String context, @UserMessage String userMessage);

	/**
	 * Streaming chat interaction for real-time responses.
	 *
	 * @param context the Fiji/ImageJ context
	 * @param userMessage the user's message
	 * @return a token stream for progressive response rendering
	 */
	@SystemMessage("You are an expert Fiji/ImageJ assistant. You help users with image analysis, " +
		"processing, and scripting in the Fiji/ImageJ environment.\n\n" +
		"Available context:\n{{context}}\n\n" +
		"Provide clear, accurate, and helpful responses. When suggesting code, " +
		"prefer ImageJ2/SciJava APIs over legacy ImageJ1 APIs when possible.")
	TokenStream chatStreaming(@V("context") String context, @UserMessage String userMessage);

	/**
	 * Explain a code snippet.
	 *
	 * @param code the code to explain
	 * @param language the programming language
	 * @return an explanation of the code
	 */
	@SystemMessage("You are an expert at explaining Fiji/ImageJ code.\n" +
		"Explain the following {{language}} code clearly and concisely:\n\n" +
		"```{{language}}\n{{code}}\n```\n\n" +
		"Focus on what the code does, not line-by-line details.")
	String explainCode(@V("code") String code, @V("language") String language);

	/**
	 * Help debug an error.
	 *
	 * @param errorMessage the error message
	 * @param stackTrace the stack trace (optional)
	 * @param code the code that caused the error (optional)
	 * @return suggestions for fixing the error
	 */
	@SystemMessage("You are an expert at debugging Fiji/ImageJ code.\n" +
		"Help the user fix the following error:\n\n" +
		"Error: {{error}}\n" +
		"{{#stackTrace}}Stack trace: {{stackTrace}}{{/stackTrace}}\n" +
		"{{#code}}Code:\n```\n{{code}}\n```{{/code}}\n\n" +
		"Provide a clear explanation of what went wrong and how to fix it.")
	String debugError(
		@V("error") String errorMessage,
		@V("stackTrace") String stackTrace,
		@V("code") String code
	);
}
