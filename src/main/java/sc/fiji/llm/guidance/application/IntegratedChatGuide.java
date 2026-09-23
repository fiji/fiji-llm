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
package sc.fiji.llm.guidance.application;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;
import sc.fiji.llm.guidance.OnboardingGuide;

/** Information about the integrated chat functionality. */
@Plugin(type = AgentGuide.class)
public class IntegratedChatGuide extends AbstractAgentGuide {

	public static final String ID = "integrated-chat";

	private static final String CONTENT = """
			# Integrated Fiji Chat

			Fiji Chat brings approachable natural-language AI assistance into Fiji, helping
			scientists discover tools, learn workflows, and create reusable scripts. Unlike
			a general-purpose chatbot, it can work with relevant Fiji application context
			through attached context items and Fiji tools while remaining inside the Fiji
			workflow.
			The integrated chat and the MCP server are separate access paths: they share
			Fiji's extensible tool framework and live application state, but not conversations
			or chat memory.

			## Start the chat

			- Run **Help > Assistants > Fiji Chat...** (also available with `Ctrl+0`).
			- Choose an **AI Service** and then a **Chat Model**. The service is the provider;
			  the model is the specific model used for the conversation.
			- If the selected service needs an API key, Fiji opens the key-management step
			  before starting the chat.

			## Main commands

			- **Fiji Chat...** starts or configures a chat session.
			- **Manage API Keys...** adds, replaces, or removes credentials for providers
			  that require them. It also links to the provider's key page. Keys are masked
			  when already configured.

			## Chat window for human users

			These are the normal controls for a human using the integrated chat window.
			An agent normally interacts by receiving a request and using its available Fiji
			tools; it should not assume that it can click these controls. A UI-capable agent
			may inspect and operate the window separately when that is explicitly required.

			- Type a request in the input area and use **Send**. While the assistant is
			  responding, the same control becomes **Stop** and interrupts the response.
			- Use **Attach context** to attach the current Fiji item or choose another
			  available context item. Attached items are included with the request; remove
			  individual context tags or use **Clear context** to remove them all.
			- Use the conversation selector to reload a previous conversation. The `+`
			  button starts a new conversation with the current model, while the trash button
			  permanently deletes the selected conversation.
			- Use the provider/model button to change the service or model. Use the key
			  button to return to API-key configuration when the active service requires it.
			- Use the `?` button for the in-application tour of the chat controls, and the
			  forum button for Image.sc support.

			Depending on the selected model and provider, the assistant may use available
			Fiji tools while answering. Explicitly attached context and the current
			conversation are different: context supplies information for a request, while
			conversation history preserves the ongoing dialogue.
			""";

	public IntegratedChatGuide() {
			super(ID, "Integrated Chat", List.of("application", "ai", "llm", "agent"),
			Authority.PROJECT_AUTHORED, List.of(OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
