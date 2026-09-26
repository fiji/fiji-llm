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

package sc.fiji.llm.ui;

import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.util.List;

import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import sc.fiji.llm.log.TextLogs;
import sc.fiji.llm.script.ScriptID;

/**
 * Small helper utilities for working with the SciJava TextEditor instances.
 */
public final class TextEditorUtils {

	private static volatile FocusedScript lastFocusedScript;

	static {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addPropertyChangeListener("focusedWindow",
				TextEditorUtils::handleFocusedWindowChange);
	}

	private TextEditorUtils() {
		// utility
	}

	private static void handleFocusedWindowChange(final PropertyChangeEvent event) {
		final Object oldValue = event.getOldValue();
		if (oldValue instanceof TextEditor) {
			rememberFocusedEditor((TextEditor) oldValue);
		}

		final Object newValue = event.getNewValue();
		if (newValue instanceof TextEditor) {
			rememberFocusedEditor((TextEditor) newValue);
		}
	}

	private static void rememberFocusedEditor(final TextEditor textEditor) {
		if (textEditor == null || !textEditor.isVisible()) return;

		final TextEditorTab tab;
		try {
			tab = textEditor.getTab();
		}
		catch (final RuntimeException e) {
			return;
		}
		if (tab != null) lastFocusedScript = new FocusedScript(textEditor, tab);
	}

	/**
	 * Record an editor as the last focused editor after a programmatic activation.
	 */
	public static void recordLastFocusedEditor(final TextEditor textEditor) {
		rememberFocusedEditor(textEditor);
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
	 * Return the index of the visible TextEditor instance that currently has
	 * focus, or -1 if no visible editor has focus.
	 */
	public static int getFocusedVisibleEditorIndex() {
		final List<TextEditor> instances = TextEditor.instances;
		if (instances == null || instances.isEmpty()) return -1;
		for (int i = 0; i < instances.size(); i++) {
			final TextEditor editor = instances.get(i);
			if (editor != null && editor.isVisible() && editor.isFocused()) return i;
		}
		return -1;
	}

	/**
	 * Return the visible TextEditor instance that currently has focus, or null
	 * if no visible editor has focus.
	 */
	public static TextEditor getFocusedVisibleEditor() {
		final int editorIndex = getFocusedVisibleEditorIndex();
		return (editorIndex == -1) ? null : TextEditor.instances.get(editorIndex);
	}

	/**
	 * Get the active script ID from the currently focused editor, or the last
	 * focused visible editor if focus has moved elsewhere.
	 *
	 * @return ScriptID of the active script, or null if no script is available
	 */
	public static ScriptID getActiveScriptID() {
		final TextEditor textEditor = getFocusedVisibleEditor();
		if (textEditor == null) {
			return getLastFocusedScriptID();
		}

		final int editorIndex = getFocusedVisibleEditorIndex();
		if (editorIndex == -1) {
			return getLastFocusedScriptID();
		}

		final TextEditorTab activeTab = textEditor.getTab();
		if (activeTab == null) {
			return getLastFocusedScriptID();
		}

		final int tabIndex = getTabIndex(textEditor, activeTab);
		if (tabIndex == -1) {
			return getLastFocusedScriptID();
		}

		lastFocusedScript = new FocusedScript(textEditor, activeTab);
		return new ScriptID(editorIndex, tabIndex);
	}

	/**
	 * Return the editor containing the active script, including when focus has
	 * moved away from all Script Editor windows.
	 */
	public static TextEditor getLastFocusedVisibleEditor() {
		final ScriptID scriptID = getActiveScriptID();
		if (scriptID == null || TextEditor.instances == null || scriptID.editorIndex < 0 ||
			scriptID.editorIndex >= TextEditor.instances.size()) return null;
		return TextEditor.instances.get(scriptID.editorIndex);
	}

	private static ScriptID getLastFocusedScriptID() {
		final FocusedScript focusedScript = lastFocusedScript;
		if (focusedScript == null || focusedScript.editor == null ||
			!focusedScript.editor.isVisible()) return null;

		final int editorIndex = TextEditor.instances.indexOf(focusedScript.editor);
		final int tabIndex = getTabIndex(focusedScript.editor, focusedScript.tab);
		if (editorIndex == -1 || tabIndex == -1) return null;
		return new ScriptID(editorIndex, tabIndex);
	}

	private static final class FocusedScript {

		private final TextEditor editor;
		private final TextEditorTab tab;

		FocusedScript(final TextEditor editor, final TextEditorTab tab) {
			this.editor = editor;
			this.tab = tab;
		}
	}

	/**
	 * Find the index of a specific tab within a TextEditor.
	 *
	 * @param textEditor the TextEditor to search
	 * @param targetTab the tab to find
	 * @return the index of the tab, or -1 if not found
	 */
	public static int getTabIndex(final TextEditor textEditor, final TextEditorTab targetTab) {
		if (textEditor == null || targetTab == null) {
			return -1;
		}

		for (int i = 0;; i++) {
			try {
				final TextEditorTab currentTab = textEditor.getTab(i);
				if (currentTab == null) {
					break;
				}
				if (currentTab == targetTab) {
					return i;
				}
			}
			catch (Exception e) {
				break;
			}
		}
		return -1;
	}

	/**
	 * Read the persistent output and error logs for a script editor tab. This
	 * method reads Swing components and should be called on the EDT.
	 */
	public static TextLogs getLogs(final TextEditor textEditor,
		final TextEditorTab tab)
	{
		final String output = tab.getScreenInstance().getText();
		final String errors = textEditor.getErrorScreen().getText();
		return new TextLogs(output, errors);
	}
}
