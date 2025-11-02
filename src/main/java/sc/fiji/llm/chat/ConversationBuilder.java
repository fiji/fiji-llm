package sc.fiji.llm.chat;

import java.util.ArrayList;
import java.util.List;

import sc.fiji.llm.tools.AiToolPlugin;

/**
 * Builder for creating conversations with a combined system message
 * that includes both the base system prompt and available tool documentation.
 * <p>
 * This builder allows you to configure both the base system message and the
 * available tools, then combines them into a single formatted system message
 * when {@link #build()} is called.
 * </p>
 */
public class ConversationBuilder {
	private String baseSystemMessage;
	private final List<AiToolPlugin> tools;

	/**
	 * Creates a new ConversationBuilder with no initial configuration.
	 */
	public ConversationBuilder() {
		this.tools = new ArrayList<>();
	}

	/**
	 * Sets the base system message for the conversation.
	 *
	 * @param baseSystemMessage the base system prompt
	 * @return this builder for method chaining
	 */
	public ConversationBuilder withBaseSystemMessage(final String baseSystemMessage) {
		this.baseSystemMessage = baseSystemMessage;
		return this;
	}

	/**
	 * Adds tools to the conversation. The descriptions of these tools will be
	 * included in the system message under an "Available Tools" section.
	 *
	 * @param tools the tools to add
	 * @return this builder for method chaining
	 */
	public ConversationBuilder withTools(final List<AiToolPlugin> tools) {
		if (tools != null) {
			this.tools.addAll(tools);
		}
		return this;
	}

	/**
	 * Adds a single tool to the conversation.
	 *
	 * @param tool the tool to add
	 * @return this builder for method chaining
	 */
	public ConversationBuilder withTool(final AiToolPlugin tool) {
		if (tool != null) {
			this.tools.add(tool);
		}
		return this;
	}

	/**
	 * Builds and returns a new Conversation with the combined system message.
	 * The system message will contain the base system message followed by a
	 * formatted list of available tools (if any were added).
	 *
	 * @return a new Conversation with the combined system message
	 */
	public Conversation build() {
		final String combinedSystemMessage = buildSystemMessage();
		return new Conversation(combinedSystemMessage);
	}

	/**
	 * Builds the combined system message from the base message and tool descriptions.
	 * <p>
	 * Format:
	 * <pre>
	 * [Base system message]
	 *
	 * ## Available Tools
	 *
	 * - **Tool Name**: Tool description
	 * - **Tool Name 2**: Tool description 2
	 * </pre>
	 * </p>
	 *
	 * @return the formatted combined system message
	 */
	private String buildSystemMessage() {
		final StringBuilder sb = new StringBuilder();

		// Add base system message
		if (baseSystemMessage != null && !baseSystemMessage.isEmpty()) {
			sb.append(baseSystemMessage);
		}

		// Add tools section if tools are available
		if (!tools.isEmpty()) {
			if (sb.length() > 0) {
				sb.append("\n\n");
			}
			sb.append("## Available Tools\n\n");

			for (final AiToolPlugin tool : tools) {
				sb.append("- **").append(tool.getName()).append("**: ");
				final String description = tool.getUsage();
				if (description != null && !description.isEmpty()) {
					sb.append(description);
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}
}
