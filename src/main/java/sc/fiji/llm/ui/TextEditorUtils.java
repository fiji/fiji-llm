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
     * Return the most-recent visible TextEditor instance, or null if none.
     * Iterates {@code TextEditor.instances} from the end because instances are
     * appended when created and the newest instance is at the last index.
     */
    public static TextEditor getMostRecentVisibleEditor() {
        final List<TextEditor> instances = TextEditor.instances;
        if (instances == null || instances.isEmpty()) return null;
        for (int i = instances.size() - 1; i >= 0; i--) {
            final TextEditor cand = instances.get(i);
            if (cand != null && cand.isVisible()) return cand;
        }
        return null;
    }
}
