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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
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

import java.lang.reflect.Method;

import net.imagej.legacy.LegacyService;

/** Utilities for reading ImageJ1's global Log window. */
public final class ImageJLogUtils {

	private ImageJLogUtils() {
		// utility class
	}

	/** Returns a snapshot of the current ImageJ Log window contents. */
	public static ImageJLog getLog(final LegacyService legacyService) {
		if (legacyService == null || !legacyService.isActive()) {
			return new ImageJLog(null, false);
		}

		try {
			final Class<?> ijClass = Class.forName("ij.IJ");
			final Method getLog = ijClass.getMethod("getLog");
			final Object text = getLog.invoke(null);
			return new ImageJLog(text instanceof String ? (String) text : null,
				text != null);
		}
		catch (ReflectiveOperationException | LinkageError e) {
			return new ImageJLog(null, false);
		}
	}

	public static final class ImageJLog {

		private final String text;
		private final boolean open;

		private ImageJLog(final String text, final boolean open) {
			this.text = text == null ? "" : text;
			this.open = open;
		}

		public String getText() {
			return text;
		}

		public boolean isOpen() {
			return open;
		}

		public ImageJLog deltaFrom(final ImageJLog initial) {
			return new ImageJLog(LogUtils.delta(initial == null ? "" : initial.text,
				text), open);
		}
	}
}