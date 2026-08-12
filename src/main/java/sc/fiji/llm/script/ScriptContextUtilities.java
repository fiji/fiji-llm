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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.scijava.script.ScriptLanguage;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import sc.fiji.llm.ui.TextEditorUtils;

public final class ScriptContextUtilities {
	private ScriptContextUtilities() {
		// Utility class
	}

	public static ScriptContextItem getActiveScriptContext() {
		try {
			TextEditor textEditor = TextEditorUtils.getMostRecentVisibleEditor();
			if (textEditor == null) {
				return null;
			}

			// Get the active tab
			TextEditorTab tab;
			try {
				tab = textEditor.getTab();
			}
			catch (Throwable t) {
				try {
					tab = textEditor.getTab(0);
				}
				catch (Throwable t2) {
					return null;
				}
			}

			if (tab == null) {
				return null;
			}

			final int tabIndex = findTabIndex(textEditor, tab);
			if (tabIndex < 0) {
				return null;
			}

			final int editorIndex = TextEditor.instances.indexOf(textEditor);
			return buildScriptContextItem(editorIndex, tabIndex);
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	public static Set<ScriptContextItem> getAvailableScripts() {
		final Set<ScriptContextItem> items = new LinkedHashSet<>();

		try {
			final List<TextEditor> instances = TextEditor.instances;

			if (instances == null || instances.isEmpty()) {
				return items;
			}
			// Add individual scripts from all open editors
			for (final TextEditor textEditor : instances) {
				final int editorIndex = TextEditor.instances.indexOf(textEditor);
				int tabIndex = 0;

				try {
					while (true) {
						final TextEditorTab tab = textEditor.getTab(tabIndex);
						if (tab == null) {
							break;
						}

						final ScriptContextItem item = buildScriptContextItem(editorIndex, tabIndex);
						if (item != null) {
							items.add(item);
						}

						tabIndex++;
					}
				}
				catch (Exception e) {
					// Skip this editor if we can't access its tabs
				}
			}
		}
		catch (Exception e) {
			// If we can't access TextEditor.instances, return empty list
		}

		return items;
	}

	/**
	 * Builds a ScriptContextItem from a TextEditor and tab.
	 */
	public static ScriptContextItem buildScriptContextItem(final int editorIndex, final int tabIndex)
	{
		final TextEditor textEditor = TextEditor.instances.get(editorIndex);
		final TextEditorTab tab = textEditor.getTab(tabIndex);

		final String scriptName = getSanitizedTabName(tab);
		final EditorPane editorPane = (EditorPane) tab.getEditorPane();
		final String scriptContent = editorPane.getText();
		final String errorOutput = getErrorOutput(textEditor);
		final int[] selectionLines = getSelectionLineNumbers(editorPane);
		final ScriptLanguage currentLanguage = editorPane.getCurrentLanguage();
		final String scriptLanguage = currentLanguage == null ? null : currentLanguage.getNames().get(0);

		return new ScriptContextItem(scriptName, scriptContent, editorIndex,
			tabIndex, selectionLines[0], selectionLines[1], scriptLanguage, errorOutput);
	}

	/**
	 * Strips leading asterisks from a script name (asterisks indicate unsaved
	 * changes).
	 */
	private static String getSanitizedTabName(final TextEditorTab tab) {
		String title = tab.getTitle();
		if (title == null) return null;

		// Remove leading asterisk if present (indicates file has been edited)
		if (title.startsWith("*")) {
			title = title.substring(1);
		}
		// Remove trailing " (Running)" if present
		if (title.endsWith(" (Running)")) {
			title = title.substring(0, title.length() - " (Running)".length());
		}

		return title;
	}

	/**
	 * Finds the tab index of a given tab within a TextEditor.
	 */
	private static int findTabIndex(final TextEditor textEditor,
		final TextEditorTab targetTab)
	{
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
	 * Gets error output from a TextEditor.
	 */
	private static String getErrorOutput(final TextEditor textEditor) {
		try {
			final javax.swing.JTextArea errorScreen = textEditor.getErrorScreen();
			if (errorScreen != null) {
				final String text = errorScreen.getText();
				return text != null ? text.trim() : "";
			}
		}
		catch (Exception e) {
			// If we can't access error output, just return empty string
		}
		return "";
	}

	/**
	 * Extracts selection start and end line numbers from an EditorPane.
	 */
	private static int[] getSelectionLineNumbers(final EditorPane editorPane) {
		int selectionStartLine = LineRange.UNSET;
		int selectionEndLine = LineRange.UNSET;

		try {
			final String selectedText = editorPane.getSelectedText();

			if (selectedText != null && !selectedText.isEmpty()) {
				final int selectionStart = editorPane.getSelectionStart();
				final int selectionEnd = editorPane.getSelectionEnd();
				selectionStartLine = editorPane.getLineOfOffset(selectionStart) + 1;
				selectionEndLine = editorPane.getLineOfOffset(selectionEnd) + 1;
			}
		}
		catch (Exception e) {
			// If we can't get selection info, just use UNSET
		}

		return new int[] { selectionStartLine, selectionEndLine };
	}
}
