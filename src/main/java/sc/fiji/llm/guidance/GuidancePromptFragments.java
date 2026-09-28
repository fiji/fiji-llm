/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
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
package sc.fiji.llm.guidance;

/** Shared instruction fragments used by integrated chat and MCP prompts. */
public final class GuidancePromptFragments {

	public static final String GUIDE_USAGE = """
Fiji-specific guidance is available through `fiji_guide_read`. Use it when a guide is
recommended or when you need Fiji-specific workflow details. Read only the most
relevant guide rather than loading the whole catalog.

""";

	public static final String DECISION_POINT_GUIDES = """
Decision-point guides:
When a user's request matches a condition below, read the guide with `fiji_guide_read`
before choosing tools or writing code.

| condition | guide_id |
| --- | --- |
| Choosing between a Fiji script and an ImageJ macro | `scripts-and-macros` |
| Writing, running, or diagnosing a Fiji script | `scripting` |
| Recording, editing, or running an ImageJ macro (`.ijm`) | `creating-macros` |
| Choosing or running a Fiji command | `running-commands` |

""";

	public static final String ERROR_RECOVERY = """
When a script produces error logs or a tool reports an exception, treat the output as
diagnostic evidence rather than an endpoint. Check the result for `recommended_tools`
and `guide_recommendations`, and follow those recommendations before retrying or
changing the workflow.

""";

	private GuidancePromptFragments() {}
}
