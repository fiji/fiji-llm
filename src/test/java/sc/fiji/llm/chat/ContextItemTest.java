package sc.fiji.llm.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import sc.fiji.llm.script.ScriptContextItem;

/**
 * Unit tests for the Conversation class.
 */
public class ContextItemTest {
 
    @Test
    public void testContextItemToString() {
        // Given: a context item
        ContextItem item = new TestContextItem("script", "test.py", "print('hello')");

        // When: we call toString
        String formatted = item.toString();

        // Then: it should have the proper format
        assertTrue(formatted.contains("script"));
        assertTrue(formatted.contains("test.py"));
        assertTrue(formatted.contains("print('hello')"));
        assertTrue(formatted.startsWith("\n---"));
    }

    @Test
    public void testContextItemEquality() {
        // Given: two context items with the same content
        ContextItem item1 = new TestContextItem("script", "test.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test.py", "print('hello')");

        // When/Then: they should be equal
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testContextItemInequality() {
        // Given: two context items with different content
        ContextItem item1 = new TestContextItem("script", "test1.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test2.py", "print('hello')");

        // When/Then: they should not be equal
        assertNotEquals(item1, item2);
    }

    @Test
    public void testContextItemInequalityDifferentType() {
        // Given: two context items with different types
        ContextItem item1 = new TestContextItem("script", "test.py", "print('hello')");
        ContextItem item2 = new TestContextItem("doc", "test.py", "print('hello')");

        // When/Then: they should not be equal
        assertNotEquals(item1, item2);
    }

    @Test
    public void testContextItemInequalityDifferentContent() {
        // Given: two context items with different content
        ContextItem item1 = new TestContextItem("script", "test.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test.py", "print('world')");

        // When/Then: they should not be equal
        assertNotEquals(item1, item2);
    }

    @Test
    public void testScriptContextItemMergeKey() {
        // Given: a script context item
        ScriptContextItem item = new ScriptContextItem("test.py", "content", 0, 1);

        // When: we get the merge key
        String mergeKey = item.getMergeKey();

        // Then: it should be constructed from instance and tab indices
        assertEquals("script:0:1", mergeKey);
    }

    @Test
    public void testScriptContextItemMergeWithSameScript() {
        // Given: two script context items from the same script with different selections
        String content = "line1\nline2\nline3\nline4\nline5";
        ScriptContextItem item1 = new ScriptContextItem("test.py", content, 0, 1, "", 1, 2);
        ScriptContextItem item2 = new ScriptContextItem("test.py", content, 0, 1, "", 4, 5);

        // When: we merge them
        ContextItem merged = item1.mergeWith(java.util.Collections.singletonList(item2));

        // Then: the result should have both ranges
        assertNotNull(merged);
        assertTrue(merged instanceof ScriptContextItem);
        String mergedStr = merged.toString();
        assertTrue(mergedStr.contains("Selected lines:"));
        assertTrue(mergedStr.contains("1-2"));
        assertTrue(mergedStr.contains("4-5"));
    }

    @Test
    public void testScriptContextItemMergeOverlappingRanges() {
        // Given: two script context items with overlapping selections
        String content = "line1\nline2\nline3\nline4\nline5";
        ScriptContextItem item1 = new ScriptContextItem("test.py", content, 0, 1, "", 1, 3);
        ScriptContextItem item2 = new ScriptContextItem("test.py", content, 0, 1, "", 2, 4);

        // When: we merge them
        ContextItem merged = item1.mergeWith(java.util.Collections.singletonList(item2));

        // Then: overlapping ranges should be combined into one
        assertNotNull(merged);
        String mergedStr = merged.toString();
        assertTrue(mergedStr.contains("1-4"));
        // Should not have separate ranges listed
        assertFalse(mergedStr.contains("1-3"));
        assertFalse(mergedStr.contains("2-4"));
    }

    @Test
    public void testScriptContextItemMergeAdjacentRanges() {
        // Given: two script context items with adjacent selections
        String content = "line1\nline2\nline3\nline4\nline5";
        ScriptContextItem item1 = new ScriptContextItem("test.py", content, 0, 1, "", 1, 2);
        ScriptContextItem item2 = new ScriptContextItem("test.py", content, 0, 1, "", 3, 4);

        // When: we merge them
        ContextItem merged = item1.mergeWith(java.util.Collections.singletonList(item2));

        // Then: adjacent ranges should be combined
        assertNotNull(merged);
        String mergedStr = merged.toString();
        assertTrue(mergedStr.contains("1-4"));
    }

    /**
     * Simple test helper implementation of ContextItem for unit tests.
     */
    private static class TestContextItem extends AbstractContextItem {
        private final String content;

        public TestContextItem(String type, String label, String content) {
            super(type, label);
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("\n--- ").append(getType()).append(": ").append(getLabel()).append(" ---\n");
            sb.append(content).append("\n");
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            final TestContextItem other = (TestContextItem) obj;
            return java.util.Objects.equals(getType(), other.getType()) &&
                   java.util.Objects.equals(getLabel(), other.getLabel()) &&
                   java.util.Objects.equals(content, other.content);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(getType(), getLabel(), content);
        }
    }
}
