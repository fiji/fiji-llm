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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TextEditorUtilsTest {

	@Test
	public void testAddLineNumbers() {
		String input = "foo\nbar\nbaz";
		String expected = "1 | foo\n2 | bar\n3 | baz\n";
		String result = TextEditorUtils.addLineNumbers(input);
		assertEquals(expected, result);
	}

	@Test
	public void testStripLineNumbers() {
		String input = "1 | foo\n2 | bar\n3 | baz\n";
		String expected = "foo\nbar\nbaz\n\n";
		String result = TextEditorUtils.stripLineNumbers(input);
		assertEquals(expected, result);
	}

	@Test
	public void testStripLineNumbersWithNonMatchingFormat() {
		String input = "foo\nbar\nbaz";
		String expected = "foo\nbar\nbaz";
		String result = TextEditorUtils.stripLineNumbers(input);
		assertEquals(expected, result);
	}

	@Test
	public void testAddAndStripLineNumbersRoundTrip() {
		String input = "alpha\nbeta\ngamma";
		String withNumbers = TextEditorUtils.addLineNumbers(input);
		String stripped = TextEditorUtils.stripLineNumbers(withNumbers);
		// stripLineNumbers appends newlines, resulting in an extra newline at the
		// end
		assertEquals(input + "\n\n", stripped);
	}

	@Test
	public void testStripMarkdownCodeFences() {
		String input = "```python\nprint('hello')\nprint('world')\n```";
		String expected = "print('hello')\nprint('world')";
		String result = TextEditorUtils.stripLineNumbers(input);
		assertEquals(expected, result);
	}

	@Test
	public void testStripMarkdownCodeFencesWithLanguage() {
		String input = "```java\npublic class Test {}\n```";
		String expected = "public class Test {}";
		String result = TextEditorUtils.stripLineNumbers(input);
		assertEquals(expected, result);
	}

	@Test
	public void testStripMarkdownCodeFencesNoLanguage() {
		String input = "```\nsome code\n```";
		String expected = "some code";
		String result = TextEditorUtils.stripLineNumbers(input);
		assertEquals(expected, result);
	}

	@Test
	public void testStripLineNumbersWithNullContent() {
		String result = TextEditorUtils.stripLineNumbers(null);
		assertEquals("", result);
	}

	@Test
	public void testStripMarkdownCodeFencesNotPresent() {
		String input = "print('hello')\nprint('world')";
		String expected = "print('hello')\nprint('world')";
		String result = TextEditorUtils.stripLineNumbers(input);
		assertEquals(expected, result);
	}
}
