package sc.fiji.llm.assistant;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest.Builder;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import sc.fiji.llm.auth.APIKeyService;
import sc.fiji.llm.provider.LLMProvider;
import sc.fiji.llm.provider.ProviderService;
import sc.fiji.llm.tools.AiToolService;

/**
 * Default implementation of AssistantService.
 */
@Plugin(type = Service.class)
public class DefaultAssistantService extends AbstractService implements AssistantService {

	@Parameter
	private ProviderService providerService;

	@Parameter
	private APIKeyService apiKeyService;

	@Parameter
	private AiToolService toolService;

	@Parameter
	private LogService logService;

	@Override
	public <T> T createAssistant(final Class<T> assistantInterface, final String providerName, final String modelName, final ChatMemory chatMemory, final ChatRequestParameters defaultChatParameters) {
		final LLMProvider provider = providerService.getProvider(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Provider not found: " + providerName);
		}

		final var builder = AiServices.builder(assistantInterface)
			.streamingChatModel(provider.createStreamingChatModel(modelName))
			.toolExecutionErrorHandler(this::handleExecutionError)
			.toolArgumentsErrorHandler(this::handleArgumentError)
			.chatModel(provider.createChatModel(modelName))
			.tools(toolService.getInstances().toArray());

		// Apply request parameters at AiServices level where they'll be used
		if (defaultChatParameters != null) {
			builder.chatRequestTransformer(chatRequest -> {
				Builder chatTransformBuilder = chatRequest.toBuilder();
				chatTransformBuilder.parameters(defaultChatParameters.overrideWith(chatRequest.parameters()));
				return chatTransformBuilder.build();
			});
		}
		if (chatMemory != null) {
			builder.chatMemory(chatMemory);
		}

		return builder.build();
	}

	public ToolErrorHandlerResult handleExecutionError(Throwable error, ToolErrorContext context) {
		return handle("Tool execution error", error, context);
	}

	public ToolErrorHandlerResult handleArgumentError(Throwable error, ToolErrorContext context) {
		return handle("Tool argument error", error, context);
	}

	private ToolErrorHandlerResult handle(String message, Throwable error, ToolErrorContext context) {
		logService.error(message, error);
		return new ToolErrorHandlerResult("I encountered an issue. Please try again. If the problem persists, please contact the developers.");
	}
}
