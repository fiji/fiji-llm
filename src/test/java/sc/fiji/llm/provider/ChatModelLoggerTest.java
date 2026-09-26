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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.scijava.log.AbstractLogService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogMessage;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

public class ChatModelLoggerTest {

	@Test
	public void testLogsRequestAndResponseSummaries() {
		final CapturingLogService log = new CapturingLogService(LogLevel.DEBUG);
		final ChatModelLogger logger = new ChatModelLogger(log, "TestProvider");
		final ChatRequest request = ChatRequest.builder().messages(UserMessage.from(
			"hello")).build();
		final Map<Object, Object> attributes = new HashMap<>();
		logger.onRequest(new ChatModelRequestContext(request, null, attributes));

		final AiMessage message = AiMessage.builder().thinking("hmm").toolExecutionRequests(
			List.of(ToolExecutionRequest.builder().name("fiji_image_list").arguments(
				"{}").build())).build();
		logger.onResponse(new ChatModelResponseContext(ChatResponse.builder()
			.aiMessage(message).finishReason(FinishReason.TOOL_EXECUTION).tokenUsage(
				new TokenUsage(10, 5)).build(), request, null, attributes));

		assertEquals(log.messages.toString(), 2, log.messages.size());
		assertTrue(log.messages.get(0), log.messages.get(0).contains(
			"1 messages, 0 tools"));
		final String response = log.messages.get(1);
		assertTrue(response, response.contains("finish=TOOL_EXECUTION"));
		assertTrue(response, response.contains("tokens in=10 out=5"));
		assertTrue(response, response.contains("thinking=3 chars"));
		assertTrue(response, response.contains("tool calls: fiji_image_list {}"));
	}

	@Test
	public void testSilentAboveDebugLevel() {
		final CapturingLogService log = new CapturingLogService(LogLevel.INFO);
		final ChatModelLogger logger = new ChatModelLogger(log, "TestProvider");
		logger.onRequest(new ChatModelRequestContext(ChatRequest.builder()
			.messages(UserMessage.from("hello")).build(), null, new HashMap<>()));
		assertTrue(log.messages.isEmpty());
	}

	private static class CapturingLogService extends AbstractLogService {

		private final List<String> messages = new ArrayList<>();

		CapturingLogService(final int level) {
			setLevel(level);
		}

		@Override
		protected void messageLogged(final LogMessage message) {
			messages.add(message.text());
		}
	}
}
