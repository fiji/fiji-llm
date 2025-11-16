/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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

    public int getStart()
    {
        return start;
    }

    public int getEnd()
    {
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
				current = new LineRange(current.getStart(), Math.max(current.getEnd(), next.getEnd()));
			} else {
				// Gap - save current and start new
				merged.add(current);
				current = next;
			}
		}
		merged.add(current);
		return merged;
	}


}
