---
name: fiji-macro-recording
description: 'Create, inspect, transfer, edit, run, and verify ImageJ macros through fiji-mcp. Use for macro recorder workflows, recording Fiji commands, creating .ijm scripts, macro dialogs, and macro execution diagnostics.'
argument-hint: 'Describe the macro workflow, commands to record, or macro behavior to verify'
user-invocable: true
disable-model-invocation: false
---

# Fiji Macro Recording

Use this skill for a complete macro-recording workflow through Fiji-MCP.

## Workflow

1. Inspect the current recorder and script state with `fiji_macro_recorder_state` and `fiji_script_list`. Preserve existing recorder contents and scripts; do not overwrite user content unless asked.
2. Start the recorder with `fiji_macro_start_recorder`, or bring the existing recorder to the front. While the recorder is open, each recordable command produces a parameterized macro invocation.
3. Run the requested commands. Commands may be executed by the user or through `fiji_command_*` tools. Only commands supported by the ImageJ recorder produce macro invocations; verify the buffer rather than assuming every command was recorded.
4. Read the recorder buffer with `fiji_macro_recorder_state` and check that the expected invocations were captured.
5. Transfer the recorded macro to the Script Editor with `fiji_macro_create_script` while the recorder is still open. Do not close the recorder first; this tool requires the recorder window and its Create button.
6. Close the recorder with `fiji_macro_close_recorder` after the script has been created. Use `fiji_macro_recorder_state` to confirm the recorder state when needed.
7. Use the `fiji_script_*` tools to inspect or edit the created `.ijm` script. Keep the macro extension so that Fiji identifies it as an ImageJ macro.
8. Run the active macro with `fiji_macro_run`. If execution pauses on a modal dialog, inspect it with `fiji_ui_dialogs_read`, respond with `fiji_ui_dialog_respond` using the exact title and button text, and poll with `fiji_macro_run_status` until the run reaches a terminal state.
9. Verify the result using an observable Fiji state appropriate to the macro, such as an image, ROI, Results Table, dialog outcome, recorder buffer, or script content. Do not claim success from a started or transferred response alone.

## Diagnostic Guidance

- Use `fiji_macro_run_status` with the returned `run_id` for an asynchronous macro run.
- Treat `success`, `finished_with_errors`, `timed_out`, and `infrastructure_error` as distinct outcomes.
- A modal `showMessage` dialog is an interaction point, not by itself a macro error. Genuine macro-error dialogs and structured error output should be reported as errors.
- Timeouts are requested but not guaranteed. If the result reports failed termination, inspect the script and logs before retrying.
- Prefer the per-run output, errors, console streams, and ImageJ/SciJava logs in the execution result for diagnosing that run. `fiji_script_read_logs` returns cumulative logs retained by the active Script Editor across runs.
- If a recorder, script editor, macro engine, or Fiji-MCP operation is unavailable, report the infrastructure limitation instead of repeatedly changing the macro.

## Reporting

Summarize the recorded commands, the created script identifier and name, any edits, the execution status, dialogs handled, diagnostics, and the observable verification result. Distinguish clearly between:

- macro recorded and transferred successfully;
- macro executed successfully and verified;
- macro execution completed with actionable errors;
- macro execution timed out or could not be terminated; and
- Fiji-MCP or Fiji infrastructure failure.
