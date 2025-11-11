package sc.fiji.llm.script;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.scijava.Priority;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.ScriptEditor;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import sc.fiji.llm.chat.ContextItem;
import sc.fiji.llm.chat.ContextItemSupplier;
import sc.fiji.llm.ui.TextEditorUtils;

/**
 * ContextItemSupplier implementation for script context items.
 * Provides available scripts from open TextEditor instances and creates ScriptContextItem objects.
 */
@Plugin(type = ContextItemSupplier.class, priority = Priority.EXTREMELY_HIGH)
public class ScriptContextSupplier implements ContextItemSupplier {

	@Parameter
	private CommandService commandService;

	@Override
	public String getDisplayName() {
		return "Script";
	}

	@Override
	public ImageIcon getIcon() {
		final URL iconUrl = getClass().getResource("/icons/petition-noun-32.png");
		if (iconUrl != null) {
			return new ImageIcon(iconUrl);
		}
		return null;
	}

	@Override
	public Set<ContextItem> listAvailable() {
		final Set<ContextItem> items = new LinkedHashSet<>();

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

						final ScriptContextItem item = buildScriptContextItem(textEditor, tab, editorIndex, tabIndex);
						if (item != null) {
							items.add(item);
						}

						tabIndex++;
					}
				} catch (Exception e) {
					// Skip this editor if we can't access its tabs
				}
			}
		} catch (Exception e) {
			// If we can't access TextEditor.instances, return empty list
		}

		return items;
	}

	@Override
	public ContextItem createActiveContextItem() {
		try {
			TextEditor textEditor = TextEditorUtils.getMostRecentVisibleEditor();
			if (textEditor == null) {
				// No visible editor - open the Script Editor
				if (SwingUtilities.isEventDispatchThread()) {
					commandService.run(ScriptEditor.class, true);
				} else {
					SwingUtilities.invokeLater(() ->
						commandService.run(ScriptEditor.class, true)
					);

					// Poll for up to 5 seconds (50 x 100ms)
					int pollCount = 0;
					while (pollCount < 50) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							break;
						}
						textEditor = TextEditorUtils.getMostRecentVisibleEditor();
						if (textEditor != null) {
							break;
						}
						pollCount++;
					}
				}
			}
			if (textEditor == null) {
				return null;
			}

			// Get the active tab
			TextEditorTab tab;
			try {
				tab = textEditor.getTab();
			} catch (Throwable t) {
				try {
					tab = textEditor.getTab(0);
				} catch (Throwable t2) {
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
			return buildScriptContextItem(textEditor, tab, editorIndex, tabIndex);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * Builds a ScriptContextItem from a TextEditor and tab.
	 */
	private ScriptContextItem buildScriptContextItem(final TextEditor textEditor, final TextEditorTab tab,
			final int editorIndex, final int tabIndex) {
		final String scriptName = stripLeadingAsterisks(tab.getTitle());
		final EditorPane editorPane = (EditorPane) tab.getEditorPane();
		final String scriptContent = TextEditorUtils.addLineNumbers(editorPane.getText());
		final String errorOutput = getErrorOutput(textEditor);
		final int[] selectionLines = getSelectionLineNumbers(editorPane);

		return new ScriptContextItem(scriptName, scriptContent, editorIndex, tabIndex, errorOutput,
			selectionLines[0], selectionLines[1]);
	}

	/**
	 * Strips leading asterisks from a script name (asterisks indicate unsaved changes).
	 */
	private String stripLeadingAsterisks(final String scriptName) {
		if (scriptName == null) {
			return null;
		}
		return scriptName.replaceAll("^\\*+", "");
	}

	/**
	 * Finds the tab index of a given tab within a TextEditor.
	 */
	private int findTabIndex(final TextEditor textEditor, final TextEditorTab targetTab) {
		for (int i = 0; ; i++) {
			try {
				final TextEditorTab currentTab = textEditor.getTab(i);
				if (currentTab == null) {
					break;
				}
				if (currentTab == targetTab) {
					return i;
				}
			} catch (Exception e) {
				break;
			}
		}
		return -1;
	}

	/**
	 * Gets error output from a TextEditor.
	 */
	private String getErrorOutput(final TextEditor textEditor) {
		try {
			final javax.swing.JTextArea errorScreen = textEditor.getErrorScreen();
			if (errorScreen != null) {
				final String text = errorScreen.getText();
				return text != null ? text.trim() : "";
			}
		} catch (Exception e) {
			// If we can't access error output, just return empty string
		}
		return "";
	}

	/**
	 * Extracts selection start and end line numbers from an EditorPane.
	 */
	private int[] getSelectionLineNumbers(final EditorPane editorPane) {
		int selectionStartLine = ScriptAddress.UNSET;
		int selectionEndLine = ScriptAddress.UNSET;

		try {
			final String selectedText = editorPane.getSelectedText();

			if (selectedText != null && !selectedText.isEmpty()) {
				final int selectionStart = editorPane.getSelectionStart();
				final int selectionEnd = editorPane.getSelectionEnd();
				selectionStartLine = editorPane.getLineOfOffset(selectionStart) + 1;
				selectionEndLine = editorPane.getLineOfOffset(selectionEnd) + 1;
			}
		} catch (Exception e) {
			// If we can't get selection info, just use UNSET
		}

		return new int[]{selectionStartLine, selectionEndLine};
	}
}
