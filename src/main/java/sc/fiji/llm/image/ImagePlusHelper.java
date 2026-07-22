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
