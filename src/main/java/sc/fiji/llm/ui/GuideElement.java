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

package sc.fiji.llm.ui;

import java.awt.Component;

/**
 * Represents a single element in the interactive guide tour.
 */
public class GuideElement {

	private final Component component;
	private final String title;
	private final String description;

	public GuideElement(Component component, String title, String description) {
		this.component = component;
		this.title = title;
		this.description = description;
	}

	public Component getComponent() {
		return component;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Get the component's approximate position for sorting. Returns a comparable
	 * value based on reading order (top-to-bottom, left-to-right).
	 */
	public int getPositionKey() {
		int y = component.getY();
		int x = component.getX();
		// Create a key that sorts by Y first (top-to-bottom), then by X
		// (left-to-right)
		// Divide into horizontal zones to handle left-to-right ordering within
		// similar Y positions
		return (y / 50) * 1000 + (x / 100);
	}
}
