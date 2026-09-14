---
name: fiji-script-debugging
description: 'Create, run, diagnose, repair, and verify Fiji scripts through fiji-mcp. Use for script syntax errors, runtime errors, missing scripting engines, asynchronous execution, and iterative script debugging.'
argument-hint: 'Describe the Fiji script, language, intended behavior, and any error to investigate'
user-invocable: true
disable-model-invocation: false
---

# Fiji Script Debugging

Use this skill for a complete edit-run-diagnose-repair-verify loop for scripts executed through Fiji-MCP.

## Workflow

1. Inspect the current Fiji script state with `fiji_script_list`.
2. Preserve existing user scripts. Create a new editor or script when appropriate; do not overwrite an existing script unless the user asks for it.
3. Choose the script language from the requested language and filename extension. If the language is uncertain, use a minimal probe and report whether the engine appears available.
4. Write or replace the script content with `fiji_script_replace_content`.
5. Run the script with `fiji_script_run`.
6. Treat a result that says only that execution started as scheduling confirmation, not proof of success. Read diagnostics after the run has had time to finish. If the first read is ambiguous, read again once.
7. Read errors with `fiji_script_read_errors` and classify the result:
   - Syntax or compilation failure: parser, compiler, expected-token, or source-location messages.
   - Runtime failure: an exception raised after parsing, usually with a script path and line number.
   - Infrastructure failure: missing engine, class-loading failure, uninitialized Fiji service, or an MCP/tool error.
   - No reported error: still require an observable success check before claiming completion.
8. If `fiji_script_read_errors` returns a large-response spill path, use `read/readFile` with the exact path from the tool result. Inspect the JSON payload and the contained `errors` text. Do not use `file_search` or directory discovery unless the returned path is missing or invalid.
9. Repair only after identifying a concrete failure. Use the smallest script edit that addresses the diagnostic, then rerun and reread diagnostics.
10. Verify success using an observable Fiji result appropriate to the script, such as a generated image, changed image state, measurement result, saved output, or other Fiji-MCP query. Do not claim success from `started_script` alone.
11. Stop after a small number of focused repair attempts if the same infrastructure failure persists. Report the blocker rather than repeatedly changing valid script code.

## Diagnostic Guidance

- Keep the script body minimal when testing the scripting engine so engine failures are distinguishable from script failures.
- Error output may be cumulative across runs or scripts. Identify the current run by its script name and start timestamp, and do not attribute older entries to the current attempt.
- Normal `print` or console output may not appear in `fiji_script_read_errors`; absence from that output is not by itself a runtime failure.
- A useful runtime diagnostic should include the exception message and, when available, the script path and source location.
- If a diagnostic is truncated by the file reader, report the visible portion and avoid inventing omitted details.

## Reporting

Summarize the final script name, language, actions taken, diagnostic classification, repair attempts, and verification result. Distinguish clearly between:

- confirmed success,
- execution started but not verified,
- script failure with actionable diagnostics, and
- Fiji-MCP or Fiji infrastructure failure.
