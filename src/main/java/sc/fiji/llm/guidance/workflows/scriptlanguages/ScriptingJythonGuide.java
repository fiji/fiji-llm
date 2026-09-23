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
package sc.fiji.llm.guidance.workflows.scriptlanguages;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;
import sc.fiji.llm.guidance.workflows.ScriptingGuide;

/** Guidance for Jython scripting in Fiji. */
@Plugin(type = AgentGuide.class)
public class ScriptingJythonGuide extends AbstractAgentGuide {

	public static final String ID = "scripting-jython";

	private static final String CONTENT = """
			# Jython Scripting in Fiji

			Jython is Python 2.7 running in the JVM. It can call Java classes on Fiji's classpath directly, including
			ImageJ 1.x, ImageJ2, SciJava, ImgLib2, and installed plugin classes. The Python standard library is
			available, but Python 3 syntax and native-code packages such as `numpy`, `scipy`, and `scikit-image`
			are not provided by Jython.

			## Imports and useful classes

			Use Python import syntax for Java packages. Import only the classes needed by the script so the Java API
			being used is clear.

			```python
			from ij import IJ, ImagePlus, WindowManager
			from ij.measure import ResultsTable
			from ij.plugin.frame import RoiManager
			from ij.process import FloatProcessor
			from net.imagej import Dataset
			from net.imagej.ops import OpService
			from org.scijava.command import CommandService
			from org.scijava.log import LogService
			from org.scijava.ui import UIService
			```

			Common API areas include `ij` for ImageJ 1.x images, commands, windows, dialogs, ROIs, and results;
			`net.imagej` for ImageJ2 data and services; `org.scijava` for commands, logging, UI, and context; and
			`net.imglib2` for typed n-dimensional image data. Use the Java type expected by each method: an
			ImageJ 1.x `ImagePlus` is not interchangeable with an ImageJ2 `Dataset`.

			## Jython idioms for Fiji APIs

			- Call Java static methods, constructors, and instance methods with ordinary Python call syntax:
			  `imp = IJ.createImage('sample', 512, 512, 1, 8)` and `imp.show()`.
			- Java bean getters and setters can often be accessed as properties: `imp.title` reads `getTitle()` and
			  `imp.changes = False` calls the corresponding setter. Use explicit methods when overloads or API
			  clarity matter.
			- Java collections and arrays are usually iterable from Jython. Python lists and tuples are convenient
			  inputs, but convert values explicitly when a Java API requires a particular array or numeric type.
			- Java overload resolution can be sensitive to Python values. Use `int(value)`, `float(value)`, or
			  `str(value)` when passing numeric values or command option strings, for example `'value=' + str(count)`.
			- Use Python 2 syntax: no f-strings, and `/` performs integer division unless the script uses
			  `from __future__ import division`. Use `%` formatting or `.format()` for strings.
			- List comprehensions, generator expressions, `enumerate`, `zip`, `lambda`, and `*args`/`**kwargs` work
			  well for adapting image and table data, but keep Java object types in mind when passing results back
			  to Fiji APIs.
			- `print` writes to the script or standalone console. Use `IJ.log(str(value))` for the ImageJ Log window;
			  `LogService` is the appropriate logger when using ImageJ2/SciJava services.

			A small Fiji-oriented example:

			```python
			from ij import IJ, WindowManager

			for image_id in WindowManager.getIDList() or []:
			    image = WindowManager.getImage(image_id)
			    IJ.log(image.getTitle() if image is not None else 'untitled')
			```
			""".strip();

	public ScriptingJythonGuide() {
		super(ID, "Jython Scripting", AgentGuide.topics(Topic.WORKFLOWS, Topic.SCRIPTS,
			Topic.PYTHON), Authority.PROJECT_AUTHORED, List.of(
				ScriptingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
