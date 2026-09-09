# Workshop Capability Gaps: Draft Issues

These are draft issues for gaps identified while validating the "Development
capability targets" tables in [WORKSHOP-PLAN.md](WORKSHOP-PLAN.md). Each one
is scoped to a specific, concrete change needed to make a workshop moment work
in a real Fiji install.

These are deliberately **not** duplicates of the foundational infrastructure
issues ([#30](https://github.com/fiji/fiji-llm/issues/30)-[#40](https://github.com/fiji/fiji-llm/issues/40)
from [FOUNDATION_ISSUES.md](FOUNDATION_ISSUES.md)). Those describe reusable,
provider-neutral infrastructure for other applications built on fiji-llm. The
issues below are about turning on and filling gaps in Fiji-specific behavior
the workshop actually exercises, whether or not that infrastructure exists yet.
Where a foundational issue is genuinely relevant, it's linked as "Related"
rather than treated as a substitute.

Two capability rows (TrackMate/StarDist/Labkit workflows, and update-site
suggestion/activation) are intentionally **not** drafted here. The workshop
plan itself marks them as stretch/demonstration capabilities, not required for
the minimum successful journey, so opening issues for them now would be
speculative.

## Issue 1: Turn on the command-search and image-list tools that already exist

**Title:** Register `ImageTool` and `CommandInteractionTool` as AI tools

**Why:** Workshop section 2 ("What commands or tools are relevant? Where can
I find a particular operation?") and the Discovery prompts need the assistant
to see real installed commands and open images, not guess from memory.

**What to actually do:**
- Uncomment `@Plugin(type = AiToolPlugin.class)` on `ImageTool`
  ([ImageTool.java](../src/main/java/sc/fiji/llm/image/ImageTool.java)) and on
  `CommandInteractionTool`
  ([CommandInteractionTool.java](../src/main/java/sc/fiji/llm/macro/CommandInteractionTool.java)).
  Both already implement working `@Tool` methods; they're just not discovered.
- Run the workshop's "Discovery" prompts (e.g. "Where is the histogram
  command?") against a real Fiji install and fix whatever breaks.
  `fiji_command_search` uses `SearchService`/`ModuleSearcher` with a 10-result,
  2-second timeout — confirm it returns real, currently-installed commands.
- Confirm `fiji_image_list` / `fiji_image_details` behave correctly with 0, 1,
  and multiple images open.

**Done when:** the "Orientation" and "Discovery" prompts in
[WORKSHOP-PLAN.md](WORKSHOP-PLAN.md) get real, grounded answers instead of
guesses.

**Related:** none of #30-#40 — this is wiring up existing Fiji-specific tools,
not generic tool infrastructure.

## Issue 2: Add calibration, current position, and ROI status to image context

**Title:** Extend image context with calibration, current position, and ROI
presence

**Why:** Workshop section 3 ("dimensions and calibration matter") and the
"calibration is missing" / "no ROI exists" failure drills need real data
instead of silence.

**What to actually do:**
- In `ImageMetaContextSupplier`
  ([ImageMetaContextSupplier.java](../src/main/java/sc/fiji/llm/image/ImageMetaContextSupplier.java)),
  pull spatial calibration (pixel width/height/depth and unit) and add it to
  `ImageMetaContextItem`.
- Add the current channel/slice/frame position, not just axis lengths.
- Add whether a selection/ROI currently exists (type/bounds if that's easy to
  include). There is currently no ROI context supplier at all.

**Done when:** asking "what's the calibration on this image?" or "is there a
selection right now?" gets a real, current answer.

**Related:** none of #30-#40 — this is Fiji-domain context, not generic
infrastructure.

## Issue 3: Add read-only tools for histogram, ROI list, measurement settings, and Results table

**Title:** Add read-only tools for image statistics, ROI list, measurement
settings, and Results table

**Why:** Workshop section 4 and the Tier 2 checklist need the assistant to
look at real results rather than describe what a command generically does.

**What to actually do:**
- Add small, read-only tools/context suppliers for: basic image
  statistics/histogram (min/max/mean, histogram bins if cheap), the current
  ROI Manager list, current measurement settings (`Set Measurements`), and
  Results table contents. None of these exist today — the only related code
  is macro function names listed in `MacroFunctionRegistry`, not live tools.
- Keep these pure reads with no side effects; execution safety is covered in
  Issue 5, not here.

**Done when:** "summarize the Results table" and "does this image have any
saturated pixels?" get answers grounded in real data.

**Related:** none of #30-#40.

## Issue 4: Check basic preconditions before acting

**Title:** Add preflight checks for missing/ambiguous image, calibration, and ROI

**Why:** Several "Basic failure behavior" and "Error recovery" prompts (no
image open, wrong image active, missing calibration, ambiguous which image)
have no dedicated check today — the assistant either guesses or gets a
generic empty state.

**What to actually do:**
- Before running an image-dependent tool, check: is there an active image at
  all? If multiple images are open, is it clear which one to use? Does this
  operation need calibration or an ROI that isn't there?
- If a precondition fails, say so plainly (e.g. "no active image is open" /
  "3 images are open, which one?") instead of proceeding or fabricating an
  answer.

**Done when:** the "Error recovery" prompts in [WORKSHOP-PLAN.md](WORKSHOP-PLAN.md)
produce an honest, specific answer instead of a guess.

**Related:** builds on Issues 1 and 2 above (needs the image list and
calibration/ROI status to check against). Loosely related to
[#32](https://github.com/fiji/fiji-llm/issues/32) (general tool
confirmation/limits contract), but this is a small Fiji-specific check that
doesn't need to wait for it.

## Issue 5: Make command execution safer before it runs

**Title:** Duplicate before destructive commands, show parameters first,
return structured results

**Why:** Workshop section 4 explicitly asks to "preserve the original" and
"identify parameters requiring user judgment" — neither is enforced today.

**What to actually do (all in
`CommandInteractionTool.runCommand()`,
[CommandInteractionTool.java](../src/main/java/sc/fiji/llm/macro/CommandInteractionTool.java)):**
- Duplicate the active image by default before running a command that isn't
  obviously read-only, or ask first if duplicating isn't sensible.
- Read `moduleInfo.getInputs()` and show the actual parameters to the
  model/user before running, instead of running blind.
- Return a structured result (succeeded/failed, produced outputs, error
  message) instead of the current generic `"Command executed: " + name`
  string.

**Done when:** running a destructive command through chat never silently
overwrites the participant's original image, and the assistant can say
exactly what changed.

**Related:** [#24](https://github.com/fiji/fiji-llm/issues/24) (run-script
tool wants the same kind of structured result, for scripts instead of
commands) and [#32](https://github.com/fiji/fiji-llm/issues/32) (general tool
contract for approval/limits) — this issue is the concrete, Fiji-command
version of that pattern and doesn't need to wait for either.

## Issue 6: Turn on the macro recorder tool and use it as ground truth

**Title:** Register `ImageJMacroTool` and expose recorded macro lines as context

**Why:** Workshop section 5 explicitly compares generated code with Macro
Recorder output — that only works if the assistant can see the recorder.

**What to actually do:**
- Uncomment `@Plugin(type = AiToolPlugin.class)` on `ImageJMacroTool`
  ([ImageJMacroTool.java](../src/main/java/sc/fiji/llm/macro/ImageJMacroTool.java)).
- Expose the currently recorded macro lines as something the assistant can
  read, so "turn what I just did into a script" is grounded in the
  recorder's actual function calls, not the model's memory of macro syntax.

**Done when:** after performing a manual sequence with the recorder on, asking
the assistant to write a macro produces code matching the recorder's real
function names.

**Related:** none of #30-#40.

## Issue 7: Flag generated macro code that uses function names that don't exist

**Title:** Check generated macro code against `MacroFunctionRegistry`

**Why:** Workshop section 5's "review this macro for commands or syntax that
may not work" prompt currently has nothing to check against but the model's
own judgment.

**What to actually do:**
- Before (or on request after) showing generated macro code, check each
  called function name against `MacroFunctionRegistry`
  ([MacroFunctionRegistry.java](../src/main/java/sc/fiji/llm/macro/MacroFunctionRegistry.java)),
  which already lists real ImageJ macro functions but is never called today.
- This is a name lookup, not a full parser. Missing a valid function is fine;
  silently accepting an invented one is the failure mode to avoid.

**Done when:** if the model invents a function name, the assistant flags it
instead of presenting it as correct.

**Related:** [#2](https://github.com/fiji/fiji-llm/issues/2) (broader "rules
for Fiji libraries" effort) and [#24](https://github.com/fiji/fiji-llm/issues/24)
(actually running the script would also catch this, but that's slower and
destructive) — complementary, not blocking.

## Issue 8: Show which facts came from Fiji vs. the model's own guess

**Title:** Distinguish observed facts, participant input, and model inference
in chat

**Why:** this is the workshop's stated conceptual center — "what do you know
about my Fiji state, and how do you know it?" — and several closing-evaluation
questions depend on participants being able to tell the difference.

**What to actually do:**
- When the assistant answers, distinguish facts it read via a tool/context
  item, information the participant typed, and its own inference/guess.
- At minimum, render tool calls as visibly distinct entries in the chat
  transcript. Today `ChatMessagePanel`'s `MessageType` enum only has `USER`,
  `ASSISTANT`, `SYSTEM`, `ERROR` — tool calls currently show up as ordinary
  assistant text.

**Done when:** a participant can point at a chat response and say which parts
were read from Fiji versus assumed.

**Related:** [#31](https://github.com/fiji/fiji-llm/issues/31) (structured
run/tool events) would make this easier to build on, but a basic version
doesn't require it.

## Issue 9: Add a sanitized problem-report generator

**Title:** Add a tool to generate a sanitized, forum-ready problem report

**Why:** once participants leave the workshop and hit issues on their own,
they need an easy way to report a problem without leaking secrets.

**What to actually do:**
- Add a tool/command that assembles Fiji/OS/Java version, the active provider
  and model name, and the last error, and explicitly strips API keys and
  local file paths before presenting it.
- Present it as text the user copies into a forum post or GitHub issue; don't
  send it anywhere automatically.

**Done when:** running it on a real error never includes an API key, and a
maintainer could act on the report without asking "what were you running?"

**Related:** none of #30-#40.
