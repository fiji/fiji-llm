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
        // stripLineNumbers appends newlines, resulting in an extra newline at the end
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
