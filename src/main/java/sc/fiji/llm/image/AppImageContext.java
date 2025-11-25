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
package sc.fiji.llm.image;

import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sc.fiji.llm.context.AppContextSupplier;

@Plugin(type = AppContextSupplier.class)
public class AppImageContext implements AppContextSupplier {

	@Parameter
	private ImagePlusHelper iPlusHelper;

	@Override
	public JsonElement appConext()
	{
		JsonObject imageContext = new JsonObject();
		imageContext.addProperty(TYPE_KEY, "image_context");

		List<Integer> ids = iPlusHelper.getIds();
		JsonArray openImageJson = new JsonArray();
		for (Integer id : ids) {
			if (iPlusHelper.isVisible(id)) {
				JsonObject imageJson = new JsonObject();
				imageJson.addProperty("id", id);
				imageJson.addProperty("title", iPlusHelper.getTitle(id));
				openImageJson.add(imageJson);
			}
		}
		if (!openImageJson.isEmpty()) {
			imageContext.add("open_images", openImageJson);
		}

		return imageContext;
	}
	
}
