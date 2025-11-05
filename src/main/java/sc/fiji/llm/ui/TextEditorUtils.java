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
     * none available. Iterates {@code TextEditor.instances} from the end
     * because instances are appended when created and the newest instance is at
     * the last index.
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
     * Adds line numbers to script content.
     * Format: "NNN | content" where NNN is padded to match the total line count.
     */
    public static String addLineNumbers(final String content) {
        final String[] lines = content.split("\n", -1);
        final StringBuilder sb = new StringBuilder();
        final int maxLineNumber = lines.length;
        final int padding = String.valueOf(maxLineNumber).length();

        for (int i = 0; i < lines.length; i++) {
            sb.append(String.format("%" + padding + "d | %s", i + 1, lines[i])).append("\n");
        }

        return sb.toString();
    }

    /**
     * Strips line numbers from script content, but only if EVERY line starts with the expected format.
     * Expected format: "NNN | content" where NNN is a line number.
     * Returns the original content unchanged if the format is not consistent.
     */
    public static String stripLineNumbers(final String content) {
        final String[] lines = content.split("\n", -1);

        // Quick check: if fewer than 2 lines, no consistent format to strip
        if (lines.length < 2) {
            return content;
        }

        // Pattern: optional whitespace, digits, space, pipe, space
        // Extract the padding width from the first line
        final String firstLine = lines[0];
        final int pipeIndex = firstLine.indexOf('|');

        if (pipeIndex <= 0) {
            // No pipe found in first line, can't strip
            return content;
        }

        // Check if everything before the pipe is whitespace + digits
        final String beforePipe = firstLine.substring(0, pipeIndex).trim();
        if (!beforePipe.matches("\\d+")) {
            // First line doesn't have line number format
            return content;
        }

        // Expected padding width (including leading whitespace)
        final int expectedPadding = pipeIndex;

        // Verify ALL lines follow this pattern
        for (final String line : lines) {
            if (line.isEmpty()) {
                // Empty lines are okay - they should still have the line number prefix
            } else {
                // Check if line has the pipe at the expected position
                if (line.length() < expectedPadding + 3) {
                    // Line too short to have proper format
                    return content;
                }
                final int linePipeIndex = line.indexOf('|', expectedPadding - 1);
                if (linePipeIndex != pipeIndex) {
                    // Pipe not at expected position
                    return content;
                }
                final String lineNumPart = line.substring(0, pipeIndex).trim();
                if (!lineNumPart.matches("\\d+")) {
                    // Line number part is not all digits
                    return content;
                }
            }
        }

        // All lines match the format - strip the line numbers
        final StringBuilder sb = new StringBuilder();
        for (final String line : lines) {
            if (line.isEmpty()) {
                sb.append("\n");
            } else {
                final int contentStart = pipeIndex + 1;
                if (contentStart < line.length() && line.charAt(contentStart) == ' ') {
                    // Skip the leading space after the pipe
                    sb.append(line.substring(contentStart + 1)).append("\n");
                } else {
                    sb.append(line.substring(contentStart)).append("\n");
                }
            }
        }

        final String result = sb.toString();
        // Remove the trailing newline we added if original didn't have one
        if (!content.endsWith("\n") && result.endsWith("\n")) {
            return result.substring(0, result.length() - 1);
        }
        return result;
    }
}
