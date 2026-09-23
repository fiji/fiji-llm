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
import sc.fiji.llm.guidance.workflows.WritingScriptsGuide;

/** Guidance for Groovy scripting in Fiji. */
@Plugin(type = AgentGuide.class)
public class ScriptingGroovyGuide extends AbstractAgentGuide {

	public static final String ID = "scripting-groovy";

	private static final String CONTENT = """
			# Groovy Scripting in Fiji

			Groovy runs on the JVM and is close to Java, but adds dynamic typing, closures, collection helpers,
			string interpolation, and concise object syntax. Fiji's Groovy integration is a JSR-223 engine exposed
			through the SciJava `ScriptLanguage` plugin. Any library on Fiji's runtime classpath is available to the
			script, including ImageJ 1.x, ImageJ2, SciJava, ImgLib2, and installed plugin classes.

			## Imports and useful classes

			Use ordinary Groovy/Java imports. Fiji classes are available because they are on the classpath; they are
			not generally in scope without an import. Prefer specific imports over wildcard imports so the API is
			clear and name collisions are visible.

			```groovy
			import ij.IJ
			import ij.ImagePlus
			import ij.WindowManager
			import ij.measure.ResultsTable
			import net.imagej.Dataset
			import net.imagej.ops.OpService
			import org.scijava.command.CommandService
			import org.scijava.log.LogService
			import org.scijava.ui.UIService
			```

			Common API areas include `ij.*` for ImageJ 1.x images, commands, windows, dialogs, ROIs, and results;
			`net.imagej.*` for ImageJ2 data and services; `org.scijava.*` for commands, logging, UI, and context;
			and `net.imglib2.*` for typed n-dimensional image data. Use the actual Java API types expected by a
			method. For example, an ImageJ 1.x `ImagePlus` is not interchangeable with an ImageJ2 `Dataset`.

			The adapter remembers imports between evaluations in the Script Editor. Saved scripts should still declare
			all of their imports explicitly and must not depend on an earlier editor evaluation.

			## Groovy idioms useful in Fiji

			- `def` declares a dynamically typed variable or method; use explicit Java types when overload selection,
			  pixel types, or plugin APIs make the contract important.
			- Parentheses and semicolons are often optional, but keep parentheses on Java/Fiji calls. In particular,
			  `2 + 2` and string concatenation can be surprising without parentheses.
			- Closures use `{ value -> ... }` and work well with `each`, `collect`, `findAll`, and `eachWithIndex`.
			  A closure is a Groovy object, not a Java `Function`, so convert or type it when an API requires a
			  specific Java interface.
			- Lists and maps use literals such as `[1, 2, 3]` and `[title: 'sample']`. Use `as String[]` or an
			  explicit cast when a Java API requires an array or another exact type.
			- Double-quoted strings interpolate expressions: `"value=${value}"`; single-quoted strings do not.
			  Call `.toString()` when a strict Java `String` is required instead of a `GString`.
			- `object.property` usually maps to Java bean getters and setters, `object?.property` avoids a null
			  dereference, and `value ?: fallback` is the Elvis defaulting operator. Groovy `==` uses equality
			  (`equals`), not Java reference identity.
			- `print` and `println` write to the script/standalone console. Use `IJ.log(...)` for the ImageJ Log
			  window and `LogService` when working with ImageJ2/SciJava services.

			A small Fiji-oriented example:

			```groovy
			import ij.IJ
			import ij.WindowManager

			def ids = WindowManager.getIDList() ?: []
			ids.each { id ->
			    def image = WindowManager.getImage(id)
			    IJ.log("${image?.title ?: 'untitled'}")
			}
			```
			""";

	public ScriptingGroovyGuide() {
		super(ID, "Groovy Scripting", AgentGuide.topics(Topic.WORKFLOWS, Topic.SCRIPTS,
			Topic.GROOVY), Authority.PROJECT_AUTHORED, List.of(
				WritingScriptsGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
