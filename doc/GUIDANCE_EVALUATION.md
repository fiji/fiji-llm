# Guidance Evaluation

This is a human-facing evaluation companion to the live checks in
[INTEGRATION_TESTS.md](INTEGRATION_TESTS.md). It evaluates whether an agent
uses the shared Fiji guidance effectively while completing representative tasks.

Do not provide this document to the agent under test. The prompts below are
intentionally neutral: they do not name the guidance tools or guide IDs. The
agent's tool trace, not its final self-report, is the authoritative record of
what it actually did.

## Evaluation Setup

For each prompt:

- Start a fresh agent conversation so previous tool calls and retrieved guides
  do not affect the result.
- Start Fiji with the MCP server available.
- Open Fiji's built-in **Blobs** sample image and close unrelated images when
  practical.
- Clear or reset the Results Table before an analysis run.
- Record the Fiji version, model/provider, model name, date, and any relevant
  runtime configuration.
- Capture the complete tool trace, including tool names, arguments, results,
  and errors. The trace should include both guidance and operational tools.
- Ask the agent to end its response with the exact Fiji tool names it used, in
  call order. Compare that list with the trace; the trace is authoritative.

Do not tell the agent that guidance usage is being evaluated. Do not require
exact wording, an exact segmentation threshold, an exact object count, or one
specific language. Fiji versions, image state, model behavior, and available
plugins can legitimately change those details.

## Shared Checks

Apply these checks to every run:

- `fiji_guide_onboarding` is called before the first non-guidance Fiji tool.
- The agent retrieves guidance relevant to the task before taking the related
  action. Acceptable related guides depend on the task; the evaluator should
  judge relevance rather than require a single rigid sequence.
- Tool calls use live Fiji state instead of assuming that an image, command,
  editor, or Results Table exists.
- The final response reports the actual result and limitations rather than
  claiming success from an unverified tool call.
- The reported tool order agrees with the captured trace, apart from harmless
  formatting differences.

A guidance read is useful only when it changes or supports the workflow. A
model that reads onboarding and then ignores relevant scripting, data, or
workflow guidance should receive partial credit rather than full credit.

## Prompt 1: Analyze the Current Image

```text
With the Blobs sample image open, use Fiji to perform a sensible
object-segmentation and measurement analysis. Establish which image is active,
choose and apply an appropriate Fiji-native workflow, and leave the resulting
mask or ROIs and Results Table available for inspection. Explain the important
choices and limitations. Report the result Fiji actually produced rather than
assuming a particular object count.

At the end, list the exact Fiji tool names you used, in order.
```

### Expected evidence

- The trace begins with onboarding guidance before image or command tools.
- The agent reads guidance relevant to running commands, image/data handling,
  and measurements or Results Table output as needed.
- The agent inspects the active image and uses a plausible segmentation and
  measurement workflow rather than inventing a result.
- The resulting image state, ROIs, and/or Results Table are observable through
  Fiji tools.
- The final answer reports actual measurements and any uncertainty.

Do not fail the run because the agent chooses a different valid thresholding
or particle-analysis route. Failures should be based on unsupported claims,
missing validation, or a workflow that is not Fiji-aware.

## Prompt 2: Make the Analysis Reproducible

```text
Create a reproducible Fiji workflow for segmenting and measuring objects in the
current image. Choose an appropriate workflow representation supported by this
Fiji installation. Use Fiji's normal Script Editor or Macro Recorder workflow
to create the artifact, preserve the important parameters and input
assumptions, and run or otherwise validate it against the current image.

Do not merely paste an untested script into the response. Explain where the
workflow artifact exists, how another Fiji user can rerun it, and what result
was observed.

At the end, list the exact Fiji tool names you used, in order.
```

### Expected evidence

- The agent reads workflow and scripting or macro guidance before creating an
  artifact, and chooses a language or macro path based on Fiji's capabilities
  rather than assuming a generic Python environment.
- The artifact is created through Fiji's normal editor or recorder tools and is
  available for inspection.
- The artifact preserves the analysis parameters, input assumptions, and
  expected outputs.
- The artifact is run or validated through Fiji's supported execution path.
- The final answer distinguishes an artifact that was created from one that was
  actually executed successfully.

The prompt intentionally does not request Jython, Python, Groovy, or a macro.
The selected path is part of what is being evaluated.

## Prompt 3: Inspect Available Operations (Optional)

```text
Report which operations are currently available in this Fiji installation for
segmenting and measuring the open image. Inspect the live application and
available commands as needed, distinguish installed capabilities from general
knowledge, and recommend a small next workflow. Do not modify the image or
Results Table unless that is necessary to support the report.

At the end, list the exact Fiji tool names you used, in order.
```

### Expected evidence

- The agent reads onboarding and any relevant application or command guidance
  before inspecting the installation.
- The report is based on live command or application information where
  possible, not only on generic Fiji knowledge.
- The agent distinguishes an available command from a merely suggested command
  or an external Python package.
- The recommendation is compatible with the current image and available
  operations.

This prompt is optional. It is useful for testing capability discovery, but it
should not block the core image-analysis and reproducibility evaluation.

## Post-Run Questionnaire

Complete one copy for each prompt and attach the tool trace and any generated
workflow artifact.

| Field | Response |
|---|---|
| Date and evaluator | |
| Fiji version | |
| MCP client | |
| Model/provider and model name | |
| Prompt number | |
| First non-guidance Fiji tool | |
| Was `fiji_guide_onboarding` called first? | Yes / No |
| Guide IDs read | |
| Were the relevant guides read before action? | Yes / Partial / No |
| Did the reported tool order match the trace? | Yes / Partial / No |
| Was the requested Fiji state or artifact actually produced? | Yes / Partial / No |
| Was the result validated in Fiji? | Yes / Partial / No |
| Main failure or limitation | |

### Score

Use 0-2 for each category. A score of 0 means absent or incorrect, 1 means
partial or weak, and 2 means clear and demonstrated.

| Category | Score |
|---|---:|
| Guidance preflight and relevance | |
| Fiji-aware workflow choices | |
| Artifact or application-state correctness | |
| Validation and reporting | |
| Tool-order report accuracy | |
| **Total / 10** | |

Do not score exact model wording or require a particular tool sequence beyond
the onboarding precondition and the task's observable requirements. Record
model-dependent behavior as an observation so the same prompt can be rerun with
another model or Fiji configuration.
