package sc.fiji.llm.assistant;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;

/**
 * The main Fiji/ImageJ assistant interface powered by LangChain4j.
 * This interface defines the capabilities of the LLM assistant.
 * LangChain4j automatically generates an implementation of this interface.
 */
public interface FijiAssistant {

	public static final String SYSTEM_PROMPT = "You are an expert Fiji/ImageJ assistant. You help users with image analysis, " +
		"processing, and scripting in the Fiji/ImageJ environment.";

	/**
	 * General chat interaction with structured messages.
	 *
	 * @param chatRequest a {@link ChatRequest} containing messages and parameters
	 * @return a {@link ChatResponse} with the assistant's response
	 */
	AiMessage chat(ChatRequest chatRequest);

	/**
	 * Streaming chat interaction for real-time responses.
	 *
	 * @param chatRequest a {@link ChatRequest} containing messages and parameters
	 * @return a token stream for progressive response rendering
	 */
	TokenStream chatStreaming(ChatRequest chatRequest);
}
