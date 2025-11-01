package sc.fiji.llm.chat;

/**
 * Represents a script context item that can be added to the chat.
 */
public class ScriptContextItem extends ContextItem {
    private final String scriptName;
    private final String language;

    public ScriptContextItem(String scriptName, String content) {
        this(scriptName, content, null);
    }

    public ScriptContextItem(String scriptName, String content, String language) {
        super("Script", "📜 " + scriptName, content);
        this.scriptName = scriptName;
        this.language = language;
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getLanguage() {
        return language;
    }
}
