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

import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sc.fiji.llm.context.AppContextSupplier;

@Plugin(type = AppContextSupplier.class)
public class AppScriptContext implements AppContextSupplier {
    public static final String EDITOR_KEY = "editors";
    public static final String TAB_KEY = "tabs";
    public static final String EDITOR_ID = "editor_id";

    @Override
    public JsonElement appConext() {
		JsonObject scriptContext = new JsonObject();
		scriptContext.addProperty(TYPE_KEY, "script_editor_context");
		JsonArray editors = new JsonArray();
		for (int i=0; i<TextEditor.instances.size(); i++) {
			TextEditor textEditor = TextEditor.instances.get(i);
			if (textEditor.isVisible()) {
				JsonObject editorJson = new JsonObject();
				editorJson.addProperty(EDITOR_ID, i);

				List<TextEditorTab> editorTabs = new ArrayList<>();
				// Find all the tabs
				int tabIndex = 0;
				try {
					while (true) {
						editorTabs.add(textEditor.getTab(tabIndex));
						tabIndex++;
					}
				} catch (IndexOutOfBoundsException e) {
					// "This is fine"
				}

				JsonArray tabJson = new JsonArray();
				for (int j=0; j<editorTabs.size(); j++) {
					JsonElement tab = ScriptContextUtilities.getTabJson(editorTabs.get(j), i, j);
					tabJson.add(tab);
				}
				editorJson.add(TAB_KEY, tabJson);
				editors.add(editorJson);
			}
		}

		if (!editors.isEmpty()) {
			scriptContext.add(EDITOR_KEY, editors);
		}
		return scriptContext;
    }
	
}
