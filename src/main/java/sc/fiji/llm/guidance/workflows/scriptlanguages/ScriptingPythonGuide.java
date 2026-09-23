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

/** Guidance for Python scripting in Fiji. */
@Plugin(type = AgentGuide.class)
public class ScriptingPythonGuide extends AbstractAgentGuide {

	public static final String ID = "scripting-python";

	private static final String CONTENT = """
			# Python Scripting in Fiji

			Fiji's Python mode runs CPython 3 inside Fiji through the SciJava `scripting-python` plugin and the
			`scyjava` Java bridge. This is different from Jython: Python 3 syntax and native Python packages can be
			used when they are installed in the configured environment. The plugin is not PyImageJ itself, although
			the default environment includes `pyimagej` and `appose` dependencies for Python-side use.

			## Configure Python mode

			Use `Edit > Options > Python...` before running Python scripts in Fiji:

			1. Set **Python environment directory** to the environment Fiji should use.
			2. Review or extend **Conda dependencies** and **Pip dependencies** when the script needs packages
			   beyond the defaults.
			3. Click **Build Python environment** and wait for environment creation to finish.
			4. Enable **Launch in Python mode**, apply the options, and restart Fiji.

			Python mode writes the selected environment and launch mode to Fiji's configuration and creates an
			`environment.yml` under the Fiji configuration directory. The Python script engine cannot run until the
			Python runner has been installed by launching Fiji in Python mode. If Python mode was just enabled, do
			not diagnose a missing runner as a script error: finish environment initialization and restart first.
			If Fiji cannot start afterward, the options dialog recommends deleting the configuration file and
			restarting so the previous launch settings can be restored.

			## The `ij` gateway

			Python mode automatically provides an `ij` ImageJ gateway. Use it as the primary entry point to Fiji and
			ImageJ2 services instead of constructing services or initializing a second ImageJ instance. Common gateway
			accessors include `ij.io()` for opening and saving data, `ij.ui()` for UI operations, `ij.op()` for Ops,
			`ij.command()` and `ij.module()` for SciJava execution, `ij.script()` for scripts, and `ij.data()` or
			`ij.dataset()` for data services. The gateway also exposes convenience properties such as `ij.IJ`,
			`ij.WindowManager`, and `ij.py` when the ImageJ legacy support and PyImageJ environment are available.

			`ij.py` provides the important Java/Python image conversions: `from_java`, `to_java`, `to_dataset`,
			`to_imageplus`, `to_img`, `to_xarray`, and `show`. Use these when moving between NumPy/xarray data and
			ImageJ, ImageJ2, or ImgLib2 data. `ij.py.from_java` and `ij.py.to_java` are not generic casts; they
			convert supported image and data objects.

			```python
			data = ij.io().open('https://media.imagej.net/pyimagej/3d/hela_a3g.tif')
			image = ij.py.from_java(data)

			ij.py.show(image)
			```

			For Java classes that are not exposed as gateway properties, use `scyjava.jimport` with the fully
			qualified class name. This includes classes from ImageJ 1.x, ImageJ2, SciJava, ImgLib2, and installed
			plugin libraries.

			```python
			from scyjava import jimport

			GenericDialog = jimport('ij.gui.GenericDialog')
			ResultsTable = jimport('ij.measure.ResultsTable')
			```

			Common API areas include `ij.*` for ImageJ 1.x images, commands, windows, dialogs, ROIs, and results;
			`net.imagej.*` for ImageJ2 data and services; `org.scijava.*` for commands, logging, UI, and context;
			and `net.imglib2.*` for typed n-dimensional image data. Use the Java type expected by each method: an
			ImageJ 1.x `ImagePlus` is not interchangeable with an ImageJ2 `Dataset`. SciJava services should
			be requested as script parameters rather than constructed manually; see Guide ID `%s`.

			## Python and Java interop

			- Java classes returned by `jimport` are proxies. Call Java methods with Python call syntax, construct
			  objects normally, and use explicit conversions such as `str(value)`, `int(value)`, and `float(value)`
			  when an overloaded Java method or command option requires a specific type.
			- Java arrays and collections can often be iterated from Python, but convert them to `list` when a Python
			  list is required, and use the Java API's expected type when passing values back.
			- Python `None` represents Java `null`. Check it before calling methods on an optional Java result.
			- Use Python 3 features such as f-strings, comprehensions, context managers, and `pathlib` when useful,
			  but remember that a Python object is not automatically a Java object accepted by every overload.
			- `print` writes to the Python/script console. Use `ij.IJ.log(str(value))` for the ImageJ Log window and
			  a `LogService` parameter for ImageJ2/SciJava logging.
			""".formatted(ScriptingGuide.ID).strip();

	public ScriptingPythonGuide() {
		super(ID, "Python Scripting", AgentGuide.topics(Topic.WORKFLOWS, Topic.SCRIPTS,
			Topic.PYTHON), Authority.PROJECT_AUTHORED, List.of(
				ScriptingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
