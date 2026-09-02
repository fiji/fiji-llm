# Development Capability Issue Drafts

Assessment date: 2026-09-02

This document records the current coverage assessment for the workshop targets and
contains draft text for new issues only. No issue was created or modified on GitHub.
The assessment is based on the current checkout and a read-only review of the Fiji
LLM issue tracker, especially [#30](https://github.com/fiji/fiji-llm/issues/30), [#31](https://github.com/fiji/fiji-llm/issues/31), [#32](https://github.com/fiji/fiji-llm/issues/32), [#33](https://github.com/fiji/fiji-llm/issues/33), [#34](https://github.com/fiji/fiji-llm/issues/34), [#35](https://github.com/fiji/fiji-llm/issues/35), [#36](https://github.com/fiji/fiji-llm/issues/36), [#37](https://github.com/fiji/fiji-llm/issues/37), [#38](https://github.com/fiji/fiji-llm/issues/38), [#39](https://github.com/fiji/fiji-llm/issues/39), and [#40](https://github.com/fiji/fiji-llm/issues/40).

## Status vocabulary

- **Supported**: a live, user-reachable implementation exists in the current checkout.
- **Partial**: some implementation or UI exists, but the complete workshop behavior is
  not reliable, not exposed to the assistant, or lacks an important safety/validation
  part.
- **Not supported**: no live implementation was found for the target behavior.
- **Not verified**: the behavior depends on a provider, model, or Fiji runtime test that
  was not available for this source-level assessment.

## Existing issues that cover related gaps

These targets do not need duplicate new issues. They should be considered dependencies
or related work when prioritizing the drafts below.

| Capability area | Existing issue(s) | Coverage of the workshop target |
|---|---|---|
| Conversation refactoring and persistence | [#16](https://github.com/fiji/fiji-llm/issues/16) | Covers conversation lifecycle and persistence follow-up, but not all UI reliability details. |
| Ollama model selection and local model behavior | [#17](https://github.com/fiji/fiji-llm/issues/17), [#28](https://github.com/fiji/fiji-llm/issues/28), closed [#5](https://github.com/fiji/fiji-llm/issues/5) | Covers arbitrary/local model handling and long model startup; closed [#5](https://github.com/fiji/fiji-llm/issues/5) was explicitly closed in favor of [#17](https://github.com/fiji/fiji-llm/issues/17). |
| MCP server security | [#23](https://github.com/fiji/fiji-llm/issues/23) | Covers localhost binding and Origin validation. |
| Script execution | [#24](https://github.com/fiji/fiji-llm/issues/24) | Covers a future `fiji_script_run` contract; the current checkout does not yet provide it. |
| Tool management and scope UI | [#25](https://github.com/fiji/fiji-llm/issues/25) | Covers a future way to inspect and manage tools; it does not by itself make dormant tools live. |
| Visual interrogation and media input | [#26](https://github.com/fiji/fiji-llm/issues/26), [#40](https://github.com/fiji/fiji-llm/issues/40) | Covers image/media requests and vision-capable model integration. |
| Context retrieval as tools | closed [#7](https://github.com/fiji/fiji-llm/issues/7), [#9](https://github.com/fiji/fiji-llm/issues/9) | [#7](https://github.com/fiji/fiji-llm/issues/7) is closed, but the current source still needs verification that all expected context tools are live. [#9](https://github.com/fiji/fiji-llm/issues/9) tracks MCP resources. |
| Provider/model capability metadata | [#30](https://github.com/fiji/fiji-llm/issues/30) | Covers model requirements such as tool use and multimodal input. |
| Assistant and tool run events | [#31](https://github.com/fiji/fiji-llm/issues/31) | Covers structured tool activity, errors, cancellation, and completion events. |
| Tool guardrails | [#32](https://github.com/fiji/fiji-llm/issues/32) | Covers generic confirmation, cancellation, limits, and progress. |
| Headless bounded execution | [#33](https://github.com/fiji/fiji-llm/issues/33) | Covers a reusable execution service outside the Swing UI. |
| Workflow composition | [#34](https://github.com/fiji/fiji-llm/issues/34) | Covers sequential steps, delegation, handoffs, and request-for-input states. |
| Run state and artifacts | [#35](https://github.com/fiji/fiji-llm/issues/35), [#40](https://github.com/fiji/fiji-llm/issues/40) | Covers resumability and references to scripts, tables, plots, and result images. |
| Chat memory and context budgets | [#36](https://github.com/fiji/fiji-llm/issues/36) | Covers persistence, compaction, and context selection policies. |
| Retrieval | [#37](https://github.com/fiji/fiji-llm/issues/37) | Covers provider-neutral knowledge retrieval. |
| Metrics | [#38](https://github.com/fiji/fiji-llm/issues/38) | Covers usage, latency, retry, and tool metrics. |
| External MCP clients | [#39](https://github.com/fiji/fiji-llm/issues/39) | Covers consuming tools hosted by other MCP servers. |

## Proposed issue drafts

The drafts are ordered by implementation dependency and workshop value. The first
four form the minimum technical foundation for a reliable guided analysis. The later
drafts improve reproducibility, diagnostics, and stretch demonstrations.

### 1. Expose a live Fiji capability inventory to assistant tools

**Problem:** The checkout contains `ImageTool`, `CommandInteractionTool`, and
`ImageJMacroTool`, but their `@Plugin(type = AiToolPlugin.class)` registrations are
commented out. Only the script editor tool is visibly registered as an AI tool. Image
and script context suppliers can populate UI attachments, but the assistant cannot
reliably query the installed command/tool surface. The MCP server can therefore expose
an incomplete capability set while its instructions imply that context tools exist.

**Goal:** Make the workshop-critical, read-only Fiji discovery capabilities live and
observable through the same `AiToolService` path used by the in-app assistant and MCP.
Return actual installed capabilities rather than relying on model memory.

**Technical goals:**

- Decide which existing image, command, and macro tools are safe for `ANY`, `MACRO`, or
  another scope, then register the approved tools through SciJava.
- Ensure `AiToolService.getToolsForContext(...)`, the chat request, and the MCP server
  expose the same approved tool set without duplicate names.
- Add read-only discovery for installed commands and, where the SciJava APIs provide it,
  the plugin/update-site source associated with a command.
- Return structured errors for an empty image list, unavailable command, unavailable
  recorder, and failed discovery.
- Add registry/integration tests that assert the expected tool names and scopes in a
  minimal SciJava context.

**Acceptance criteria:**

- A test or diagnostic command can enumerate the tools exposed to the in-app assistant
  and MCP, including the intended image-list, image-details, command-search, and
  macro-recorder capabilities.
- The assistant can verify that a command is installed before recommending or running it.
- Search results identify the command name and menu path and do not claim availability
  for commands absent from the current registry.
- No mutating command becomes available merely because discovery tools were enabled.
- Tool registration failures are visible in logs and do not silently produce an empty
  or misleading capability list.

### 2. Add a complete Fiji image and selection state snapshot

**Problem:** Image context currently includes title, dimensions, axis labels, and pixel
type. It does not include spatial calibration, units, current channel/Z/time position,
or selection/ROI state. The assistant cannot reliably distinguish a missing image,
missing calibration, and a user selection that is required by a workflow.

**Goal:** Provide a structured, provider-neutral snapshot of the relevant Fiji image
state for context attachment and state-query tools. The snapshot must describe observed
Fiji facts without implying biological interpretation.

**Technical goals:**

- Extend the image state model with image identity, dimensions and axes, pixel type,
  calibration values and units, current channel/Z/frame, and selection/ROI presence.
- Represent absent calibration, absent selection, and unavailable fields explicitly
  rather than using empty strings that look like valid values.
- Support ImageJ1-backed and ImageJ2-backed images through existing helper/services.
- Include an observation timestamp or snapshot identifier so a later result can be
  related to the state that was inspected.
- Mark values as observed application state; keep participant-provided facts and model
  inferences separate from the snapshot.
- Add deterministic tests for no image, one image, multiple images, calibrated data,
  uncalibrated data, and an active ROI.

**Acceptance criteria:**

- A caller can obtain one JSON snapshot containing title/id, axes and lengths, pixel
  type, calibration and units, current position, and ROI/selection state.
- Missing values have explicit machine-readable status, including `not_available` or
  `not_present` where appropriate.
- The active image and current position are read from Fiji at request time, not cached
  indefinitely in the chat window.
- The snapshot can be attached to a message without sending pixel data to a text-only
  provider.
- Tests cover both the normal state and the required failure states.

### 3. Add image-analysis inspection tools for measurements and results

**Problem:** The workshop workflow depends on inspecting a histogram, value range,
threshold result, ROIs, measurement settings, and the Results table. The current
checkout has no live tools for those observations, so an assistant can propose an
analysis but cannot help the participant check what Fiji actually produced.

**Goal:** Add narrowly scoped, read-only tools for inspecting analysis inputs and
outputs. Keep scientific interpretation with the user and model, while making the
underlying Fiji observations available and provenance-preserving.

**Technical goals:**

- Add a histogram/statistics operation that reports value range, summary statistics,
  histogram information, and saturation counts when Fiji can calculate them.
- Add ROI inspection with count, type, bounds, and calibrated measurements where
  available, without assuming that an ROI represents a biological object.
- Add measurement-settings inspection and a Results-table summary with column names,
  row count, representative values, and table identity.
- Define stable JSON result types for empty tables, absent ROIs, unavailable statistics,
  and calculation errors.
- Include the source image/table/ROI identity and observation time in every result.
- Add tests using small deterministic images and Results tables.

**Acceptance criteria:**

- The assistant can ask Fiji for histogram/statistics information without changing the
  image.
- The assistant can list current ROIs and explain which measurements are pixel-based
  or calibrated, without claiming biological meaning.
- The assistant can report the active measurement settings and summarize an existing
  Results table, including clear empty-state responses.
- Results identify the Fiji object inspected and distinguish an unavailable value from
  a measured zero.
- The tools do not mutate the image, ROI manager, measurement settings, or Results table.

### 4. Add Fiji-specific workflow preflight and clarification support

**Problem:** Current tool and chat paths do not provide a structured preflight before
an analysis begins. A model may proceed with no image, the wrong active image, missing
calibration, no ROI, or scientifically underspecified parameters. Generic workflow and
approval APIs in [#32](https://github.com/fiji/fiji-llm/issues/32)-[#34](https://github.com/fiji/fiji-llm/issues/34) will not by themselves know which Fiji state is required.

**Goal:** Provide a deterministic Fiji preflight contract that reports required inputs,
missing conditions, ambiguities, and suggested questions before a mutating step runs.

**Technical goals:**

- Define a preflight request containing the intended operation, target image identity,
  required state, and parameters supplied by the participant.
- Compare the request to the live image/ROI/calibration state and return structured
  `ready`, `needs_input`, or `blocked` results.
- Detect no image, multiple possible target images, stale target identity, missing
  calibration, absent ROI, unsupported dimensionality, and missing parameters.
- Allow the assistant to ask one or more explicit clarification questions and continue
  only after the participant resolves them.
- Keep scientific assumptions visible; never turn an inferred biological label into a
  satisfied technical precondition.

**Acceptance criteria:**

- A preflight for counting calibrated objects reports whether an image, target, pixel
  calibration, threshold parameters, and size limits are available.
- No-image and ambiguous-image cases identify the missing user decision instead of
  selecting silently.
- A missing ROI or calibration is reported only when the requested operation requires it.
- A preflight result can be consumed by the in-app chat, a headless caller, and the
  future workflow API in [#34](https://github.com/fiji/fiji-llm/issues/34).
- Tests cover all failure cases listed above and a ready case for a simple duplicate,
  threshold, and measurement workflow.

### 5. Add safe, inspectable Fiji command execution and result reporting

**Problem:** The existing command execution class is not registered as a live tool and,
where enabled, only permits a narrow set of interactive/sample commands. It does not
identify a target image in the execution contract, duplicate the original by default,
request explicit approval for destructive work, or summarize resulting images, ROIs,
tables, and errors. Generic tool guardrails in [#32](https://github.com/fiji/fiji-llm/issues/32) need a Fiji-specific execution
adapter.

**Goal:** Provide a bounded Fiji command execution path that makes the intended action,
target, parameters, safety decision, and observed result explicit.

**Technical goals:**

- Add command metadata and a pre-execution plan containing command identity, target
  image id, parameters, mutability, and expected output types.
- Classify commands as read-only, duplicate-safe, or mutating, with an explicit fallback
  when classification is unknown.
- Duplicate or otherwise preserve the original image by default for mutating analysis
  commands; require an auditable confirmation when preservation is not possible.
- Use SciJava module parameter metadata or an interactive UI to expose important values;
  never invent a parameter name or silently choose a scientific threshold.
- Return a structured execution result containing status, target/output identities,
  created images/tables/ROIs, warnings, and the full useful error detail.
- Integrate with the common tool-event and approval work from issues 31 and 32.

**Acceptance criteria:**

- Before execution, the user/model can inspect a plan naming the command, target image,
  parameters, mutability, and preservation action.
- A mutating command leaves the original unchanged by default and reports the duplicate
  used for analysis.
- Destructive or unknown operations cannot run without an explicit approval decision.
- Success reports the observed Fiji outputs; it does not claim that the scientific goal
  was achieved merely because the module completed.
- Failure reports distinguish unavailable command, invalid arguments, user cancellation,
  module failure, and result-inspection failure.
- Tests cover a read-only command, a mutating command on a duplicate, cancellation,
  and an unavailable command.

### 6. Capture macro-recorder output and Fiji action history as artifacts

**Problem:** The workshop payoff is turning a successful manual sequence into
inspectable code. The current macro and recorder tool classes are dormant, and there is
no action history that can summarize thresholding, masking, particle analysis, or
measurement steps performed by the participant.

**Goal:** Capture reproducible Fiji actions and make the resulting macro/script and
history available to the assistant and Script Editor without relying on model guesswork.

**Technical goals:**

- Define a session-scoped action event containing command identity, menu path, target
  object, parameters/options, timestamp, and success/failure state.
- Integrate with ImageJ's macro recorder where available and preserve the exact recorded
  command options.
- Provide start, stop, clear, inspect, and export operations with clear ownership of
  the active recording session.
- Convert a completed recording into an inspectable ImageJ macro and optionally place it
  in the Script Editor.
- Exclude secrets and avoid embedding large image data in the action history.
- Link recorded actions to execution results and artifact references from [#35](https://github.com/fiji/fiji-llm/issues/35).

**Acceptance criteria:**

- A participant can record a short manual sequence and retrieve the exact recorded
  command/options as a macro or structured action list.
- The assistant can summarize actions already performed, including failed or cancelled
  steps, without inventing steps.
- The exported macro is placed in the Script Editor with the selected language and is
  clearly marked for user inspection.
- An inactive recorder and an empty history produce explicit empty-state responses.
- Tests cover start/stop/clear, command ordering, options preservation, and export.

### 7. Add language-aware script validation and diagnostics

**Problem:** The script editor can create and update files, but the current tool path
does not reliably validate syntax, report line/column diagnostics, or check whether an
API is available in the selected scripting language. [#24](https://github.com/fiji/fiji-llm/issues/24) proposes script
execution, and [#2](https://github.com/fiji/fiji-llm/issues/2) covers rules prompts, but neither provides runtime validation
before the assistant presents a script as reusable.

**Goal:** Validate generated and edited scripts using the selected Fiji scripting
language and provide actionable diagnostics without silently claiming that a script is
valid.

**Technical goals:**

- Use the available SciJava `ScriptService` and language metadata to select a validator
  or execution-backed syntax check where possible.
- Define diagnostics with language, severity, line, column, message, and source range.
- Provide ImageJ macro-specific checks for known functions and command syntax, while
  marking unknown functions as unknown rather than definitely invalid.
- Expose validation through the Script Editor workflow and return diagnostics to the
  assistant as structured data.
- Keep execution output, syntax diagnostics, and model-generated explanation separate.

**Acceptance criteria:**

- A syntactically invalid macro or script returns a useful diagnostic with location when
  the selected language supports that information.
- A script written for a different language is not silently treated as valid because its
  file contents look plausible.
- Validation reports unsupported or unavailable APIs as warnings or unknowns according
  to evidence, not as fabricated certainty.
- A successful validation does not claim that the scientific analysis is correct.
- Tests cover at least ImageJ macro language and one additional installed scripting
  language, or document the unavailable language explicitly.

### 8. Generate sanitized, evidence-backed Fiji diagnostic reports

**Problem:** Provider, authentication, rate-limit, and tool failures currently reach the
chat as generic or truncated messages. Participants need a concise report containing
useful Fiji state and exact failure evidence, while avoiding API keys, private image
data, and unpublished content.

**Goal:** Provide a report model and UI action for creating a user-reviewable,
sanitation-aware diagnostic report from a failed assistant run or Fiji operation.

**Technical goals:**

- Combine provider/model identity, Fiji version and platform, run/tool identifiers,
  command parameters, error class/message, and relevant state snapshots.
- Integrate with the structured run events in [#31](https://github.com/fiji/fiji-llm/issues/31) and optional metrics in [#38](https://github.com/fiji/fiji-llm/issues/38).
- Redact API keys, authorization headers, local secrets, and private paths by default;
  exclude image pixels and script contents unless the user explicitly selects them.
- Preserve full internal error details for the report while keeping the chat display
  concise.
- Allow copy/export only after the user can review the report and remove fields.

**Acceptance criteria:**

- A failed tool run can produce a forum-ready report with exact failure type, useful
  message, provider/model, Fiji environment, and relevant observed state.
- The default report contains no API key, authorization value, image pixels, or full
  unpublished script content.
- The user can inspect and edit the report before copying or exporting it.
- A report identifies facts observed from Fiji separately from participant input and
  model suggestions.
- Tests cover redaction, missing optional fields, rate-limit errors, and tool failures.

### 9. Add optional plugin-specific workflow adapters for TrackMate, StarDist, and Labkit

**Problem:** TrackMate, StarDist, and Labkit are attractive demonstrations but are not
covered by generic command discovery alone. Their setup, model/plugin availability,
parameters, interactive steps, and output objects require plugin-specific knowledge.
No adapter for these workflows was found in the current checkout or existing issues.

**Goal:** Define optional, capability-checked adapters for selected plugin workflows
without making those plugins mandatory dependencies of the core artifact.

**Technical goals:**

- Discover whether each plugin and its required commands/models are installed before
  offering a workflow.
- Expose a small, version-tolerant plan/preview/execute contract and require explicit
  confirmation for installation, model downloads, or destructive changes.
- Return plugin outputs as Fiji artifacts with provenance and links to the source image.
- Keep plugin adapters in optional modules or SciJava plugins so core users are not
  forced to install every dependency.
- Add one deterministic smoke test or a clear unavailable-plugin response per adapter.

**Acceptance criteria:**

- The assistant never claims that TrackMate, StarDist, or Labkit is available without a
  live capability check.
- An installed adapter can show its required inputs and important parameters before
  execution.
- An unavailable plugin produces a specific response naming what is missing and does
  not silently activate an update site.
- Outputs and limitations are reported without claiming biological success.
- Each adapter documents the Fiji/plugin versions and manual validation steps required.

### 10. Add explicit, user-approved update-site inspection and activation

**Problem:** The assistant cannot currently inspect update-site state or safely suggest
activation. Automatically changing update sites during a workshop could alter the
installation, require a restart, or introduce an incompatible plugin.

**Goal:** Support read-only update-site inspection and optional, explicit activation as
a guarded workflow, separate from ordinary command discovery.

**Technical goals:**

- Expose enabled/disabled update sites and relevant installed-plugin metadata through a
  read-only capability query.
- Match a requested capability to a documented update site and show the source,
  packages, restart requirement, and compatibility caveats.
- Require explicit user approval before enabling an update site or installing anything;
  do not make changes from a suggestion alone.
- Report download, restart, and rollback/failure states through the common event API.
- Keep credentials, private repositories, and update-site configuration out of model
  prompts unless the user explicitly includes them.

**Acceptance criteria:**

- The assistant can report current update-site state without mutating the installation.
- A suggestion names the requested capability, source update site, expected changes, and
  restart requirement.
- Activation cannot happen without an explicit approval action and a visible plan.
- Failed or cancelled activation leaves an observable status and does not claim success.
- Tests cover read-only inspection, approval denial, unavailable site, and failure.

## Prioritization notes

For the September workshop, prioritize drafts 1-5. Drafts 1-4 establish the evidence
and safety path needed for the threshold/Analyze Particles exercise. Draft 5 makes
execution inspectable. Drafts 6-8 support the reproducibility and feedback portions of
the workshop. Drafts 9-10 are stretch work and should not be dependencies for the core
journey.

Issues 30-40 should be implemented as shared library foundations where possible. In
particular, capability metadata ([#30](https://github.com/fiji/fiji-llm/issues/30)), run events ([#31](https://github.com/fiji/fiji-llm/issues/31)), tool guardrails ([#32](https://github.com/fiji/fiji-llm/issues/32)), and
media/artifact requests ([#40](https://github.com/fiji/fiji-llm/issues/40)) should be reused by the Fiji-specific drafts instead of
creating parallel UI-only mechanisms.
