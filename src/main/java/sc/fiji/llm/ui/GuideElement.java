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
