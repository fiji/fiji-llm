/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
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

package sc.fiji.llm.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

public class FijiAssistantMultimodalTest {

	@Test
	public void testStructuredContentsBecomeOllamaImages() throws Exception {
		final AtomicReference<String> requestBody = new AtomicReference<>();
		final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/api/chat", exchange -> {
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(),
				StandardCharsets.UTF_8));
			final byte[] response = "{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"done\":true}"
				.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(response);
			}
		});
		server.start();

		try {
			final OllamaChatModel model = OllamaChatModel.builder().baseUrl(
				"http://localhost:" + server.getAddress().getPort()).modelName("test")
				.maxRetries(0).build();
			final FijiAssistant assistant = AiServices.builder(FijiAssistant.class)
				.chatModel(model).streamingChatModel(new StreamingChatModel() {}).build();
			final String base64 = Base64.getEncoder().encodeToString(new byte[] { 1, 2,
				3 });
			final List<Content> contents = List.of(TextContent.from(
				"describe this image"), ImageContent.from(base64, "image/png"));

			assistant.chat(contents);

			final JsonObject message = JsonParser.parseString(requestBody.get()).getAsJsonObject()
				.getAsJsonArray("messages").get(0).getAsJsonObject();
			assertEquals("describe this image", message.get("content").getAsString());
			assertEquals(base64, message.getAsJsonArray("images").get(0).getAsString());
			assertFalse(message.get("content").getAsString().startsWith("ChatRequest {"));
			assertFalse(message.get("content").getAsString().startsWith("UserMessage {"));
		}
		finally {
			server.stop(0);
		}
	}
}
