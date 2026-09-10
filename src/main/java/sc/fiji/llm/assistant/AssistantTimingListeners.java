package sc.fiji.llm.assistant;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.scijava.log.LogService;

import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceEvent;
import dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;

public final class AssistantTimingListeners {

	private AssistantTimingListeners() {}

	public static AiServiceListener<?>[] create(final LogService logService,
		final String providerName, final String modelName)
	{
		final State state = new State();
		return new AiServiceListener<?>[] {
			new RequestListener(state, logService, providerName, modelName),
			new ResponseListener(state, logService, providerName, modelName),
			new CompleteListener(state, logService, providerName, modelName),
			new ErrorListener(state, logService, providerName, modelName)
		};
	}

	private static final class State {

		private final Map<UUID, Long> invocationStarts = new ConcurrentHashMap<>();
		private final Map<UUID, Long> requestStarts = new ConcurrentHashMap<>();
		private final Map<UUID, AtomicInteger> rounds = new ConcurrentHashMap<>();
	}

	private abstract static class Listener<T extends AiServiceEvent> implements
		AiServiceListener<T>
	{

		protected final State state;
		protected final LogService logService;
		protected final String providerName;
		protected final String modelName;

		Listener(final State state, final LogService logService,
			final String providerName, final String modelName)
		{
			this.state = state;
			this.logService = logService;
			this.providerName = providerName;
			this.modelName = modelName;
		}

		protected void log(final String message) {
			logService.debug("LLM timing provider=" + providerName + " model=" +
				modelName + " " + message);
		}
	}

	private static final class RequestListener extends
		Listener<AiServiceRequestIssuedEvent>
	{

		RequestListener(final State state, final LogService logService,
			final String providerName, final String modelName)
		{
			super(state, logService, providerName, modelName);
		}

		@Override
		public Class<AiServiceRequestIssuedEvent> getEventClass() {
			return AiServiceRequestIssuedEvent.class;
		}

		@Override
		public void onEvent(final AiServiceRequestIssuedEvent event) {
			final UUID id = event.invocationContext().invocationId();
			final long now = System.nanoTime();
			state.invocationStarts.putIfAbsent(id, now);
			state.requestStarts.put(id, now);
			final int round = state.rounds.computeIfAbsent(id,
				ignored -> new AtomicInteger()).incrementAndGet();
			log("model-round=" + round + " started invocation=" + id);
		}
	}

	private static final class ResponseListener extends
		Listener<AiServiceResponseReceivedEvent>
	{

		ResponseListener(final State state, final LogService logService,
			final String providerName, final String modelName)
		{
			super(state, logService, providerName, modelName);
		}

		@Override
		public Class<AiServiceResponseReceivedEvent> getEventClass() {
			return AiServiceResponseReceivedEvent.class;
		}

		@Override
		public void onEvent(final AiServiceResponseReceivedEvent event) {
			final UUID id = event.invocationContext().invocationId();
			final Long start = state.requestStarts.remove(id);
			final long elapsed = start == null ? -1 : (System.nanoTime() - start) /
				1_000_000;
			log("model-round response invocation=" + id + " durationMs=" + elapsed);
		}
	}

	private static final class CompleteListener extends
		Listener<AiServiceCompletedEvent>
	{

		CompleteListener(final State state, final LogService logService,
			final String providerName, final String modelName)
		{
			super(state, logService, providerName, modelName);
		}

		@Override
		public Class<AiServiceCompletedEvent> getEventClass() {
			return AiServiceCompletedEvent.class;
		}

		@Override
		public void onEvent(final AiServiceCompletedEvent event) {
			final UUID id = event.invocationContext().invocationId();
			final Long start = state.invocationStarts.remove(id);
			final long elapsed = start == null ? -1 : (System.nanoTime() - start) /
				1_000_000;
			state.requestStarts.remove(id);
			state.rounds.remove(id);
			log("completed invocation=" + id + " durationMs=" + elapsed);
		}
	}

	private static final class ErrorListener extends Listener<AiServiceErrorEvent> {

		ErrorListener(final State state, final LogService logService,
			final String providerName, final String modelName)
		{
			super(state, logService, providerName, modelName);
		}

		@Override
		public Class<AiServiceErrorEvent> getEventClass() {
			return AiServiceErrorEvent.class;
		}

		@Override
		public void onEvent(final AiServiceErrorEvent event) {
			final UUID id = event.invocationContext().invocationId();
			final Long start = state.invocationStarts.remove(id);
			final long elapsed = start == null ? -1 : (System.nanoTime() - start) /
				1_000_000;
			state.requestStarts.remove(id);
			state.rounds.remove(id);
			log("failed invocation=" + id + " durationMs=" + elapsed + " error=" +
				event.error().getClass().getSimpleName());
		}
	}
}