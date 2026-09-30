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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sc.fiji.llm.Setup;

public class HostedProviderModelHealthTest {

	private static final String OUTPUT_PROPERTY = "provider.url.check.output";
	private static final String PREVIOUS_STATE_PROPERTY =
		"provider.url.check.previous-state";
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.connectTimeout(REQUEST_TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	private Context context;

	@Before
	public void setUp() {
		context = Setup.context();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void providerUrlsAreReachableAndUnchanged() throws Exception {
		final String outputProperty = System.getProperty(OUTPUT_PROPERTY);
		final String previousStateProperty = System.getProperty(
			PREVIOUS_STATE_PROPERTY);
		Assume.assumeTrue("This network check is enabled explicitly", outputProperty !=
			null && previousStateProperty != null);

		final JsonObject previousState = readState(Path.of(previousStateProperty));
		final JsonObject currentState = new JsonObject();
		final List<String> failures = new ArrayList<>();
		final ProviderService providerService = context.getService(
			ProviderService.class);

		for (final LLMProvider provider : providerService.getInstances()) {
			if (!provider.requiresApiKey()) continue;

			final JsonObject providerState = new JsonObject();
			providerState.addProperty("models_documentation_last_modified", provider
				.getModelsDocumentationLastModified().toString());
			final JsonObject previousProviderState = previousState.has(provider
				.getName()) ? previousState.getAsJsonObject(provider.getName()) : null;
			checkUrl(provider, "models_documentation", provider
				.getModelsDocumentationUrl(), providerState, previousProviderState,
				failures);
			checkUrl(provider, "api_key", provider.getApiKeyUrl(), providerState,
				previousProviderState, failures);
			carryForwardModelReview(provider, providerState, previousProviderState,
				failures);
			currentState.add(provider.getName(), providerState);
		}

		currentState.addProperty("checked_at", Instant.now().toString());
		writeState(Path.of(outputProperty), currentState);
		if (!failures.isEmpty()) fail(String.join(System.lineSeparator(), failures));
	}

	private static void carryForwardModelReview(final LLMProvider provider,
		final JsonObject currentProviderState,
		final JsonObject previousProviderState, final List<String> failures)
	{
		if (previousProviderState == null || !previousProviderState.has(
			"models_documentation_last_modified")) return;
		if (!currentProviderState.get("models_documentation_last_modified").equals(
			previousProviderState.get("models_documentation_last_modified"))) return;

		final JsonObject previousModels = previousProviderState.has(
			"models_documentation") ? previousProviderState.getAsJsonObject(
				"models_documentation") : null;
		if (previousModels == null || !previousModels.has("models_review_required") ||
			!previousModels.get("models_review_required").getAsBoolean()) return;

		final JsonObject currentModels = currentProviderState.getAsJsonObject(
			"models_documentation");
		currentModels.addProperty("models_review_required", true);
		failures.add(provider.getName() +
			" models documentation still requires review; update " +
			"getModelsDocumentationLastModified() after reviewing it");
	}

	private static void checkUrl(final LLMProvider provider, final String name,
		final String url, final JsonObject currentProviderState,
		final JsonObject previousProviderState, final List<String> failures)
	{
		final JsonObject current = new JsonObject();
		current.addProperty("url", url == null ? "" : url);
		if (url == null || url.isBlank()) {
			current.addProperty("status", "not_applicable");
			currentProviderState.add(name, current);
			return;
		}

		try {
			final HttpResponse<Void> response = request(URI.create(url));
			final int status = response.statusCode();
			current.addProperty("status", status);
			current.addProperty("final_url", response.uri().toString());
			response.headers().firstValue("Last-Modified").ifPresent(value -> current
				.addProperty("last_modified", value));
			if ("models_documentation".equals(name) && !isBrokenStatus(status)) {
				checkModelsDocumentationContent(provider, response.uri(), current,
					failures);
			}
			if (isBrokenStatus(status)) failures.add(provider.getName() + " " + name +
				" returned HTTP " + status + " for " + url);
		}
		catch (final Exception e) {
			current.addProperty("error", e.toString());
			failures.add(provider.getName() + " " + name + " failed for " + url +
				": " + e.getMessage());
		}

		compareWithPrevious(provider, name, current, previousProviderState,
			failures);
		currentProviderState.add(name, current);
	}

	private static void checkModelsDocumentationContent(final LLMProvider provider,
		final URI uri,
		final JsonObject current, final List<String> failures)
	{
		try {
			final HttpRequest request = HttpRequest.newBuilder(uri).timeout(
				REQUEST_TIMEOUT).GET().build();
			final HttpResponse<String> response = HTTP_CLIENT.send(request,
				HttpResponse.BodyHandlers.ofString());
			if (isBrokenStatus(response.statusCode())) {
				throw new IOException("GET returned HTTP " + response.statusCode());
			}

			final Element content = Jsoup.parse(response.body(), uri.toString()).select(
				provider.getModelsDocumentationContentSelector()).first();
			if (content == null) {
				throw new IOException("selector did not match: " + provider
					.getModelsDocumentationContentSelector());
			}

			current.addProperty("content_selector", provider
				.getModelsDocumentationContentSelector());
			current.addProperty("content_hash", contentHash(content));
		}
		catch (final IOException | InterruptedException e) {
			current.addProperty("content_error", e.toString());
			failures.add(provider.getName() + " models documentation content check failed: " +
				e.getMessage());
		}
	}

	private static String contentHash(final Element content) throws IOException {
		try {
			final byte[] normalizedContent = content.text().replaceAll("\\s+", " ")
				.trim().getBytes(StandardCharsets.UTF_8);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(normalizedContent));
		}
		catch (final NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 is unavailable", e);
		}
	}

	private static HttpResponse<Void> request(final URI uri) throws IOException,
		InterruptedException
	{
		final HttpResponse<Void> headResponse = send(uri, "HEAD");
		if (headResponse.statusCode() != 405 && headResponse.statusCode() != 501)
			return headResponse;
		return send(uri, "GET");
	}

	private static HttpResponse<Void> send(final URI uri, final String method)
		throws IOException, InterruptedException
	{
		final HttpRequest request = HttpRequest.newBuilder(uri).timeout(
			REQUEST_TIMEOUT).method(method, HttpRequest.BodyPublishers.noBody()).build();
		return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
	}

	private static boolean isBrokenStatus(final int status) {
		return status == 400 || status == 404 || status == 408 || status == 410 ||
			status >= 500;
	}

	private static void compareWithPrevious(final LLMProvider provider,
		final String name, final JsonObject current,
		final JsonObject previousProviderState, final List<String> failures)
	{
		if (previousProviderState == null || !previousProviderState.has(name)) return;
		final JsonObject previous = previousProviderState.getAsJsonObject(name);
		for (final String field : List.of("url", "status", "final_url",
			"last_modified", "content_hash"))
		{
			final JsonElement previousValue = previous.has(field) ? previous.get(field) :
				JsonNull.INSTANCE;
			final JsonElement currentValue = current.has(field) ? current.get(field) :
				JsonNull.INSTANCE;
			if (!previousValue.equals(currentValue)) {
				current.addProperty("changed_since_previous", true);
				if ("models_documentation".equals(name) && ("last_modified".equals(
					field) || "content_hash".equals(field)))
				{
					current.addProperty("models_review_required", true);
				}
				final String action = "models_documentation".equals(name) &&
					("last_modified".equals(field) || "content_hash".equals(field)) ?
					"; re-evaluate the provided models" :
					"";
				failures.add(provider.getName() + " " + name + " changed " + field +
					" from " + previousValue + " to " + currentValue + action);
			}
		}
	}

	private static JsonObject readState(final Path path) throws IOException {
		if (!Files.isRegularFile(path)) return new JsonObject();
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}

	private static void writeState(final Path path, final JsonObject state)
		throws IOException
	{
		final Path parent = path.getParent();
		if (parent != null) Files.createDirectories(parent);
		Files.writeString(path, GSON.toJson(state));
	}
}
