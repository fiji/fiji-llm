package sc.fiji.llm.provider;

import java.util.function.Supplier;

import org.scijava.log.LogService;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.TokenCountEstimator;

public final class TimedTokenCountEstimator implements TokenCountEstimator {

	private final TokenCountEstimator delegate;
	private final String providerName;
	private final String modelName;
	private final LogService logService;

	public TimedTokenCountEstimator(final TokenCountEstimator delegate,
		final String providerName, final String modelName,
		final LogService logService)
	{
		this.delegate = delegate;
		this.providerName = providerName;
		this.modelName = modelName;
		this.logService = logService;
	}

	@Override
	public int estimateTokenCountInText(final String text) {
		return timed("text", () -> delegate.estimateTokenCountInText(text));
	}

	@Override
	public int estimateTokenCountInMessage(final ChatMessage message) {
		return timed("message", () -> delegate.estimateTokenCountInMessage(message));
	}

	@Override
	public int estimateTokenCountInMessages(final Iterable<ChatMessage> messages) {
		return timed("messages", () -> delegate.estimateTokenCountInMessages(messages));
	}

	private int timed(final String operation, final Supplier<Integer> action) {
		final long start = System.nanoTime();
		try {
			return action.get();
		}
		finally {
			final long elapsed = (System.nanoTime() - start) / 1_000_000;
			logService.debug("LLM timing token-count provider=" + providerName +
				" model=" + modelName + " operation=" + operation +
				" durationMs=" + elapsed);
		}
	}
}