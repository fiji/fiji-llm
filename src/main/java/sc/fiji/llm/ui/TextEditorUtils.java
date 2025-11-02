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
}
