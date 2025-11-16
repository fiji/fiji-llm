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

package sc.fiji.llm.ui;

import java.util.List;

import org.scijava.ui.swing.script.TextEditor;

/**
 * Small helper utilities for working with the SciJava TextEditor instances.
 */
public final class TextEditorUtils {

	private TextEditorUtils() {
		// utility
	}

	/**
	 * Return the index of the most-recent visible TextEditor instance, or -1 if
	 * none available. Iterates {@code TextEditor.instances} from the end because
	 * instances are appended when created and the newest instance is at the last
	 * index.
	 */
	public static int getMostRecentVisibleEditorIndex() {
		final List<TextEditor> instances = TextEditor.instances;
		if (instances == null || instances.isEmpty()) return -1;
		for (int i = instances.size() - 1; i >= 0; i--) {
			final TextEditor editor = instances.get(i);
			if (editor != null && editor.isVisible()) return i;
		}
		return -1;
	}

	/**
	 * Return the most-recent visible TextEditor instance, or null if none.
	 * Iterates {@code TextEditor.instances} from the end because instances are
	 * appended when created and the newest instance is at the last index.
	 */
	public static TextEditor getMostRecentVisibleEditor() {
		final int editorIndex = getMostRecentVisibleEditorIndex();
		return (editorIndex == -1) ? null : TextEditor.instances.get(editorIndex);
	}

	/**
	 * Adds line numbers to script content. Format: "NNN | content" where NNN is
	 * padded to match the total line count.
	 */
	public static String addLineNumbers(final String content) {
		final String[] lines = content.split("\n", -1);
		final StringBuilder sb = new StringBuilder();
		final int maxLineNumber = lines.length;
		final int padding = String.valueOf(maxLineNumber).length();

		for (int i = 0; i < lines.length; i++) {
			sb.append(String.format("%" + padding + "d | %s", i + 1, lines[i]))
				.append("\n");
		}

		return sb.toString();
	}

	/**
	 * Strips line numbers from script content, but only if EVERY line starts with
	 * the expected format. Expected format: "NNN | content" where NNN is a line
	 * number. Also strips markdown code fences (```language and ```) if present.
	 * Returns the original content unchanged if the format is not consistent.
	 * Returns empty string if content is null.
	 */
	public static String stripLineNumbers(final String content) {
		if (content == null) {
			return "";
		}

		// First, strip markdown code fences if present
		String cleanedContent = stripMarkdownCodeFences(content);

		final String[] lines = cleanedContent.split("\n", -1);

		// Quick check: if fewer than 2 lines, no consistent format to strip
		if (lines.length < 2) {
			return cleanedContent;
		}

		// Pattern: optional whitespace, digits, space, pipe, space
		// Extract the padding width from the first line
		final String firstLine = lines[0];
		final int pipeIndex = firstLine.indexOf('|');

		if (pipeIndex <= 0) {
			// No pipe found in first line, can't strip
			return cleanedContent;
		}

		// Check if everything before the pipe is whitespace + digits
		final String beforePipe = firstLine.substring(0, pipeIndex).trim();
		if (!beforePipe.matches("\\d+")) {
			// First line doesn't have line number format
			return cleanedContent;
		}

		// Expected padding width (including leading whitespace)
		final int expectedPadding = pipeIndex;

		// Verify ALL lines follow this pattern
		for (final String line : lines) {
			if (line.isEmpty()) {
				// Empty lines are okay - they should still have the line number prefix
			}
			else {
				// Check if line has the pipe at the expected position
				if (line.length() < expectedPadding + 3) {
					// Line too short to have proper format
					return cleanedContent;
				}
				final int linePipeIndex = line.indexOf('|', expectedPadding - 1);
				if (linePipeIndex != pipeIndex) {
					// Pipe not at expected position
					return cleanedContent;
				}
				final String lineNumPart = line.substring(0, pipeIndex).trim();
				if (!lineNumPart.matches("\\d+")) {
					// Line number part is not all digits
					return cleanedContent;
				}
			}
		}

		// All lines match the format - strip the line numbers
		final StringBuilder sb = new StringBuilder();
		for (final String line : lines) {
			if (line.isEmpty()) {
				sb.append("\n");
			}
			else {
				final int contentStart = pipeIndex + 1;
				if (contentStart < line.length() && line.charAt(contentStart) == ' ') {
					// Skip the leading space after the pipe
					sb.append(line.substring(contentStart + 1)).append("\n");
				}
				else {
					sb.append(line.substring(contentStart)).append("\n");
				}
			}
		}

		final String result = sb.toString();
		// Remove the trailing newline we added if original didn't have one
		if (!cleanedContent.endsWith("\n") && result.endsWith("\n")) {
			return result.substring(0, result.length() - 1);
		}
		return result;
	}

	/**
	 * Strips markdown code fences from content if present. Handles both ``` and
	 * ```language formats. Returns the original content if no code fences are
	 * found.
	 */
	private static String stripMarkdownCodeFences(final String content) {
		if (content == null || content.isEmpty()) {
			return content;
		}

		final String trimmed = content.trim();

		// Check if content starts with ``` (with optional language)
		if (trimmed.startsWith("```")) {
			// Find the end of the first line (the opening fence)
			final int firstNewline = trimmed.indexOf('\n');
			if (firstNewline == -1) {
				// Only a code fence, no actual content
				return "";
			}

			// Check if content ends with ```
			if (trimmed.endsWith("```")) {
				// Find the start of the last line (the closing fence)
				final int lastFenceStart = trimmed.lastIndexOf("\n```");
				if (lastFenceStart > firstNewline) {
					// Extract content between fences
					return trimmed.substring(firstNewline + 1, lastFenceStart);
				}
			}
		}

		return content;
	}
}
