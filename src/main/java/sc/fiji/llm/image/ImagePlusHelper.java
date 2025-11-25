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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import net.imagej.ImageJService;
import net.imagej.display.ImageDisplay;
import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

@Plugin(type = Service.class)
public final class ImagePlusHelper extends AbstractService implements ImageJService {

	@Parameter
	private LegacyService legacyService;

	public List<Integer> getIds() {
		List<Integer> ids = new ArrayList<>();
		helper().ifPresent(helper -> {
			try {
				Arrays.stream(helper.getIDList()).forEach(ids::add);
			} catch (Exception e) {
				// Presumably, no images open
			}

		});
		return Collections.unmodifiableList(ids);
	}

	public boolean isVisible(int id) {
		boolean[] visibility = {false};
		helper().ifPresent(helper -> {
			try {
				visibility[0] = helper.getImage(id).isVisible();
			} catch (Exception e)  {
			}
		});

		return visibility[0];
	}

	public String getTitle(int id) {
		String[] title = {""};
		helper().ifPresent(helper -> {
			try {
				title[0] = helper.getImage(id).getTitle();
			} catch (Exception e)  {
			}
		});

		return title[0];
	}

	public int getId(ImageDisplay display) {
		if (legacyService.isActive()) {
			return legacyService.getImageMap().lookupImagePlus(display).getID();
		}
		return -1;
	}


	private Optional<IJ1Helper> helper() {
		if (legacyService.isActive()) {
			return Optional.of(legacyService.getIJ1Helper());
		}
		return Optional.empty();
	}
}
