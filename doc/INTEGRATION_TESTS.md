# Script and Macro Integration Tests

This is the repeatable live regression checklist for script and ImageJ macro
execution through Fiji-LLM. These are integration tests: run them against a
live Fiji instance with the Script Editor and MCP tools available.

## Test Setup

- Start Fiji with the Fiji-LLM plugin installed and the MCP server available.
- Open the Fiji Script Editor through `fiji_script_open_editor`.
- Create or open one test script, then use `fiji_script_activate` before each
  run as needed.
- Run non-`.ijm` code through `fiji_script_run`; run `.ijm` macros through
      `fiji_macro_run`. Never bypass the Script Editor with a direct ImageJ macro
      execution path.
- Before a diagnostic case, note or clear the current ImageJ Log and SciJava
  log state so that new output can be distinguished from older output.
- When a case may show a blocking dialog, inspect it with
  `fiji_ui_dialogs_read` before taking any manual action.
- `fiji_command_run` returns an `environment` object with before/after metadata
      and a `changes` object; verify those fields for command cases that open,
      close, or modify images or Results table metadata.

## Tool Sequence and Common Checks

Keep these tool calls explicit so that a regression run can be repeated in the
same order:

1. `fiji_script_list` reports the expected editor, tab, and active script.
2. `fiji_script_read_content` returns the code that is about to run.
3. `fiji_script_run` returns the expected `output`, `errors`, and
      `completion_state` fields. A dialog-paused run returns `blocked_by_dialog`
      and a `run_id`; poll it with `fiji_script_run_status` after responding to
      the dialog with `fiji_ui_dialog_respond`.
      Console writes are also returned as `console_stdout` and
      `console_stderr`; stderr is included in `errors`.
4. Output from an earlier run is not repeated in the next run's delta.
5. `fiji_log_imagej_read` exposes ImageJ macro `print()` output when
      applicable.
6. SciJava messages are observable by starting a capture with
      `fiji_log_scijava_start_capture` before the operation and stopping it with
      `fiji_log_scijava_stop_capture` afterward.
7. `fiji_ui_dialogs_read` reports visible error or confirmation dialogs,
      including their titles, messages, buttons, and modal state.
8. When a test intentionally proceeds through a dialog, call
      `fiji_ui_dialog_respond` with the exact observed title and button, then
      call `fiji_ui_dialogs_read` again to verify the resulting state.

## Scripts

Run these cases with at least one supported Script Editor language used by the
project, such as Python. Repeat the language-independent cases for another
available language when practical.

For each script case, repeat this tool sequence:

1. Call `fiji_script_open_editor` if no Script Editor is open.
2. Call `fiji_script_create` or open the test script, then use
      `fiji_script_rename` to give it the language-specific extension and
      `fiji_script_activate` to select it.
3. Call `fiji_script_list` and `fiji_script_read_content` to verify the active
      editor tab and the exact source under test.
4. For cases that should produce SciJava diagnostics, start a capture with
      `fiji_log_scijava_start_capture`.
5. Call `fiji_script_run` and check `output`, `errors`, and
      `completion_state`. If a parameter or other modal dialog appears,
      inspect it with `fiji_ui_dialogs_read`, respond with
      `fiji_ui_dialog_respond`, and poll with `fiji_script_run_status`. Do not
      use it for `.ijm` tabs; it must direct the caller to `fiji_macro_run`.
6. Read the final SciJava messages with
      `fiji_log_scijava_stop_capture`, and inspect ImageJ Log or visible dialogs
      when the case is expected to produce them.

Keep the execution path through the Script Editor for every case, including
syntax failures, runtime failures, and timeouts.

- [ ] Successful image-processing script that creates or modifies an image.
- [ ] Malformed syntax: verify a non-success result and useful error text.
- [ ] Runtime exception after partial image processing: verify that output or
      images produced before the exception remain observable and the error is
      reported.
- [ ] Console exception: write an exception to the language console or stderr
      and verify that the diagnostic is captured in `errors` and
      `console_stderr` or another relevant log.
- [ ] Timeout: run a script longer than 30 seconds and verify
      `completion_state: "timed_out"`, a true `timeout_requested`, the actual
      `execution_terminated`/`termination_status`, and any `termination_failure`
      from the Script Editor; if termination fails, the recommended action must
      mention that manual inspection is required.

## ImageJ Macros

Start each macro regression run by deriving the test macro from recorded Fiji
commands:

1. Call `fiji_macro_start_recorder` and verify the recorder state with
      `fiji_macro_recorder_state`, including an empty buffer when no commands
      have been recorded yet.
2. Run a small image-processing workflow through Fiji, using the normal Fiji
      UI or `fiji_command_search` and `fiji_command_run`. Include commands that
      create or modify an image so the recorded macro has a useful baseline.
      Call `fiji_macro_recorder_state` again and verify the recorded buffer has
            changed.
3. Call `fiji_macro_create_script` to transfer the recorder contents into an
      `.ijm` tab in the Script Editor. Verify the new tab with
      `fiji_script_list` and inspect its source with `fiji_script_read_content`.
4. Stop the recorder with `fiji_macro_close_recorder`.
5. Use the transferred macro as the successful baseline, then edit copies of
      it to create the error, dialog, and timeout cases below. Run every case with
      `fiji_macro_run`; do not bypass the Script Editor with direct ImageJ macro
      execution.

This workflow keeps the recorded command syntax grounded in the Fiji instance
under test while allowing the Script Editor, ImageJ Log, and UI dialogs to be
inspected together.

- [ ] Successful image-processing macro with `print()`.
- [ ] A dialog-paused macro returns `blocked_by_dialog`, a `run_id`, and dialog
      details. `fiji_macro_run_status` reports the same paused run without
      clicking; after `fiji_ui_dialog_respond`, status eventually becomes a
      completed result.
- [ ] No-image failure: run an image-dependent command without an open image
      and verify the error and any blocking dialog.
- [ ] Malformed macro syntax: verify the actual macro error rather than only a
      generic Script Editor failure.
- [ ] Macro error after partial image processing: verify that the partial
      image state and the later error are both observable.
- [ ] Command requiring `Close All` confirmation: verify the confirmation
      dialog's title, message, buttons, and modal state with
      `fiji_ui_dialogs_read`; when proceeding is intentional, respond with
      `fiji_ui_dialog_respond` using the exact title and button, then verify
      that the dialog state changed.
- [ ] Console exception: trigger a macro exception or error after `print()`
      output and compare the Script Editor, ImageJ Log, and SciJava results.
- [ ] Timeout: run a macro longer than 30 seconds and verify interruption,
      `timeout_requested`, the corresponding `execution_terminated`/
      `termination_status`, and any `termination_failure` surfaced by the
      timed-out result.

## Commands

- [ ] Use `fiji_command_search` to find a known command and run it with
      `fiji_command_run`; verify `status: "success"`, command metadata, and
      the `environment` before/after report.
- [ ] Run a command that opens an image and verify `changes.images_opened` and
      the active-image metadata.
- [ ] Run a command that changes or closes an image and verify
      `changes.images_changed` or `changes.images_closed`.
- [ ] Run a measurement command and verify the Results table row/heading
      metadata and `changes.results_table_changed`.
- [ ] Run a command that emits ImageJ or SciJava log output and verify the
      corresponding log delta in `environment`.

## ImageJ Data Objects

These read-only tools inspect common ImageJ data objects directly. Use a live
Fiji instance with the ImageJ legacy layer active.

- [ ] Results Table: create at least one measurement row, call
      `fiji_results_read`, and verify `present`, `row_count`,
      `column_count`, `columns`, and `rows`. Confirm the returned row values
      and headings match the visible Results Table.
- [ ] Results Table empty state: clear or reset the table, call
      `fiji_results_read`, and verify the returned row and column arrays
      reflect the cleared state.
- [ ] ROI Manager: open the ROI Manager, add at least two named ROIs, and call
      `fiji_rois_read`. Verify `present`, `count`, and each ROI's
      `index`, `name`, `selected`, and `type` fields.
- [ ] ROI Manager unavailable state: close the ROI Manager, call
      `fiji_rois_read`, and verify it reports `present: false` without
      throwing an error.

## System Information

- [ ] Call `fiji_system_read` and verify it returns ImageJ 1.x and application
      versions, the Java version, JVM memory values in bytes, and operating
      system name, version, and architecture. Verify `active_update_sites`
      contains the enabled sites with their names and URLs.
- [ ] Call `fiji_system_list_update_sites` and verify every returned entry contains
      `active`, `name`, and `url`, including both active and inactive sites when
      the update-site configuration contains both.