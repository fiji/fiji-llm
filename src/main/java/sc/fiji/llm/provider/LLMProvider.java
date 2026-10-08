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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jsoup.Jsoup;
import org.scijava.Disposable;
import org.scijava.Initializable;
import org.scijava.plugin.SingletonPlugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

/**
 * Plugin interface for LLM providers. Each provider (OpenAI, Anthropic, Google,
 * etc.) implements this interface to provide access to their chat models.
 */
public interface LLMProvider extends SingletonPlugin, Initializable,
	Disposable
{

	/** Default timeout duration for API calls */
	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

	/** Default maximum number of retries for API calls */
	public static int DEFAULT_MAX_RETRIES = 0;

	public static final String VALIDATION_FAILED =
		"sc.fiji.llm.provider.validation_failed";

	/** The provider's knowledge of a model's image-input support. */
	public enum VisionSupport {
		SUPPORTED, UNSUPPORTED, UNKNOWN
	}

	/**
	 * Standard token pricing in US dollars per million tokens.
	 *
	 * @param inputPerMillionTokens input token price
	 * @param outputPerMillionTokens output token price
	 */
	record ModelCost(double inputPerMillionTokens, double outputPerMillionTokens) {}

	/**
	 * Approximate runtime memory demand curve for a model.
	 *
	 * @param baseGiB estimated memory demand before context storage
	 * @param contextGiBPerToken estimated additional memory per context token
	 */
	record ModelDemand(double baseGiB, double contextGiBPerToken)
	{

		/**
		 * Estimates the runtime memory demand at a context size.
		 *
		 * @param contextTokens context size to estimate
		 * @return estimated runtime memory demand in GiB
		 */
		public double estimateGiB(final int contextTokens) {
			return baseGiB + contextGiBPerToken * contextTokens;
		}
	}

	/**
	 * The provider and model labels used when displaying a selected model.
	 *
	 * @param provider the provider label
	 * @param model the model label
	 */
	record ModelDisplay(String provider, String model) {}

	/**
	 * Reports whether a model accepts image content in chat messages.
	 *
	 * @param modelName the model name
	 * @return the known vision capability, or {@link VisionSupport#UNKNOWN}
	 */
	default VisionSupport getVisionSupport(final String modelName) {
		return VisionSupport.UNKNOWN;
	}

	/**
	 * Reports whether a model is known to accept image content.
	 *
	 * @param modelName the model name
	 * @return true only when image support is known
	 */
	default boolean supportsVision(final String modelName) {
		return getVisionSupport(modelName) == VisionSupport.SUPPORTED;
	}

	/**
	 * Reports whether a model transport accepts image content in tool-result
	 * messages.
	 *
	 * @param modelName the model name
	 * @return true when image-bearing tool results can be sent natively
	 */
	default boolean supportsImageToolResults(final String modelName) {
		return false;
	}

	/**
	 * @return True if this model requires an API key (i.e. cloud-based models)
	 */
	default boolean requiresApiKey() {
		return true;
	}

	/**
	 * Hook for when a model requires additional actions. This is a transformative
	 * action, allowing for descriptive identifiers attached to model names that
	 * require validation. (e.g. when downloading a remote model)
	 *
	 * @param modelToValidate Name of the model for validation
	 * @return The validated model name, or {@link #VALIDATION_FAILED} if
	 *         validation wasunsuccessful.
	 */
	default String validateModel(String modelToValidate) {
		return modelToValidate;
	}

	/**
	 * @param modelName the name of the model to configure
	 * @return the base {@link ChatRequestParameters} recommended for this model
	 */
	default ChatRequestParameters defaultChatRequestParameters(
		final String modelName)
	{
		return ChatRequestParameters.builder().frequencyPenalty(0.0)
			.presencePenalty(0.0).temperature(0.1).build();
	}

	/**
	 * Prepare a model for use. Providers that do not need model-specific
	 * preparation complete immediately.
	 *
	 * @param modelName the name of the model to prepare
	 * @return a stage that completes with an optional user-facing message when the
	 *         model is ready; preparation failures complete the stage exceptionally
	 */
	default CompletionStage<String> prepare(final String modelName) {
		return CompletableFuture.completedFuture("");
	}

	/**
	 * Check whether a model is currently prepared for use. Providers that do not
	 * unload models complete immediately with {@code true}.
	 *
	 * @param modelName the name of the model to check
	 * @return a stage containing whether the model is ready
	 */
	default CompletionStage<Boolean> isPrepared(final String modelName) {
		return CompletableFuture.completedFuture(true);
	}

	/**
	 * Report if this provider supports model selection.
	 *
	 * @return true if this LLM provider allows model selection
	 */
	default boolean supportsModelSelection() {
		return true;
	}

	/**
	 * Get the standard input and output token pricing for a model.
	 *
	 * @param modelName the model name
	 * @return the model pricing, if known
	 */
	default Optional<ModelCost> getCost(final String modelName) {
		return Optional.empty();
	}

	/**
	 * Gets the approximate local runtime memory demand for a model.
	 *
	 * @param modelName the model name
	 * @return the model demand, if known
	 */
	default Optional<ModelDemand> getDemand(final String modelName) {
		return Optional.empty();
	}

	/**
	 * Get the name of this provider.
	 *
	 * @return the provider name (e.g., "OpenAI", "Anthropic", "Google")
	 */
	String getName();

	/**
	 * Get the provider and model labels used when displaying the selected model.
	 *
	 * @param modelName the configured model name
	 * @return the labels to show in the chat window
	 */
	default ModelDisplay getModelDisplay(final String modelName) {
		return new ModelDisplay(getName(), modelName);
	}

	/**
	 * Get a description of this provider.
	 *
	 * @return a human-readable description
	 */
	String getDescription();

	/**
	 * Get the list of available models for this provider.
	 *
	 * @return list of model names
	 */
	List<String> getAvailableModels();

	/**
	 * Get the URL to the provider's models documentation.
	 *
	 * @return URL to the models documentation page
	 */
	String getModelsDocumentationUrl();

	/**
	 * Create a stable hash of the model facts in the provider's documentation.
	 * Providers may parse the raw HTML to exclude volatile prose while retaining
	 * model identifiers, prices, and lifecycle information.
	 *
	 * @param html the raw models documentation HTML
	 * @return a SHA-256 hash of the provider's model facts
	 * @throws IOException if the documentation cannot be parsed or hashed
	 */
	default String getModelsDocumentationContentHash(final String html)
		throws IOException
	{
		return hashNormalizedDocumentationText(Jsoup.parse(html).text());
	}

	/**
	 * Reports whether the probe should fetch and hash the documentation body for
	 * the supplied {@code Last-Modified} value.
	 *
	 * @param lastModified the documentation response's {@code Last-Modified} value
	 * @return true when the documentation body should be checked
	 */
	default boolean shouldCheckModelsDocumentationContent(
		final String lastModified)
	{
		return true;
	}

	/**
	 * Reports whether a changed documentation {@code Last-Modified} value should
	 * independently trigger model review.
	 *
	 * @return true when the header change is itself review-worthy
	 */
	default boolean shouldReviewModelsDocumentationLastModified() {
		return true;
	}

	/**
	 * Hash normalized documentation text for provider-specific model parsers.
	 *
	 * @param text the canonical model facts to hash
	 * @return a SHA-256 hash
	 * @throws IOException if SHA-256 is unavailable
	 */
	static String hashNormalizedDocumentationText(final String text)
		throws IOException
	{
		try {
			final byte[] normalizedText = text.replaceAll("\\s+", " ").trim()
				.getBytes(StandardCharsets.UTF_8);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(normalizedText));
		}
		catch (final NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 is unavailable", e);
		}
	}

	/**
	 * Get the date on which the models documentation was last checked against the
	 * models supplied by this provider.
	 *
	 * @return the date recorded by the provider maintainer
	 */
	LocalDate getModelsDocumentationLastModified();

	/**
	 * Get the URL where users can obtain an API key for this provider.
	 *
	 * @return URL to the API key page
	 */
	String getApiKeyUrl();

	/**
	 * @return A {@link TokenWindowChatMemory} appropriate for the specified
	 *         model, or {@code null} if not supported.
	 */
	TokenWindowChatMemory createTokenChatMemory(String modelName);

	/**
	 * Create a chat language model with the specified API key and model name.
	 *
	 * @param modelName the name of the model to use
	 * @return a configured chat language model
	 */
	ChatModel createChatModel(String modelName);

	/**
	 * Create a streaming chat language model with the specified API key and model
	 * name.
	 *
	 * @param modelName the name of the model to use
	 * @return a configured streaming chat language model
	 */
	StreamingChatModel createStreamingChatModel(String modelName);
}
