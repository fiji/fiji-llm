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

package sc.fiji.llm.ui;

import java.util.List;

import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import sc.fiji.llm.script.ScriptID;

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
	 * Get the active script ID from the most recently visible editor's currently selected tab.
	 *
	 * @return ScriptID of the active script, or null if no editor is visible or no tab is selected
	 */
	public static ScriptID getActiveScriptID() {
		final TextEditor textEditor = getMostRecentVisibleEditor();
		if (textEditor == null) {
			return null;
		}

		final int editorIndex = getMostRecentVisibleEditorIndex();
		if (editorIndex == -1) {
			return null;
		}

		final TextEditorTab activeTab = textEditor.getTab();
		if (activeTab == null) {
			return null;
		}

		final int tabIndex = getTabIndex(textEditor, activeTab);
		if (tabIndex == -1) {
			return null;
		}

		return new ScriptID(editorIndex, tabIndex);
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
