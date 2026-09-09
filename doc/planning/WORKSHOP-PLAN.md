# Fiji LLM User-focused Workshop

## Minimum successful journey
> A participant opens an image, asks Fiji Chat to help them understand it, identifies an analysis goal, discovers a suitable workflow, performs a simple measurement, and then turns the successful interaction into a reproducible macro or script.

## Workshop Description
The Fiji Chatbot is designed to provide users with an interface for using Large Language Models
(LLMs) in a context aware of your actual installation, with an initial focus on common image
analysis and scripting tasks. In this workshop we will discuss expectations and best practices for
LLM use in image analysis, work through carefully guided tasks together with the chatbot,
discuss your particular needs and use cases, and see how they align with the capabilities of the
chatbot at that time. Bring your questions, your data (if you wish), and consider: how do you
want this tool to work for you?

## Target audience
ImageJ/Fiji users without a strong technical background. No prior experience with LLMs,
programming, or scripting is required.

## Participation style
This will be a guided, interactive tutorial. The goal is to create a low-stress environment where
we can freely interact, ask questions, and make mistakes together. This is new, complex, rapidly
evolving software, in a new workshop format; come with the expectation of supporting and
learning from each other. This is an opportunity to improve your personal experience with
ImageJ, but also to provide valuable information about how people like you want to interact with
the chatbot, while assessing its strengths and weaknesses. Expect awkward icebreakers, and
be ready to fill out an evaluation form!

## Duration
2 hours

## Size
10-20 (prefer smaller)

# Workshop Outline

## 1. Welcome and expectations

**Purpose:** Establish that the assistant is a collaborator and interface, not an image-analysis authority.

Topics:
- What Fiji Chat can currently see and do
- Difference between asking a general LLM and using one connected to Fiji
- LLM output can be plausible but wrong
- The user remains responsible for interpreting images and validating results
- Avoid entering sensitive or unpublished information into external services
- Different models may behave differently
- How to interrupt, correct, or narrow a response

A useful framing might be:
> Today we are testing whether an assistant can help you navigate Fiji, make informed choices, and produce a workflow you can inspect and reuse. We are not testing whether an LLM can replace image-analysis expertise.
## 2. Meet Fiji through the assistant
Use the staples of the introductory decks:
- What is currently open?
- What kind of image is this?
- What are its dimensions, channels, slices, frames, bit depth, and calibration?
- What commands or tools are relevant?
- Where can I find a particular operation?

This is the “LLM-powered guide through Fiji” portion in its purest form.

It also gives participants immediate experience with the distinction between:
- information supplied explicitly in their prompt;
- context attached through the UI;
- state discovered through tools;
- general model knowledge.

## 3. Understand the data before analyzing it
Adapt the strongest conceptual sequence from the hands-on deck:
- pixels are values;
- visual appearance is not equivalent to quantitative content;
- bit depth matters;
- dimensions and calibration matter;
- image artifacts and acquisition quality matter;
- the biological question determines the analysis.

The chatbot should help inspect and explain these properties, but it should **not infer biological meaning from an image without evidence**.

A good shared exercise:
> “Before we process this image, help me understand what information Fiji has about it and what I should verify.”

This tests whether the assistant gives a disciplined inspection plan rather than immediately proposing segmentation.

## 4. From question to workflow
Introduce a simple analysis question, such as:
> “I want to count bright, approximately separated objects and measure their areas.”

The assistant should:
1. ask relevant clarifying questions;
2. inspect the active image;
3. suggest a simple workflow;
4. explain what each step changes;
5. identify parameters requiring user judgment;
6. guide the participant through previewing or executing steps;
7. help inspect the results.

A likely workflow:
- duplicate or preserve the original;
- inspect histogram;
- apply or choose a threshold;
- create a mask;
- analyze particles;
- inspect ROIs and measurements.

This maps directly onto the existing sequence of thresholding, Analyze Particles, and measurement.

## 5. Turn interaction into reproducible work
This should be the payoff, not an optional advanced topic.

Participants ask:
> “Turn the analysis we just performed into an ImageJ script that I can inspect and reuse.”

Then:
- open the result in the Script Editor;
- compare it with Macro Recorder output;
- identify hard-coded values;
- add comments;
- expose meaningful parameters;
- optionally adapt it for batch processing.

This closely reflects Fiji-LLM’s stated emphasis on generating familiar scripts and macros rather than inventing a new workflow representation. It also preserves the existing workshops’ message that scripting supports documentation, automation, sharing, and reproducibility.
## 6. Structured exploration and feedback
Rather than opening the floor to arbitrary datasets immediately, offer a few bounded paths:
- **Explain:** Help me understand an unfamiliar image or Fiji concept.
- **Discover:** Help me find an installed command or plugin for a goal.
- **Automate:** Help me turn manual operations into a reusable macro.
- **Debug:** Help me understand why this script or workflow failed.
- **Adapt:** Help me change an existing script for a related task.

Participants can try one path individually or discuss what they would want the assistant to do with their own work.

## 7. Closing evaluation
Ask questions that generate actionable development information:
- At what point did you trust the assistant most?
- At what point did you trust it least?
- Did you understand what information the assistant could access?
- Did it ask useful questions before acting?
- Were its explanations too long, too short, or appropriately detailed?
- Could you tell when it was suggesting versus executing?
- What did you expect it to do that it could not?
- Would you reuse the resulting macro without modification?
- What would have prevented you from completing the exercise?

# Development capability targets

Capability rows below were checked against the workshop outline above (they should map to something a participant actually does in sections 1-7), and coverage was re-verified against the current source in September 2026. "Draft: issue N" refers to [DEVELOPMENT-CAPABILITY-ISSUES.md](DEVELOPMENT-CAPABILITY-ISSUES.md), which is scoped to workshop-specific gaps and does not duplicate the foundational infrastructure issues ([#30](https://github.com/fiji/fiji-llm/issues/30)-[#40](https://github.com/fiji/fiji-llm/issues/40)).

## Tier 0: Essential Concepts
The assistant should be able to answer:
> **“What do you know about my current Fiji state, and how do you know it?”**

That is the conceptual center of the project.

The differentiator is not merely that the assistant can produce an ImageJ macro. General LLMs can already attempt that. The differentiator is that Fiji-LLM can connect model reasoning with the participant’s actual Fiji state and make that provenance visible.

Making that provenance visible will also help users develop an accurate mental model and catch failures.

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| Explain what the assistant knows about current Fiji state and how it knows it | **Partial** | The chat attaches selected context and exposes live script tools, but it lacks a general provenance contract. Drafts: issues 1, 2, and 8 in [DEVELOPMENT-CAPABILITY-ISSUES.md](DEVELOPMENT-CAPABILITY-ISSUES.md). |
| Connect reasoning with the participant’s actual installation | **Partial** | Provider/model lists are live, but command and plugin discovery tools are dormant. Draft: issue 1; related: [#25](https://github.com/fiji/fiji-llm/issues/25), [#30](https://github.com/fiji/fiji-llm/issues/30). |
| Connect reasoning with active images | **Partial** | Image metadata can be attached from the UI; image querying and pixel/media input are incomplete. Draft: issue 2; related: [#40](https://github.com/fiji/fiji-llm/issues/40). |
| Connect reasoning with installed capabilities | **Not supported** | No complete live capability inventory is exposed to the assistant. Draft: issue 1. |
| Connect reasoning with open scripts | **Supported** | Script context suppliers and the registered Script Editor tool expose open scripts. |
| Connect reasoning with recorded actions | **Not supported** | Macro recorder integration is dormant and no action history is available. Draft: issue 6. |
| Connect reasoning with execution results | **Not supported** | There are no live tools for histogram, ROI, measurement, Results table, or structured command outputs. Drafts: issues 3 and 5. |
| Make provenance visible to users | **Partial** | Attached context labels are visible, but observed facts, participant input, model knowledge, and suggestions are not systematically separated. Drafts: issues 2 and 8; related: [#30](https://github.com/fiji/fiji-llm/issues/30), [#31](https://github.com/fiji/fiji-llm/issues/31). |
## Tier 1: Workshop-critical capabilities

### A. Chat UI and interaction

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| Start a new conversation reliably | **Partial** | [FijiAssistantChat.java](../src/main/java/sc/fiji/llm/ui/FijiAssistantChat.java) creates a conversation on the first message; the New Conversation button is disabled for an empty conversation. Related: [#16](https://github.com/fiji/fiji-llm/issues/16). |
| Select a supported provider and model | **Supported** | [Fiji_Chat.java](../src/main/java/sc/fiji/llm/commands/Fiji_Chat.java) populates provider/model choices and validates the selected model. Related: [#17](https://github.com/fiji/fiji-llm/issues/17). |
| Explain provider/model requirements in the UI | **Partial** | [Fiji_Chat.java](../src/main/java/sc/fiji/llm/commands/Fiji_Chat.java) provides provider guidance, documentation links, and API-key configuration, but no model capability descriptor. Related: [#30](https://github.com/fiji/fiji-llm/issues/30). |
| Attach the active image as context | **Supported** | [ImageMetaContextSupplier.java](../src/main/java/sc/fiji/llm/image/ImageMetaContextSupplier.java) attaches image metadata; it does not attach pixels. Related: [#40](https://github.com/fiji/fiji-llm/issues/40). |
| Attach an open script or macro as context | **Supported** | [ScriptContextSupplier.java](../src/main/java/sc/fiji/llm/script/ScriptContextSupplier.java) lists open editor tabs and supports active-script context. |
| Clearly display which context items are attached | **Supported** | [FijiAssistantChat.java](../src/main/java/sc/fiji/llm/ui/FijiAssistantChat.java) renders attached context tags. |
| Remove an accidentally attached context item | **Supported** | [FijiAssistantChat.java](../src/main/java/sc/fiji/llm/ui/FijiAssistantChat.java) supports per-item removal and Clear All. |
| Preserve conversation state during the exercise | **Supported** | [DefaultConversationService.java](../src/main/java/sc/fiji/llm/chat/DefaultConversationService.java) stores conversation messages in memory and persists JSON history. Related: [#16](https://github.com/fiji/fiji-llm/issues/16). |
| Stop or cancel a long-running response | **Supported** | [FijiAssistantChat.java](../src/main/java/sc/fiji/llm/ui/FijiAssistantChat.java) cancels the streaming handle; cancellation of a running tool needs [#31](https://github.com/fiji/fiji-llm/issues/31) and [#32](https://github.com/fiji/fiji-llm/issues/32). |
| Distinguish ordinary text responses from tool actions | **Partial** | LangChain4j callbacks are connected in [FijiAssistantChat.java](../src/main/java/sc/fiji/llm/ui/FijiAssistantChat.java), but the UI does not render tool actions as distinct entries. Related: [#31](https://github.com/fiji/fiji-llm/issues/31). |
| Show tool activity and whether it succeeded or failed | **Partial** | Tool callbacks exist, while [DefaultAiToolService.java](../src/main/java/sc/fiji/llm/tools/DefaultAiToolService.java) has no-op lifecycle handlers and no activity panel. Related: [#31](https://github.com/fiji/fiji-llm/issues/31), [#32](https://github.com/fiji/fiji-llm/issues/32). |
| Render code in a copyable, legible form | **Supported** | [ChatMessagePanel.java](../src/main/java/sc/fiji/llm/ui/ChatMessagePanel.java) renders Markdown code blocks and provides text copying. |
| Move generated code into the Script Editor | **Supported** | [ScriptEditorTool.java](../src/main/java/sc/fiji/llm/script/ScriptEditorTool.java) creates and updates editor tabs. |
| Recover cleanly from provider, authentication, rate-limit, and tool errors | **Partial** | [FijiAssistantChat.java](../src/main/java/sc/fiji/llm/ui/FijiAssistantChat.java) handles rate limits and generic errors; tool errors are generic and chat errors are truncated. Related: [#31](https://github.com/fiji/fiji-llm/issues/31), [#38](https://github.com/fiji/fiji-llm/issues/38). |
| Provide obvious access to concise help | **Supported** | [InteractiveGuide.java](../src/main/java/sc/fiji/llm/ui/InteractiveGuide.java) provides an in-chat UI tour. |
### B. Fiji environment awareness
The assistant must reliably obtain:

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| Active image name | **Supported** | [ImageMetaContextSupplier.java](../src/main/java/sc/fiji/llm/image/ImageMetaContextSupplier.java) obtains the title for an attached image context item. |
| Image dimensions | **Supported** | [ImageMetaContextSupplier.java](../src/main/java/sc/fiji/llm/image/ImageMetaContextSupplier.java) extracts dataset axis lengths. |
| Channel, Z, and time dimensions | **Supported** | Dataset axis types are serialized when present. |
| Bit depth or image type | **Supported** | Dataset pixel type is included in image context. |
| Spatial calibration and units | **Not supported** | Calibration is not extracted by [ImageMetaContextSupplier.java](../src/main/java/sc/fiji/llm/image/ImageMetaContextSupplier.java). Draft: issue 2. |
| Current slice/channel/frame | **Partial** | Dimension lengths are available, but current position is not captured. Draft: issue 2. |
| Whether a selection or ROI exists | **Not supported** | No ROI context supplier or live ROI query was found. Drafts: issues 2 and 3. |
| Open image list | **Partial** | The image context supplier can list images for a UI dropdown, but [ImageTool.java](../src/main/java/sc/fiji/llm/image/ImageTool.java) is not registered as an AI tool. Draft: issue 1. |
| Open Script Editor documents | **Supported** | [ScriptEditorTool.java](../src/main/java/sc/fiji/llm/script/ScriptEditorTool.java) and [ScriptContextSupplier.java](../src/main/java/sc/fiji/llm/script/ScriptContextSupplier.java) expose open editors/tabs. |
| Relevant installed commands | **Not supported** | [CommandInteractionTool.java](../src/main/java/sc/fiji/llm/macro/CommandInteractionTool.java) is present but its AI-tool registration is commented out. Draft: issue 1. |
| Enough plugin/update-site information to avoid recommending unavailable commands | **Not supported** | No live command capability inventory or update-site inspection was found. Drafts: issues 1 and 10. |
| Clearly differentiate facts observed from Fiji, participant information, model knowledge, and guesses | **Partial** | Context is labeled as attached user context, but there is no general provenance contract for observations versus inferences. Drafts: issues 2 and 8; related: [#30](https://github.com/fiji/fiji-llm/issues/30), [#31](https://github.com/fiji/fiji-llm/issues/31). |
### C. Command discovery and explanation

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| Handle "Where is the histogram command?" | **Not supported** | No live command-search tool is registered. Draft: issue 1. |
| Handle "What installed tools can threshold this image?" | **Not supported** | The dormant command search would query the module registry, but is not exposed to the assistant. Draft: issue 1. |
| Handle "How do I measure the area of selected objects?" | **Not supported** | No live command discovery or ROI/measurement inspection path. Drafts: issues 1 and 3. |
| Explain the difference between setting a threshold and converting to a mask | **Partial** | The model can provide general knowledge, but the library supplies no Fiji-specific command metadata or result inspection. Drafts: issues 1 and 3. |
| Handle "Is the command you suggested actually installed here?" | **Not supported** | The available ModuleSearcher integration is in an unregistered tool. Draft: issue 1. |

A successful response should use the actual Fiji environment rather than relying only on memorized menu paths.
### D. Safe execution

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| State what it proposes to do | **Partial** | The system/tool usage prompts encourage a search-then-run sequence, but there is no structured execution plan. Drafts: issues 4 and 5; related: [#32](https://github.com/fiji/fiji-llm/issues/32). |
| Identify the target image | **Partial** | Image context includes an id, but command execution has no required target-image contract. Drafts: issues 2, 4, and 5. |
| Preserve the original by default, or explicitly ask before destructive work | **Not supported** | No duplicate-by-default or approval path is implemented for Fiji commands. Draft: issue 5; related: [#32](https://github.com/fiji/fiji-llm/issues/32). |
| Expose important parameters | **Partial** | Interactive commands can show a Fiji dialog, but non-interactive commands are blocked and module parameters are not presented as a plan. Draft: issue 5. |
| Report whether execution succeeded | **Partial** | The dormant command tool returns a generic success/error string; there is no live generic command path or structured result. Draft: issue 5; related: [#31](https://github.com/fiji/fiji-llm/issues/31). |
| Describe resulting images, tables, or ROIs | **Not supported** | No result introspection tools were found. Drafts: issues 3 and 5. |
| Avoid claiming scientific success merely because a command completed | **Partial** | Prompt guidance can encourage caution, but execution results do not carry an enforced scientific-interpretation boundary. Drafts: issues 4 and 5. |
### E. Script and macro workflow

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| Generate a small ImageJ macro for the demonstrated workflow | **Supported** | The LLM can generate code and [ScriptEditorTool.java](../src/main/java/sc/fiji/llm/script/ScriptEditorTool.java) can place it in an editor. |
| Place it in or transfer it to the Script Editor | **Supported** | Live `ScriptEditorTool` create/update operations are registered. |
| Explain the generated code | **Supported** | The assistant can explain the code in chat, although the explanation is model-generated rather than validated by Fiji. |
| Edit the script in response to follow-up instructions | **Supported** | [ScriptEditorTool.java](../src/main/java/sc/fiji/llm/script/ScriptEditorTool.java) supports script updates and line ranges. |
| Diagnose basic errors | **Partial** | Editor/tool failures are returned, but there is no language-aware syntax validation or script execution in the current checkout. Draft: issue 7; related: [#24](https://github.com/fiji/fiji-llm/issues/24). |
| Use available context from the Macro Recorder | **Not supported** | [ImageJMacroTool.java](../src/main/java/sc/fiji/llm/macro/ImageJMacroTool.java) is not registered and no live recorder context path was found. Draft: issue 6. |
| Convert a successful manual sequence into inspectable code | **Not supported** | No action history or recorder export path is live. Draft: issue 6. |
| Parameterize threshold, size limits, paths, or other relevant values | **Supported** | Script creation/editing plus model-generated follow-up edits support this workflow. |
| Avoid silently using APIs unavailable in the selected scripting language | **Partial** | [MacroFunctionRegistry.java](../src/main/java/sc/fiji/llm/macro/MacroFunctionRegistry.java) exists, but language-aware validation is not connected. Draft: issue 7; related: [#2](https://github.com/fiji/fiji-llm/issues/2), [#24](https://github.com/fiji/fiji-llm/issues/24). |
### F. Basic failure behavior

| Failure case | Coverage | Evidence / related issue |
|---|---|---|
| No image is open | **Partial** | The image context UI reports that no active image is available, but there is no live assistant state query or structured preflight. Draft: issue 4. |
| Multiple images are open | **Partial** | The UI can list selectable image context items, but the assistant lacks a live image-list tool and target-selection contract. Drafts: issues 1 and 4. |
| The wrong image is active | **Partial** | Image ids can be attached manually, but no live assistant operation verifies or selects the intended image. Drafts: issues 2 and 4. |
| No ROI exists | **Not supported** | No ROI state query or operation-specific preflight was found. Drafts: issues 2 and 4. |
| Calibration is missing | **Not supported** | Calibration is not captured and no measurement preflight exists. Drafts: issues 2 and 4. |
| A command is unavailable | **Not supported** | The dormant command tool has an error path, but no live command discovery/execution tool is registered. Draft: issue 1. |
| Tool execution fails | **Partial** | Registered script tools and generic callbacks return errors, but failures are not structured or clearly shown as tool activity. Related: [#31](https://github.com/fiji/fiji-llm/issues/31), [#32](https://github.com/fiji/fiji-llm/issues/32); draft: issue 8. |
| The model does not support tool use | **Not supported** | No current model capability check prevents this failure. Related: [#17](https://github.com/fiji/fiji-llm/issues/17), [#30](https://github.com/fiji/fiji-llm/issues/30); closed [#5](https://github.com/fiji/fiji-llm/issues/5) was closed in favor of [#17](https://github.com/fiji/fiji-llm/issues/17). |
| A user asks for a scientifically underspecified analysis | **Partial** | The system prompt can guide the model to ask questions, but there is no Fiji-specific clarification/preflight contract. Draft: issue 4; related: [#33](https://github.com/fiji/fiji-llm/issues/33), [#34](https://github.com/fiji/fiji-llm/issues/34). |
| The user requests something outside current capabilities | **Partial** | The model can decline or explain limitations, but there is no authoritative capability inventory. Draft: issue 1; related: [#30](https://github.com/fiji/fiji-llm/issues/30). |

A graceful, specific failure is a successful workshop outcome. A confident fabrication is not.

## Tier 2: Important capabilities
These significantly improve the workshop but need not all be on the shared critical path.

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| Inspect histogram or summary statistics | **Not supported** | No live image statistics tool. Draft: issue 3. |
| Report saturation or value-range concerns without overinterpreting them | **Not supported** | No live statistics or saturation inspection path. Draft: issue 3. |
| List and explain current ROIs | **Not supported** | No ROI context item or tool. Draft: issue 3. |
| Inspect measurement settings | **Not supported** | No measurement-settings tool. Draft: issue 3. |
| Inspect or summarize Results table output | **Not supported** | No Results-table inspection tool. Draft: issue 3. |
| Recommend candidate workflows and compare tradeoffs | **Partial** | The model can reason about generic workflows, but no Fiji-specific workflow knowledge or observed-result feedback is supplied. Related: [#2](https://github.com/fiji/fiji-llm/issues/2), [#37](https://github.com/fiji/fiji-llm/issues/37). |
| Search available commands by analysis goal | **Not supported** | The command-search implementation is not registered. Draft: issue 1. |
| Explain how a command’s parameters affect results | **Partial** | The model can explain from general knowledge, but module parameter metadata is not exposed to it. Drafts: issues 1 and 5. |
| Create a batch-processing version of a macro | **Supported** | The Script Editor tool can create/update scripts and the model can generate the batch loop. |
| Use the Macro Recorder as evidence for valid command syntax | **Not supported** | No live recorder tool or action-history path. Draft: issue 6. |
| Summarize the sequence of actions already performed | **Not supported** | No action history is recorded. Draft: issue 6. |
| Produce a concise “what to validate next” checklist | **Partial** | The model can generate a generic checklist, but it cannot ground it in Fiji results. Drafts: issues 3, 4, and 6. |
| Generate a forum-ready problem report containing environment and error details, while excluding secrets or private data | **Not supported** | No sanitized diagnostic-report generator. Draft: issue 9. |
## Tier 3: Demonstration or stretch capabilities
These are attractive but risky as core workshop dependencies:

| Capability | Coverage | Evidence / related issue |
|---|---|---|
| Building TrackMate workflows | **Not supported** | No TrackMate adapter or capability-specific workflow contract. Not required for this workshop; not drafted as an issue. |
| StarDist or Labkit setup and execution | **Not supported** | No StarDist/Labkit adapter or setup path. Not required for this workshop; not drafted as an issue. |
| Suggestion or activation of update sites | **Not supported** | No update-site inspection or approval workflow. Not required for this workshop; not drafted as an issue. Loosely related: [#25](https://github.com/fiji/fiji-llm/issues/25). |
| Robust analysis of arbitrary participant datasets | **Partial** | Basic image metadata works for open datasets, but calibration, ROI, preflight, result inspection, and safety controls are incomplete. Drafts: issues 2-5. |
| Interactive workflow execution, prompting the user for steps | **Partial** | Chat can stream and call tools, but explicit request-for-input and approval loops are not implemented. Related: [#31](https://github.com/fiji/fiji-llm/issues/31)-[#34](https://github.com/fiji/fiji-llm/issues/34); draft: issue 4. |
| Visual interpretation of biological structures | **Partial** | The current implementation supplies image metadata rather than pixels or computer-vision results. Related: [#40](https://github.com/fiji/fiji-llm/issues/40). |
| Local-model setup during the session | **Supported** | [AbstractOllamaProvider.java](../src/main/java/sc/fiji/llm/provider/AbstractOllamaProvider.java) and [OllamaProcessManager.java](../src/main/java/sc/fiji/llm/provider/OllamaProcessManager.java) start Ollama and pull models, subject to local installation. Related: [#17](https://github.com/fiji/fiji-llm/issues/17), [#28](https://github.com/fiji/fiji-llm/issues/28). |
| External MCP client demonstrations | **Partial** | The checkout provides a local MCP server/client loop for Fiji tools, but not a configurable external MCP client adapter. Related: [#39](https://github.com/fiji/fiji-llm/issues/39). |
# Prompt acceptance suite
These prompts can become regression tests across recommended models. Maintain a canonical set and record whether each model:
- understood the request;
- selected the correct tools;
- requested necessary clarification;
- used actual context;
- avoided fabrication;
- completed the task;
- explained the result at an appropriate level.
## Orientation
1. **“What images are currently open in Fiji, and which one is active?”**
2. **“Describe the active image using information available from Fiji. Separate observed facts from anything you are inferring.”**
3. **“What are the dimensions, bit depth, channels, slices, frames, and spatial calibration of this image?”**
4. **“Which channel, slice, or frame am I currently viewing? Is there an active selection (ROI) on this image?”**
5. **“I am new to Fiji. Explain what I am looking at without assuming I know image-analysis terminology.”**
## Discovery
1. **“I want to inspect the distribution of pixel values. Find the relevant installed command and tell me what it will show.”**
2. **“What installed tools could help me separate bright objects from a darker background? Give me two simple options and explain the tradeoffs.”**
3. **“Is Analyze Particles available in this Fiji installation? What input does it expect?”**
4. **“Do not invent a menu path. Use Fiji’s available commands to find the operation.”**
5. **“What’s the difference between setting a threshold and actually converting the image to a binary mask?”**
## Guided analysis
1. **“I want to count bright, separated objects and measure their areas. Before doing anything, ask me the questions needed to choose a reasonable workflow.”**
2. **“Show me the histogram of the active image and tell me if there’s evidence of saturation before we choose a threshold.”**
3. **“Guide me through this one step at a time. Preserve the original image.”**
4. **“Show me which settings require scientific judgment rather than choosing them silently.”**
5. **“Apply the agreed thresholding step to a duplicate and tell me what changed.”**
6. **“Help me inspect whether the result matches the objects I intended to identify.”**
7. **“Run the measurement step and summarize what outputs Fiji created. Do not interpret biological significance.”**
## Reproducibility
1. **“Turn the workflow we just performed into an ImageJ macro.”**
2. **“Open the macro in the Script Editor and add comments explaining each step.”**
3. **“Replace hard-coded analysis values with clearly named variables at the top.”**
4. **“Adapt this macro to process all TIFF files in a folder, while keeping the original files unchanged.”**
5. **“Review this macro for commands or syntax that may not work in ImageJ macro language.”**
6. **“Compare the macro you just generated with what the Macro Recorder captured while I performed the same steps manually. Are they consistent?”**
## Error recovery
1. **“Why did the last command fail? Use the error and current Fiji state rather than guessing.”**
2. **“There is no active image. Explain what I need to do before continuing.”**
3. **“The spatial calibration is missing. What measurements would be affected?”**
4. **“The command you proposed is not installed. Find an available alternative or explain what is missing.”**
5. **“You appear to have assumed something about my image. Identify the assumption and revise the plan.”**
6. **“Three images are open and I didn’t say which one to use. Ask me instead of guessing.”**
7. **“I asked you to measure the selected region, but I never made a selection. What should happen?”**
8. **“The tool you just called failed. Tell me exactly what went wrong instead of assuming it worked.”**
## Metacognition and trust
1. **“What information from Fiji did you use to answer me?”**
2. **“What parts of your proposed workflow are uncertain?”**
3. **“What should I check manually before accepting these measurements?”**
4. **“Explain what you can do here that a general web chatbot cannot.”**
5. **“Summarize what we did in a form I could include in my analysis notes.”**
6. **“Generate a summary of this error I can post to the Fiji forum, without including my API key or file paths.”**
