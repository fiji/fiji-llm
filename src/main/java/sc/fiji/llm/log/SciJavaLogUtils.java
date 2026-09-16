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

package sc.fiji.llm.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.scijava.log.LogListener;
import org.scijava.log.LogMessage;
import org.scijava.log.LogService;

/** Utilities for capturing structured SciJava log messages during an operation. */
public final class SciJavaLogUtils {

	private SciJavaLogUtils() {
		// utility class
	}

	/** Starts capturing messages emitted by the supplied log service. */
	public static LogCapture capture(final LogService logService) {
		return new LogCapture(logService);
	}

	public static final class LogCapture implements AutoCloseable {

		private final LogService logService;
		private final ConcurrentLinkedQueue<LogMessage> messages =
			new ConcurrentLinkedQueue<>();
		private final AtomicBoolean closed = new AtomicBoolean();

		private final LogListener listener = messages::add;

		private LogCapture(final LogService logService) {
			if (logService == null) throw new IllegalArgumentException(
				"Log service cannot be null");
			this.logService = logService;
			logService.addLogListener(listener);
		}

		/** Returns a point-in-time snapshot of captured messages. */
		public LogMessages getLogs() {
			return new LogMessages(new ArrayList<>(messages));
		}

		/** Stops capturing messages. Safe to call more than once. */
		@Override
		public void close() {
			if (closed.compareAndSet(false, true)) logService.removeLogListener(
				listener);
		}
	}

	public static final class LogMessages {

		private final List<LogMessage> messages;

		private LogMessages(final List<LogMessage> messages) {
			this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
		}

		public List<LogMessage> getMessages() {
			return messages;
		}

		/** Returns the captured messages rendered using SciJava's log format. */
		public String getText() {
			final StringBuilder text = new StringBuilder();
			for (final LogMessage message : messages) {
				text.append(message.toString());
			}
			return text.toString();
		}

		/**
		 * Returns messages captured after the initial snapshot. Snapshots must come
		 * from the same {@link LogCapture} instance.
		 */
		public LogMessages deltaFrom(final LogMessages initial) {
			if (initial == null || initial.messages.size() > messages.size()) {
				return this;
			}
			return new LogMessages(messages.subList(initial.messages.size(), messages
				.size()));
		}
	}
}
