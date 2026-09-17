---
name: fiji-script-workflow
description: 'Create, inspect, edit, run, diagnose, repair, and verify Fiji scripts through fiji-mcp. Use for script authoring, execution, dialogs, runtime errors, missing scripting engines, asynchronous execution, and iterative repair.'
argument-hint: 'Describe the Fiji script, language, intended behavior, and any error to investigate'
user-invocable: true
disable-model-invocation: false
---

# Fiji Script Workflow

Use this skill for a complete edit-run-diagnose-repair-verify loop for scripts executed through Fiji-MCP.

## Workflow

1. Inspect the current Fiji script state with `fiji_script_list`.
2. Preserve existing user scripts. Create a new editor or script when appropriate; do not overwrite an existing script unless the user asks for it.
3. Choose the script language from the requested language and filename extension. If the language is uncertain, use a minimal probe and report whether the engine appears available.
4. Write or replace the script content with `fiji_script_replace_content`.
5. Run the script with `fiji_script_run`.
6. Treat `fiji_script_run` as returning after the run completes or pauses on a new modal dialog. If it returns `blocked_by_dialog`, inspect the dialog with `fiji_ui_dialogs_read`, respond with `fiji_ui_dialog_respond` using the exact title and button text, and poll with `fiji_script_run_status` using the returned `run_id` until the run reaches a terminal state. Still verify an observable Fiji result rather than relying on the tool response alone.
7. Read script logs with `fiji_script_read_logs` and classify the `errors` field:
   - Syntax or compilation failure: parser, compiler, expected-token, or source-location messages.
   - Runtime failure: an exception raised after parsing, usually with a script path and line number.
   - Infrastructure failure: missing engine, class-loading failure, uninitialized Fiji service, or an MCP/tool error.
   - No reported error: still require an observable success check before claiming completion.
8. If `fiji_script_read_logs` returns a large-response spill path, use `read/readFile` with the exact path from the tool result. Inspect the JSON payload and the contained `errors` and `output` fields. Do not use `file_search` or directory discovery unless the returned path is missing or invalid.
9. Repair only after identifying a concrete failure. Use the smallest script edit that addresses the diagnostic, then rerun and reread diagnostics.
10. Verify success using an observable Fiji result appropriate to the script, such as a generated image, changed image state, measurement result, saved output, or other Fiji-MCP query. Do not claim success from `started_script` alone.
11. Stop after a small number of focused repair attempts if the same infrastructure failure persists. Report the blocker rather than repeatedly changing valid script code.

## Diagnostic Guidance

- Keep the script body minimal when testing the scripting engine so engine failures are distinguishable from script failures.
- Error output may be cumulative across runs or scripts. Identify the current run by its script name and start timestamp, and do not attribute older entries to the current attempt.
- Normal `print` or console output may appear in `output` rather than `errors`; absence from `errors` is not by itself a runtime failure.
- Parameter dialogs and other modal dialogs can pause a script. Treat `blocked_by_dialog` as an interaction point, not a completed run.
- A useful runtime diagnostic should include the exception message and, when available, the script path and source location.
- If a diagnostic is truncated by the file reader, report the visible portion and avoid inventing omitted details.

## Reporting

Summarize the final script name, language, actions taken, diagnostic classification, repair attempts, and verification result. Distinguish clearly between:

- confirmed success,
- execution completed but not verified,
- script failure with actionable diagnostics, and
- Fiji-MCP or Fiji infrastructure failure.
