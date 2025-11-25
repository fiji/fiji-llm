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

import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.TextEditorTab;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ScriptContextUtilities {

	public static JsonElement getTabJson(TextEditorTab tab, ScriptID scriptID) {
		JsonObject tabJson = new JsonObject();
		tabJson.addProperty(ScriptContextItem.NAME_KEY, ((EditorPane)tab.getEditorPane()).getFile().getName());
		tabJson.addProperty(ScriptContextItem.SCRIPT_ID_KEY, scriptID.toString());
		return tabJson;
	}

	public static JsonElement getTabJson(TextEditorTab tab, int editorIndex, int tabIndex) {
		return getTabJson(tab, new ScriptID(editorIndex, tabIndex));
	}

	
}
