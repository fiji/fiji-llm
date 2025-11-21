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
package sc.fiji.llm.macro;

import java.awt.Frame;

import org.scijava.plugin.Plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sc.fiji.llm.context.AppContextSupplier;

@Plugin(type = AppContextSupplier.class)
public class AppMacroContext implements AppContextSupplier {

    @Override
    public JsonElement appConext() {
		JsonObject macroContext = new JsonObject();
		macroContext.addProperty(TYPE_KEY, "macro_context");
		boolean recorderOpen = false;
		for (Frame frame : Frame.getFrames()) {
			if (frame.toString().startsWith("ij.plugin.frame.Recorder")) {
				recorderOpen = frame.isVisible();
				break;
			}
		}
		macroContext.addProperty("recorder_is_open", recorderOpen);
		return macroContext;
    }
	
}
