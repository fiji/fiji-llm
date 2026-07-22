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

package sc.fiji.llm.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a range of lines.
 */
public class LineRange {

	public static final int UNSET = -1;

	private final int start;
	private final int end;

	public LineRange(final int start, final int end) {
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final LineRange other = (LineRange) obj;
		return start == other.start && end == other.end;
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, end);
	}

	@Override
	public String toString() {
		return start + "-" + end;
	}

	/**
	 * Merges overlapping or adjacent line ranges.
	 */
	public static List<LineRange> mergeRanges(final List<LineRange> ranges) {
		if (ranges.isEmpty()) {
			return ranges;
		}

		final List<LineRange> merged = new ArrayList<>();
		LineRange current = ranges.get(0);

		for (int i = 1; i < ranges.size(); i++) {
			final LineRange next = ranges.get(i);
			if (current.getEnd() >= next.getStart() - 1) {
				// Overlapping or adjacent - merge them
				current = new LineRange(current.getStart(), Math.max(current.getEnd(),
					next.getEnd()));
			}
			else {
				// Gap - save current and start new
				merged.add(current);
				current = next;
			}
		}
		merged.add(current);
		return merged;
	}

}
