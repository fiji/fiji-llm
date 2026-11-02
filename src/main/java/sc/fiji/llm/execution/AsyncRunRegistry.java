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
package sc.fiji.llm.execution;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Tracks active asynchronous runs and retains terminal results for a bounded period. */
public final class AsyncRunRegistry<K, V> {

	/** Retention period for a terminal run result. */
	public static final long DEFAULT_RETENTION_MILLIS = TimeUnit.MINUTES.toMillis(10);

	private static final long CLEANUP_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);
	private static final ScheduledExecutorService CLEANUP_SERVICE = Executors
		.newSingleThreadScheduledExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "Fiji-async-run-cleanup");
			thread.setDaemon(true);
			return thread;
		});

	private final Map<String, Entry<K, V>> entries = new HashMap<>();
	private final Map<K, String> activeRunIDs = new HashMap<>();
	private final long retentionMillis;
	private final LongSupplier currentTimeMillis;

	/** Creates a registry with the default terminal-result retention period. */
	public AsyncRunRegistry() {
		this(DEFAULT_RETENTION_MILLIS, System::currentTimeMillis, true);
	}

	AsyncRunRegistry(final long retentionMillis, final LongSupplier currentTimeMillis,
		final boolean scheduleCleanup)
	{
		if (retentionMillis <= 0) throw new IllegalArgumentException(
			"retentionMillis must be positive");
		this.retentionMillis = retentionMillis;
		this.currentTimeMillis = Objects.requireNonNull(currentTimeMillis);
		if (scheduleCleanup) CLEANUP_SERVICE.scheduleWithFixedDelay(this::removeExpiredEntries,
			CLEANUP_INTERVAL_MILLIS, CLEANUP_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Registers a run if no run of the same category is active.
	 *
	 * @param category category whose runs share an active slot
	 * @param runID unique identifier for this run
	 * @param value run information to retain
	 * @throws RunInProgressException if another run in the category is active
	 */
	public synchronized void start(final K category, final String runID,
		final V value)
	{
		Objects.requireNonNull(category);
		Objects.requireNonNull(runID);
		Objects.requireNonNull(value);
		removeExpiredEntries();
		final String activeRunID = activeRunIDs.get(category);
		if (activeRunID != null) throw new RunInProgressException(activeRunID);
		if (entries.containsKey(runID)) throw new IllegalArgumentException(
			"runID is already registered: " + runID);
		entries.put(runID, new Entry<>(category, value));
		activeRunIDs.put(category, runID);
	}

	/** Returns an active or retained run, or null when it is unknown or expired. */
	public synchronized V get(final String runID) {
		removeExpiredEntries();
		final Entry<K, V> entry = entries.get(runID);
		return entry == null ? null : entry.value;
	}

	/** Returns the active run in a category, or null when that category is idle. */
	public synchronized V getActive(final K category) {
		removeExpiredEntries();
		final String runID = activeRunIDs.get(category);
		final Entry<K, V> entry = runID == null ? null : entries.get(runID);
		return entry == null ? null : entry.value;
	}

	/** Marks a run terminal and releases its category while retaining its result. */
	public synchronized void complete(final String runID) {
		final Entry<K, V> entry = entries.get(runID);
		if (entry == null || entry.completedAt >= 0) return;
		entry.completedAt = currentTimeMillis.getAsLong();
		activeRunIDs.remove(entry.category, runID);
	}

	/** Removes a run that failed before it became externally observable. */
	public synchronized void remove(final String runID) {
		final Entry<K, V> entry = entries.remove(runID);
		if (entry != null) activeRunIDs.remove(entry.category, runID);
	}

	private synchronized void removeExpiredEntries() {
		final long now = currentTimeMillis.getAsLong();
		final Iterator<Map.Entry<String, Entry<K, V>>> iterator = entries.entrySet()
			.iterator();
		while (iterator.hasNext()) {
			final Map.Entry<String, Entry<K, V>> entry = iterator.next();
			final long completedAt = entry.getValue().completedAt;
			if (completedAt >= 0 && now - completedAt >= retentionMillis) iterator.remove();
		}
	}

	private static final class Entry<K, V> {

		private final K category;
		private final V value;
		private long completedAt = -1;

		private Entry(final K category, final V value) {
			this.category = category;
			this.value = value;
		}
	}

	/** Signals that a category already has an active run. */
	public static final class RunInProgressException extends IllegalStateException {

		private static final long serialVersionUID = 1L;

		private final String activeRunID;

		private RunInProgressException(final String activeRunID) {
			super("A run is already active: " + activeRunID);
			this.activeRunID = activeRunID;
		}

		/** Returns the run ID that currently occupies the category. */
		public String getActiveRunID() {
			return activeRunID;
		}
	}
}
