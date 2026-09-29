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
- `fiji_command_run` returns an optional `environment_impact` object with
      changes-only metadata and a nested `changes` object; verify those fields for command cases that open,
      close, or modify images or Results table metadata.

## Integrated Chat Window (Out of Scope for Fiji-MCP Testing)

This section applies only to the integrated Fiji chat window, not to testing
the exposed `fiji-mcp` tool surface. Chat attachment actions and their context
suppliers are in-process Fiji UI functionality; they are not exposed as MCP
tools. Skip this section when evaluating `fiji-mcp` tool availability or
MCP-only workflows, and do not treat the absence of attachment tools as a
failure. Run these checks separately when testing the integrated chat window.

Manually test the Fiji chat window's attachment workflow with at least one item
from each currently supported context category:

- Script: attach an open script, including a selected range when available, and
      ask the model to identify or summarize the attached code.
- Image: attach a visible image without overlays and ask the model to describe
      the attached image.
- Annotated image: add a visible ROI or overlay, attach the annotated image, and
      ask the model to account for the annotation.

For each category, verify that the attachment appears in the chat window, the
model receives the attached content, and the outgoing request contains the
matching item under `USER-ATTACHED CONTEXT (JSON)`. Verify that the request also
contains the automatically attached `FIJI SESSION SNAPSHOT` when live scripts or
images are available, including the expected `fiji_script_list` and/or
`fiji_image_list` data and active-item state. Distinguish this automatic
snapshot from the items explicitly attached through the chat window.

When a script is attached, verify that the recommendations contain the
`scripting` guide and the `fiji_script_*` tool-family prefix. When an image or
annotated image is attached, verify that they contain the `image-types` guide
and the `fiji_image_*` prefix. With both a script and an image attached, verify
that one recommendation block contains both guide IDs and both tool-family
prefixes, without an exhaustive listing of individual tools. Verify that an
ordinary message, or a message after removing all attached items, does not add
the context-specific recommendation block. The automatic session snapshot may
still be present when live scripts or images are available.

Removing an attachment before sending must prevent its user-attached JSON,
binary content, and context-specific recommendations from being included. When
practical, repeat the test with multiple attachments and with a new
conversation.

- [ ] Start a new conversation and verify that its UUID is stable while its
      display label initially falls back to that UUID.
- [ ] Send the first message and verify that the outgoing request contains one
      `conversation_recommendation` with `tool: "fiji_conversation_name"` and
      the new conversation's `conversation_id`.
- [ ] Verify that the assistant can name the conversation from its history, the
      result contains the same conversation ID and a timestamped display name,
      and the chat selector updates without reopening the conversation.
- [ ] Verify that the name persists after restarting or reloading the
      conversation, that later messages do not repeat the naming recommendation,
      and that a second naming attempt returns an error.

## Cross-Cutting Evaluation Criteria

Apply these checks while reviewing tool specifications and observing live tool
calls. Record a failure when a tool's description, parameters, or result makes
the workflow ambiguous even if the underlying operation succeeds.

- [ ] Tool names are clear, consistent, and describe the operation without
      requiring model-specific or implementation-specific knowledge.
- [ ] Every parameter has a meaningful name and description, with required or
      optional status, valid values, units, defaults, and index conventions
      stated where applicable.
- [ ] Tool descriptions identify relevant scope, side effects, prerequisites,
      and the expected result or state change.
- [ ] Results use structured fields for status, outputs, errors, warnings, and
      relevant state changes. A completed run with only known non-fatal
      diagnostics reports `status: "finished_with_warnings"` and does not put
      those diagnostics in `errors`. Optional empty data is omitted when it is
      not meaningful.
- [ ] Multi-stage workflows identify the next action when one is needed. Verify
      that `recommended_tools`, `recommended_tool`, or `guide_recommendations`
      names an available follow-up tool or guide, includes enough context to
      continue, and is omitted when no follow-up is needed.
- [ ] Paused, asynchronous, dialog-driven, and stateful workflows expose the
      identifiers and status fields needed to resume or inspect the operation.
- [ ] Errors explain what failed and provide a useful recovery path without
      claiming success or hiding relevant partial state.
- [ ] Destructive or broadly state-changing operations expose appropriate
      confirmation, scope, or impact metadata.
- [ ] Model-facing descriptions do not duplicate details already present in
      structured result JSON, and irrelevant logs, fields, and tool listings
      are not included merely for completeness.

## Guidance and Recommendations

- [ ] Call `fiji_guide_list` and verify each guide includes its ID, title,
      summary, topics when available, and authority.
- [ ] Call `fiji_guide_read` with a returned guide ID and verify that it returns
      the matching metadata and guide content. An unknown guide ID should
      return a useful error that recommends `fiji_guide_list`.
- [ ] For every `recommended_tool`, `recommended_tools`, or
      `guide_recommendations` value observed in another tool result, verify
      that the referenced tool or guide exists and is appropriate for the
      reported state. Verify that conditional recommendations are absent when
      the follow-up is not needed.

## Image Content

With at least two visible images open, use the MCP server as an external client
would:

1. Call `fiji_image_list` and note the active image and a different returned `id`.
2. Call `fiji_image_activate` with the different `image_id`.
3. Call `fiji_image_list` again and verify that the selected image is now active.
4. Call `fiji_image_details` with that `image_id` and verify the title, active
      state, pixel type, and dimension metadata.

5. Call `fiji_image_view` with that `image_id`.
6. Verify that the result contains a text render-metadata content block and an
      MCP `image` content block with MIME type `image/png` and non-empty base64
      data.
7. Call `fiji_image_preview` with the same `image_id`.
8. Verify that the preview result contains an MCP `image` content block and no
      render-metadata content block.
9. Add a visible ROI or image overlay, then call `fiji_image_view` again with
      the same `image_id`.
10. Verify that the view result's metadata includes `render_mode`,
      `roi_included`, and `overlay_included`, as well as the MCP `image` content
      block.
11. Verify that an unknown image id returns a useful text error instead of an
      image block or a server failure.

## Tool Sequence and Common Checks

Keep these tool calls explicit so that a regression run can be repeated in the
same order:

1. `fiji_script_list` reports the expected editor, tab, and active script.
2. `fiji_script_read_content` returns the code that is about to run.
3. `fiji_script_run` returns `status` and optional non-empty `output` and
      `errors` fields. A dialog-paused run returns `blocked_by_dialog` and a
      `run_id`; poll it with `fiji_script_run_status` after responding with
      `fiji_ui_dialog_respond` or closing with `fiji_ui_dialog_close`.
      Console writes are returned as optional
      `environment_impact.console_stdout` and
      `environment_impact.console_stderr` fields; stderr is also included in
      `errors`.
4. Output from an earlier run is not repeated in the next run's delta.
5. `fiji_log_imagej_read` exposes ImageJ macro `print()` output when
      applicable.
6. SciJava messages are observable by starting a capture with
      `fiji_log_scijava_start_capture` before the operation, reading the active
      capture with `fiji_log_scijava_read`, and stopping it with
      `fiji_log_scijava_stop_capture` afterward.
7. `fiji_ui_dialogs_read` reports visible error or confirmation dialogs,
      including their titles, messages, buttons, and modal state.
8. When a test intentionally proceeds through a dialog, call
      `fiji_ui_dialog_respond` with the exact observed title and button, or
      `fiji_ui_dialog_close` with the exact title, then call
      `fiji_ui_dialogs_read` again to verify the resulting state.

- [ ] Dialog without a suitable button: close it with
      `fiji_ui_dialog_close` and verify the reported visibility.

## UI Inspection and Screenshots

With Fiji showing at least one ordinary AWT or Swing window:

1. Call `fiji_ui_windows_read` and verify visible windows include their title,
      class, type, bounds, active state, and modality metadata.
2. Call `fiji_ui_controls_read` with one exact window title and verify that
      supported labels, buttons, text fields, checkboxes, choices, combo boxes, and
      menu entries include roles and state metadata. Supply the exact window
      class as well when titles are duplicated.
3. Call `fiji_ui_screenshot` and verify the result contains JSON metadata plus
      an MCP `image` content block with PNG data. Screenshots request focus
      automatically only when the target is not already active; verify the
      `focus_requested` and `focus_restored` metadata. Focus activation and
      restoration are best effort and do not guarantee an unobstructed capture.
4. Use a returned showing component path to capture a component crop, and
      verify that a hidden or ambiguous target returns a text error rather than an
      image block.

## Scripts

Run these cases with at least one supported Script Editor language used by the
project, such as Python. Repeat the language-independent cases for another
available language when practical.

- [ ] Call `fiji_script_list_languages` and verify the language and extension
      used for the test are available before renaming the script.
- [ ] Call `fiji_script_read_lines` on a known range and verify that the
      response reports the requested 1-indexed inclusive bounds and content.
      Verify that an invalid or out-of-range range returns a useful error.
- [ ] After a run, call `fiji_script_read_logs` and verify that retained output
      and errors are reported without internal start banners.
- [ ] On a disposable script, use `fiji_script_insert_content`,
      `fiji_script_delete_lines`, and `fiji_script_replace_content`, reading
      the content after each operation to verify the requested source change
      and reported line counts.

For each script case, repeat this tool sequence:

1. Call `fiji_script_open_editor` if no Script Editor is open.
2. Call `fiji_script_create` or open the test script, then use
      `fiji_script_rename` to give it the language-specific extension and
      `fiji_script_activate` to select it.
3. Call `fiji_script_list` and `fiji_script_read_content` to verify the active
      editor tab and the exact source under test.
4. For cases that should produce SciJava diagnostics, start a capture with
      `fiji_log_scijava_start_capture`.
5. Call `fiji_script_run` and check `status` plus optional `output`, `errors`,
      and `warnings`. A completed run with only known non-fatal diagnostics
      reports `status: "finished_with_warnings"`; actionable failures report
      `status: "finished_with_errors"`. If a parameter or other modal dialog appears,
      inspect it with `fiji_ui_dialogs_read`, respond with
      `fiji_ui_dialog_respond` or close with `fiji_ui_dialog_close`, and poll
      with `fiji_script_run_status`. Do not
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
- [ ] Long-running script: run a script longer than 30 seconds and verify the
      initial result reports `status: "running"`, `wait_expired: true`,
      `duration_ms`, and a `run_id`; verify that wait expiry
      does not populate `errors` or cancel the Script Editor task. Poll the run
      until it reaches a terminal result.

## ImageJ Macros

Start each macro regression run by deriving the test macro from recorded Fiji
commands:

Before recording, call `fiji_macro_list_categories`, select a returned
category, and call `fiji_macro_list_functions` with that category. Verify the
function names and descriptions, and verify that an empty or unknown category
returns a useful error.

1. Call `fiji_macro_start_recorder` and verify the recorder state with
      `fiji_macro_recorder_state`, including an empty buffer when no commands
      have been recorded yet and `script_mode: false` for Macro (IJM) mode.
      When the recorder was previously set to another language, verify that an
      empty recorder is switched to Macro mode and that the prior language is
      restored after transfer or recorder close.
2. Run a small image-processing workflow through Fiji, using the normal Fiji
      UI or `fiji_command_search` and `fiji_command_run`. Include commands that
      create or modify an image so the recorded macro has a useful baseline.
      Call `fiji_macro_recorder_state` again and verify the recorded buffer has
            changed.
3. Call `fiji_macro_create_script` to transfer the recorder contents into an
      `.ijm` tab in the Script Editor. Verify the result reports
      `script_language: "ijm"` and `recorder_mode_restored: true`, then verify
      the new tab with `fiji_script_list` and inspect its source with
      `fiji_script_read_content`. Also verify that a non-empty recorder buffer
      recorded in another language returns a useful conversion error instead of
      creating a misleading `.ijm` tab.
4. Stop the recorder with `fiji_macro_close_recorder`.
5. Use the transferred macro as the successful baseline, then edit copies of
      it to create the error, dialog, and timeout cases below. Run every case with
      `fiji_macro_run`; verify optional `warnings` separately from `errors`.
      A completed run with only known non-fatal diagnostics reports
      `status: "finished_with_warnings"`; actionable failures report
      `status: "finished_with_errors"`. Do not bypass the Script Editor with
      direct ImageJ macro execution.

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
- [ ] Command invoking `Close All`: when possible, first create a disposable
      dirty or unsaved image to exercise the confirmation path. If Fiji shows a
      confirmation dialog, verify its title, message, buttons, and modal state
      with `fiji_ui_dialogs_read`; when proceeding is intentional, respond with
      `fiji_ui_dialog_respond` using the exact title and button, then verify
      that the dialog state changed. If there is no dirty or unsaved state, the
      command may proceed without a dialog; treat that as a valid result rather
      than a failed confirmation test.
- [ ] Console exception: trigger a macro exception or error after `print()`
      output and compare the Script Editor, ImageJ Log, and SciJava results.
- [ ] Long-running macro: run a macro longer than 30 seconds and verify the
      initial result reports `status: "running"`, `wait_expired: true`,
      `duration_ms`, and a `run_id`; verify that wait expiry
      does not populate `errors` or cancel the Script Editor task. Poll the run
      until it reaches a terminal result.

## Commands

- [ ] Use `fiji_command_search` to find a known command and run it with
      `fiji_command_run`; verify `status: "success"`, command metadata, and
      an optional changes-only `environment_impact` report.
- [ ] Run a command that opens an image and verify `changes.images_opened` and
      the active-image metadata.
- [ ] Run a command that changes or closes an image and verify
      `changes.images_changed` or `changes.images_closed`; for in-place pixel
      edits, verify final `changes.pixel_changes: "changed"` and captured
      `pixel_hash_status` values. For large or lazy images, verify sampled or
      skipped hashes produce `pixel_changes: "inconclusive"`.
- [ ] Run a measurement command and verify the Results table row/heading
      metadata in `changes.results_table`.
- [ ] Run a command that emits ImageJ or SciJava log output and verify the
      corresponding log delta in `environment_impact`.

## ImageJ Data Objects

These read-only tools inspect common ImageJ data objects directly. Use a live
Fiji instance with the ImageJ legacy layer active.

- [ ] Results Table: create at least one measurement row, call
      `fiji_results_read`, and verify `row_count`,
      `column_count`, `columns`, and `rows`. Confirm the returned row values
      and headings match the visible Results Table.
- [ ] Results Table empty state: clear or reset the table, call
      `fiji_results_read`, and verify it returns `{}`.
- [ ] ROI Manager: open the ROI Manager, add at least two named ROIs, and call
      `fiji_rois_read`. Verify `present`, `count`, and each ROI's
      `roi_index`, `name`, `selected`, `type`, and `bounds` fields. Verify each
      bounding box contains `x`, `y`, `width`, and `height`.
- [ ] ROI Manager details: call `fiji_rois_read_details` with a valid ROI
      index and verify the returned `shape`, `bounds`, and `coordinates`.
      Call it with an invalid index and verify that it returns a useful error.
- [ ] ROI Manager selection: call `fiji_rois_select` with a valid zero-based
      index and verify the selected `roi_index`, `selected`, and
      `applied_to_active_image` state. With an active image, verify the ROI is
      restored there and any reported slice change is expected.
- [ ] ROI Manager unavailable state: close the ROI Manager, call
      `fiji_rois_read`, and verify it reports `present: false` without empty
      `count` or `rois` fields.

## System Information

- [ ] Call `fiji_system_read` and verify it returns ImageJ 1.x and application
      versions, the Java version, JVM memory values in bytes, and operating
      system name, version, and architecture. Verify `active_update_sites`
      contains the enabled sites with their names and URLs.
- [ ] Call `fiji_system_list_update_sites` and verify every returned entry contains
      `active`, `name`, and `url`, including both active and inactive sites when
      the update-site configuration contains both.