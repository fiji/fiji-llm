package sc.fiji.llm.tools;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

public final class DeadlineToolExecutor implements ToolExecutor {

	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(
		task -> {
			Thread thread = new Thread(task, "Fiji-AI-Tool");
			thread.setDaemon(true);
			return thread;
		});

	private final ToolExecutor delegate;
	private final Duration timeout;

	public DeadlineToolExecutor(final ToolExecutor delegate) {
		this(delegate, DEFAULT_TIMEOUT);
	}

	public DeadlineToolExecutor(final ToolExecutor delegate,
		final Duration timeout)
	{
		this.delegate = delegate;
		this.timeout = timeout;
	}

	@Override
	public String execute(final ToolExecutionRequest request,
		final Object memoryId)
	{
		final Future<String> future = EXECUTOR.submit(() -> delegate.execute(request,
			memoryId));
		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new RuntimeException("Tool execution interrupted: " + request.name(), e);
		}
		catch (TimeoutException e) {
			future.cancel(true);
			throw new RuntimeException("Tool execution exceeded " + timeout.toSeconds() +
				" seconds: " + request.name(), e);
		}
		catch (ExecutionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new RuntimeException("Tool execution failed: " + request.name(), cause);
		}
	}
}