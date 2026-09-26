/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.scijava.log.LogService;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.TokenUsage;
import sc.fiji.llm.log.LogUtils;

/**
 * Logs each model API call to the SciJava {@link LogService}. Debug level
 * summarizes requests and responses: message and tool counts, duration,
 * finish reason, token usage, and requested tool calls. Trace level adds the
 * latest request message and the full thinking and response text.
 */
public class ChatModelLogger implements ChatModelListener {

	private static final String START_TIME = ChatModelLogger.class.getName() +
		".startTime";
	private static final int SUMMARY_LENGTH = 300;

	private final LogService log;
	private final String providerName;

	public ChatModelLogger(final LogService log, final String providerName) {
		this.log = log;
		this.providerName = providerName;
	}

	@Override
	public void onRequest(final ChatModelRequestContext context) {
		if (log == null || !log.isDebug()) return;
		context.attributes().put(START_TIME, System.nanoTime());
		final ChatRequest request = context.chatRequest();
		final List<ChatMessage> messages = request.messages();
		final int toolCount = request.toolSpecifications() == null ? 0 : request
			.toolSpecifications().size();
		log.debug("LLM request to " + providerName + " (" + request.modelName() +
			"): " + messages.size() + " messages, " + toolCount + " tools");
		if (log.isTrace() && !messages.isEmpty()) {
			log.trace("LLM request latest message: " + messages.get(messages.size() -
				1));
		}
	}

	@Override
	public void onResponse(final ChatModelResponseContext context) {
		if (log == null || !log.isDebug()) return;
		final ChatResponse response = context.chatResponse();
		final AiMessage message = response.aiMessage();
		final ChatResponseMetadata metadata = response.metadata();
		final StringBuilder sb = new StringBuilder("LLM response from " +
			providerName + " after " + elapsedMillis(context.attributes().get(
				START_TIME)) + " ms: finish=" + metadata.finishReason());
		final TokenUsage usage = metadata.tokenUsage();
		if (usage != null) {
			sb.append(", tokens in=" + usage.inputTokenCount() + " out=" + usage
				.outputTokenCount());
		}
		if (message.thinking() != null) {
			sb.append(", thinking=" + message.thinking().length() + " chars");
		}
		if (message.text() != null) {
			sb.append(", text=" + message.text().length() + " chars");
		}
		if (message.hasToolExecutionRequests()) {
			sb.append(", tool calls: " + message.toolExecutionRequests().stream().map(
				ChatModelLogger::summarize).collect(Collectors.joining("; ")));
		}
		log.debug(sb.toString());
		if (log.isTrace()) {
			if (message.thinking() != null) {
				log.trace("LLM thinking:\n" + message.thinking());
			}
			if (message.text() != null) log.trace("LLM text:\n" + message.text());
		}
	}

	@Override
	public void onError(final ChatModelErrorContext context) {
		if (log == null || !log.isDebug()) return;
		log.debug("LLM request to " + providerName + " failed after " +
			elapsedMillis(context.attributes().get(START_TIME)) + " ms", context
				.error());
	}

	private static String summarize(final ToolExecutionRequest request) {
		return request.name() + " " + LogUtils.abbreviate(request.arguments(),
			SUMMARY_LENGTH);
	}

	private static long elapsedMillis(final Object startTime) {
		if (!(startTime instanceof Long start)) return -1;
		return (System.nanoTime() - start) / 1_000_000;
	}
}
