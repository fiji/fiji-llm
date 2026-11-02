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

package sc.fiji.llm.ui;

import java.awt.Rectangle;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import sc.fiji.llm.image.ImageRenderingService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolRecommendationUtils;

/** AI tools for inspecting Fiji's visible UI. */
@Plugin(type = AiToolPlugin.class)
public class UiToolPlugin extends AbstractAiToolPlugin {

	private static final Gson GSON = new Gson();
	private static final long INTERACTION_MONITOR_INTERVAL_MS = 50;
	private static final int MAX_INTERACTION_MENU_ITEMS = 50;

	private final Map<String, InteractionRun> interactionRuns = new ConcurrentHashMap<>();

	@Parameter
	private ImageRenderingService imageRenderingService;

	public UiToolPlugin() {
		super(UiToolPlugin.class);
	}

	@Override
	public String getName() {
		return "UI Tools";
	}

	@Tool(value = { "List visible AWT and Swing windows, including titles, classes, bounds, active state, and modality" }, name = "fiji_ui_windows_read")
	public String readWindows() {
		try {
			final JsonArray windows = new JsonArray();
			for (final AWTDialogUtils.WindowInfo window : AWTDialogUtils
				.getVisibleWindows()) windows.add(windowJson(window));
			final JsonObject result = new JsonObject();
			if (windows.size() > 0) result.add("windows", windows);
			result.addProperty("count", windows.size());
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_ui_windows_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Inspect supported visible controls in one exact window." }, name = "fiji_ui_controls_read")
	public String readControls(@P(name = "window_title", value = "Exact window title from fiji_ui_windows_read; blank for the active window", required = false) final String windowTitle,
		@P(name = "window_class_name", value = "Exact window class name for the same window; use it to distinguish windows with the same title", required = false) final String windowClassName)
	{
		try {
			final JsonArray controls = new JsonArray();
			boolean hasInteractableControl = false;
			for (final AWTDialogUtils.ControlInfo control : AWTDialogUtils
				.getVisibleControls(windowTitle, windowClassName))
			{
				controls.add(controlJson(control));
				if (isInteractableControl(control)) hasInteractableControl = true;
			}
			final JsonObject result = new JsonObject();
			if (windowTitle != null && !windowTitle.isBlank()) result.addProperty("window_title",
				windowTitle);
			if (controls.size() > 0) result.add("controls", controls);
			result.addProperty("count", controls.size());
			if (hasInteractableControl) ToolRecommendationUtils.addToolRecommendations(result,
				"fiji_ui_control_interact");
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_ui_controls_read: " + e.getMessage());
		}
	}

	/**
	 * Reads top-level menu entries or one level of an already-open submenu.
	 *
	 * @param windowTitle exact title from {@code fiji_ui_windows_read}
	 * @param windowClassName optional class name to disambiguate windows with the same title
	 * @param parentPath submenu path from a previous {@code fiji_ui_menu_read} result; null or
	 *        blank reads the top-level menu bar
	 * @return JSON containing the immediate menu entries and their component paths
	 */
	@Tool(value = { "Read the top-level menus of one visible Swing window, or only the immediate entries of a submenu that is currently open. Omit parent_path for the menu bar; otherwise use a submenu path returned by this tool. Use fiji_ui_control_interact with action click to open a submenu, poll its run status to completion, then read the submenu entries." }, name = "fiji_ui_menu_read")
	public String readMenus(@P(name = "window_title", value = "Exact window title from fiji_ui_windows_read") final String windowTitle,
		@P(name = "window_class_name", value = "Exact window class name from fiji_ui_windows_read; use it to distinguish windows with the same title", required = false) final String windowClassName,
		@P(name = "parent_path", value = "Submenu component path returned by a previous fiji_ui_menu_read call; omit to read the top-level menus", required = false) final String parentPath)
	{
		if (windowTitle == null || windowTitle.isBlank()) {
			return jsonError("window_title cannot be null or blank");
		}

		try {
			final List<AWTDialogUtils.ControlInfo> menuItems = AWTDialogUtils
				.getVisibleMenuItems(windowTitle, windowClassName, parentPath);
			final JsonArray items = new JsonArray();
			boolean hasInteractableMenuItem = false;
			for (final AWTDialogUtils.ControlInfo menuItem : menuItems) {
				items.add(controlJson(menuItem));
				if (isInteractableControl(menuItem)) hasInteractableMenuItem = true;
			}
			final JsonObject result = new JsonObject();
			result.addProperty("window_title", windowTitle.trim());
			if (windowClassName != null && !windowClassName.isBlank()) result.addProperty(
				"window_class_name", windowClassName.trim());
			if (parentPath != null && !parentPath.isBlank()) result.addProperty("parent_path",
				parentPath.trim());
			result.add("items", items);
			result.addProperty("count", items.size());
			if (hasInteractableMenuItem) ToolRecommendationUtils.addToolRecommendations(result,
				"fiji_ui_control_interact");
			return result.toString();
		}
		catch (final RuntimeException e) {
			return jsonError("Failed to run fiji_ui_menu_read: " + e.getMessage());
		}
	}

	private boolean isInteractableControl(final AWTDialogUtils.ControlInfo control) {
		if (!control.isEnabled() || !control.isShowing()) return false;
		return switch (control.getRole()) {
			case "button", "checkbox", "toggle_button", "combo_box", "choice", "list", "table",
				"tree" -> true;
			case "menu", "menu_item" -> control.getClassName().startsWith("javax.swing.");
			case "text_field", "text_area" -> Boolean.TRUE.equals(control.isEditable()) &&
				!control.isValueRedacted();
			default -> false;
		};
	}

	/**
	 * Performs one supported interaction on a visible control selected by window and component path.
	 *
	 * @param windowTitle exact title from {@code fiji_ui_windows_read}
	 * @param windowClassName optional class name to disambiguate windows with the same title
	 * @param componentPath exact path from {@code fiji_ui_controls_read}
	 * @param action one of {@code click}, {@code set_text}, {@code set_checked}, or {@code select}
	 * @param text new text for {@code set_text}
	 * @param checked desired state for {@code set_checked}
	 * @param index zero-based item or row index for {@code select}
	 * @return JSON containing a run ID and current interaction status
	 */
	@Tool(value = { "Asynchronously interact with one exact visible control in an AWT or Swing window. Returns a run_id immediately; poll fiji_ui_control_interact_status until completed, failed, or blocked_by_dialog. Actions: click (enabled buttons), set_text (editable non-password text fields or areas, requires text), set_checked (checkboxes or toggle buttons, requires checked), select (choices, combo boxes, lists, tables, or visible tree rows, requires a zero-based index). Completion includes only UI changes observed between the start and end of the action." }, name = "fiji_ui_control_interact")
	public String interactWithControl(@P(name = "window_title", value = "Exact window title from fiji_ui_windows_read") final String windowTitle,
		@P(name = "window_class_name", value = "Exact window class name from fiji_ui_windows_read; use it to distinguish windows with the same title", required = false) final String windowClassName,
		@P(name = "component_path", value = "Exact component path from fiji_ui_controls_read for the target control") final String componentPath,
		@P(name = "action", value = "Interaction to perform: click, set_text, set_checked, or select") final String action,
		@P(name = "text", value = "Text to set; required only for set_text", required = false) final String text,
		@P(name = "checked", value = "Desired true/false state; required only for set_checked", required = false) final Boolean checked,
		@P(name = "index", value = "Zero-based item or row index; required only for select", required = false) final Integer index)
	{
		if (windowTitle == null || windowTitle.isBlank()) {
			return jsonError("window_title cannot be null or blank");
		}
		if (componentPath == null || componentPath.isBlank()) {
			return jsonError("component_path cannot be null or blank");
		}
		if (action == null || action.isBlank()) {
			return jsonError("action cannot be null or blank");
		}

		final String normalizedAction = action.trim();
		final InteractionRun run = new InteractionRun(newInteractionRunID(), windowTitle.trim(),
			windowClassName, componentPath.trim(), normalizedAction,
			"fiji_ui_control_interact_status");
		interactionRuns.put(run.runID, run);
		startInteraction(run, () -> AWTDialogUtils.interactWithControl(run.windowTitle,
			run.windowClassName, run.componentPath, run.action, text, checked, index));
		return interactionRunJson(run).toString();
	}

	@Tool(value = { "Poll a UI control interaction run. Returns running or blocked_by_dialog with visible modal dialogs until the action reaches a terminal state; completed runs include changed control paths with their resulting states (null when a path no longer exists), plus window and dialog changes." }, name = "fiji_ui_control_interact_status")
	public String interactWithControlStatus(@P(name = "run_id", value = "Run ID from fiji_ui_control_interact") final String runID) {
		return interactionStatus(runID, "fiji_ui_control_interact_status");
	}

	/**
	 * Requests normal window-manager close handling for one visible frame.
	 *
	 * @param windowTitle exact title from {@code fiji_ui_windows_read}
	 * @param windowClassName optional class name to disambiguate windows with the same title
	 * @return JSON containing a run ID and current close status
	 */
	@Tool(value = { "Asynchronously request the normal window-manager close action for one exact visible frame. Returns a run_id immediately; poll fiji_ui_window_close_status until completed, failed, or blocked_by_dialog. A blocking save prompt is reported for explicit handling; completion includes the frame visibility change." }, name = "fiji_ui_window_close")
	public String closeWindow(@P(name = "window_title", value = "Exact frame title from fiji_ui_windows_read") final String windowTitle,
		@P(name = "window_class_name", value = "Exact frame class name from fiji_ui_windows_read; use it to distinguish windows with the same title", required = false) final String windowClassName)
	{
		if (windowTitle == null || windowTitle.isBlank()) {
			return jsonError("window_title cannot be null or blank");
		}

		final InteractionRun run = new InteractionRun(newInteractionRunID(), windowTitle.trim(),
			windowClassName, null, "close_window", "fiji_ui_window_close_status");
		interactionRuns.put(run.runID, run);
		startInteraction(run, () -> AWTDialogUtils.closeWindow(run.windowTitle,
			run.windowClassName));
		return interactionRunJson(run).toString();
	}

	@Tool(value = { "Poll an asynchronous frame close request. Returns running or blocked_by_dialog with visible modal dialogs until the close request completes, then reports the UI delta, including whether the frame remains visible." }, name = "fiji_ui_window_close_status")
	public String closeWindowStatus(@P(name = "run_id", value = "Run ID from fiji_ui_window_close") final String runID) {
		return interactionStatus(runID, "fiji_ui_window_close_status");
	}

	private String interactionStatus(final String runID, final String statusToolName) {
		if (runID == null || runID.isBlank()) {
			return jsonError("run_id cannot be null or blank");
		}
		final InteractionRun run = interactionRuns.get(runID);
		if (run == null || !statusToolName.equals(run.statusToolName)) return jsonError(
			"No UI interaction found for run_id: " + runID);
		return interactionRunJson(run).toString();
	}

	private void startInteraction(final InteractionRun run,
		final InteractionOperation operation)
	{
		final Thread monitor = new Thread(() -> monitorInteraction(run),
			"Fiji-ui-interaction-monitor-" + run.runID);
		monitor.setDaemon(true);
		try {
			monitor.start();
		}
		catch (final RuntimeException e) {
			run.fail(e.getMessage() == null ? e.toString() : e.getMessage());
			return;
		}

		final Thread worker = new Thread(() -> {
			try {
				final JsonObject initialState = captureInteractionState(run);
				operation.execute();
				final JsonObject finalState = captureInteractionState(run);
				run.complete(interactionDelta(run, initialState, finalState));
			}
			catch (final Throwable t) {
				run.fail(t.getMessage() == null ? t.toString() : t.getMessage());
			}
		}, "Fiji-ui-interaction-" + run.runID);
		worker.setDaemon(true);
		try {
			worker.start();
		}
		catch (final RuntimeException e) {
			run.fail(e.getMessage() == null ? e.toString() : e.getMessage());
		}
	}

	private void monitorInteraction(final InteractionRun run) {
		while (!run.isTerminal()) {
			if (run.snapshotQueued.compareAndSet(false, true)) {
				SwingUtilities.invokeLater(() -> {
					try {
						final JsonArray dialogs = new JsonArray();
						for (final AWTDialogUtils.DialogInfo dialog : AWTDialogUtils
							.getVisibleDialogs())
						{
							if (dialog.isModal()) dialogs.add(dialogJson(dialog));
						}
						run.updateDialogs(dialogs);
					}
					catch (final RuntimeException e) {
						run.recordMonitorError(e.getMessage());
					}
					finally {
						run.snapshotQueued.set(false);
					}
				});
			}
			try {
				Thread.sleep(INTERACTION_MONITOR_INTERVAL_MS);
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	private JsonObject captureInteractionState(final InteractionRun run)
	{
		final JsonObject state = new JsonObject();
		final JsonObject windows = new JsonObject();
		boolean targetWindowVisible = false;
		try {
			for (final AWTDialogUtils.WindowInfo window : AWTDialogUtils.getVisibleWindows()) {
				windows.add(windowKey(window), windowJson(window));
				if (run.windowTitle.equals(window.getTitle()) && (run.windowClassName == null ||
					run.windowClassName.isBlank() || run.windowClassName.equals(window
						.getClassName()))) targetWindowVisible = true;
			}
		}
		catch (final RuntimeException e) {
			state.addProperty("windows_error", e.getMessage());
		}
		state.add("windows", windows);
		state.addProperty("target_window_visible", targetWindowVisible);

		final JsonObject controls = new JsonObject();
		if (targetWindowVisible) {
			try {
				for (final AWTDialogUtils.ControlInfo control : AWTDialogUtils
					.getVisibleControls(run.windowTitle, run.windowClassName))
				{
					controls.add(control.getPath(), controlStateJson(control));
				}
			}
			catch (final RuntimeException e) {
				state.addProperty("controls_error", e.getMessage());
			}
			if (captureVisibleMenuItems(run, controls)) state.addProperty(
				"menu_items_truncated", true);
		}
		state.add("controls", controls);

		final JsonObject dialogs = new JsonObject();
		try {
			for (final AWTDialogUtils.DialogInfo dialog : AWTDialogUtils.getVisibleDialogs()) {
				dialogs.add(dialogKey(dialog), dialogJson(dialog));
			}
		}
		catch (final RuntimeException e) {
			state.addProperty("dialogs_error", e.getMessage());
		}
		state.add("dialogs", dialogs);
		return state;
	}

	private boolean captureVisibleMenuItems(final InteractionRun run,
		final JsonObject controls)
	{
		if (!"click".equals(run.action) || run.componentPath == null || !run.componentPath
			.startsWith("menu_bar/")) return false;

		final int itemSegment = run.componentPath.lastIndexOf("/item[");
		boolean truncated = false;
		if (itemSegment >= 0) truncated = captureMenuItems(run, run.componentPath.substring(0,
			itemSegment), controls);
		return captureMenuItems(run, run.componentPath, controls) || truncated;
	}

	private boolean captureMenuItems(final InteractionRun run, final String parentPath,
		final JsonObject controls)
	{
		try {
			final List<AWTDialogUtils.ControlInfo> menuItems = AWTDialogUtils
				.getVisibleMenuItems(run.windowTitle, run.windowClassName, parentPath);
			for (int index = 0; index < menuItems.size(); index++) {
				if (index == MAX_INTERACTION_MENU_ITEMS) return true;
				final AWTDialogUtils.ControlInfo menuItem = menuItems.get(index);
				controls.add(menuItem.getPath(), controlStateJson(menuItem));
			}
			return false;
		}
		catch (final RuntimeException e) {
			// A closed submenu has no visible entries to snapshot.
			return false;
		}
	}

	private JsonObject interactionDelta(final InteractionRun run, final JsonObject before,
		final JsonObject after)
	{
		final JsonObject delta = new JsonObject();
		delta.addProperty("window_title", run.windowTitle);
		if (run.windowClassName != null && !run.windowClassName.isBlank()) delta.addProperty(
			"window_class_name", run.windowClassName.trim());
		delta.addProperty("action", run.action);
		if (run.componentPath != null) delta.addProperty("component_path", run.componentPath);

		final JsonObject changes = new JsonObject();
		addObjectMapDelta(changes, before.getAsJsonObject("windows"), after.getAsJsonObject(
			"windows"), "windows_opened", "windows_closed", "windows_changed");
		addObjectMapDelta(changes, before.getAsJsonObject("dialogs"), after.getAsJsonObject(
			"dialogs"), "dialogs_opened", "dialogs_closed", "dialogs_changed");

		final boolean windowWasVisible = before.get("target_window_visible").getAsBoolean();
		final boolean windowIsVisible = after.get("target_window_visible").getAsBoolean();
		if (windowWasVisible != windowIsVisible) {
			final JsonObject visibilityChange = new JsonObject();
			visibilityChange.addProperty("before", windowWasVisible);
			visibilityChange.addProperty("after", windowIsVisible);
			changes.add("target_window_visibility", visibilityChange);
		}
		if (windowIsVisible || !windowWasVisible) addControlDelta(changes,
			before.getAsJsonObject("controls"), after.getAsJsonObject("controls"));
		if (before.has("menu_items_truncated") || after.has("menu_items_truncated")) changes
			.addProperty("menu_items_truncated", true);
		addSnapshotWarnings(changes, before, after);
		delta.add("changes", changes);
		return delta;
	}

	private void addObjectMapDelta(final JsonObject changes, final JsonObject before,
		final JsonObject after, final String addedKey, final String removedKey,
		final String changedKey)
	{
		final JsonArray added = new JsonArray();
		final JsonArray removed = new JsonArray();
		final JsonArray changed = new JsonArray();
		for (final String key : after.keySet()) {
			final JsonObject afterValue = after.getAsJsonObject(key);
			if (!before.has(key)) added.add(afterValue.deepCopy());
			else if (!before.get(key).equals(afterValue)) {
				final JsonObject change = new JsonObject();
				change.addProperty("key", key);
				change.add("before", before.getAsJsonObject(key).deepCopy());
				change.add("after", afterValue.deepCopy());
				changed.add(change);
			}
		}
		for (final String key : before.keySet()) {
			if (after.has(key)) continue;
			removed.add(before.getAsJsonObject(key).deepCopy());
		}
		if (added.size() > 0) changes.add(addedKey, added);
		if (removed.size() > 0) changes.add(removedKey, removed);
		if (changed.size() > 0) changes.add(changedKey, changed);
	}

	private void addControlDelta(final JsonObject changes, final JsonObject before,
		final JsonObject after)
	{
		final JsonArray controls = new JsonArray();
		for (final String path : after.keySet()) {
			final JsonObject afterState = after.getAsJsonObject(path);
			final boolean pathWasPresent = before.has(path);
			final boolean includeData;
			if (!pathWasPresent) includeData = true;
			else {
				final JsonObject beforeState = before.getAsJsonObject(path);
				if (beforeState.equals(afterState)) continue;
				includeData = beforeState.has("data") != afterState.has("data") ||
					beforeState.has("data") && !beforeState.get("data").equals(afterState.get("data"));
			}
			final JsonObject controlChange = new JsonObject();
			controlChange.addProperty("component_path", path);
			controlChange.add("state", controlDeltaValue(afterState, includeData));
			controls.add(controlChange);
		}
		for (final String path : before.keySet()) {
			if (after.has(path)) continue;
			final JsonObject controlChange = new JsonObject();
			controlChange.addProperty("component_path", path);
			controlChange.add("state", JsonNull.INSTANCE);
			controls.add(controlChange);
		}
		if (controls.size() > 0) changes.add("controls", controls);
	}

	private JsonObject controlDeltaValue(final JsonObject state,
		final boolean includeData)
	{
		final JsonObject result = state.getAsJsonObject("control").deepCopy();
		result.remove("component_path");
		if (includeData && state.has("data")) result.add("data", state.get("data")
			.deepCopy());
		return result;
	}

	private JsonObject controlStateJson(final AWTDialogUtils.ControlInfo control) {
		final JsonObject state = new JsonObject();
		state.add("control", compactControlJson(control));
		if (!control.getData().isEmpty()) state.add("data", controlDataState(control.getData()));
		return state;
	}

	private JsonObject controlDataState(final Map<String, Object> data) {
		final JsonObject state = GSON.toJsonTree(data).getAsJsonObject().deepCopy();
		for (final String key : List.of("items", "rows", "nodes")) {
			if (!state.has(key) || !state.get(key).isJsonArray()) continue;
			final JsonArray entries = state.getAsJsonArray(key);
			for (int index = 0; index < entries.size(); index++) {
				if (entries.get(index).isJsonObject()) entries.get(index).getAsJsonObject()
					.remove("selected");
			}
		}
		return state;
	}

	private String windowKey(final AWTDialogUtils.WindowInfo window) {
		return window.getClassName() + "|" + window.getTitle() + "|" + window.getOwnerTitle();
	}

	private String dialogKey(final AWTDialogUtils.DialogInfo dialog) {
		return dialog.getClassName() + "|" + dialog.getTitle() + "|" + dialog.getOwnerName();
	}

	private void addSnapshotWarnings(final JsonObject changes, final JsonObject before,
		final JsonObject after)
	{
		final JsonArray warnings = new JsonArray();
		for (final String key : List.of("windows_error", "controls_error", "dialogs_error")) {
			if (before.has(key)) warnings.add("before: " + before.get(key).getAsString());
			if (after.has(key)) warnings.add("after: " + after.get(key).getAsString());
		}
		if (warnings.size() > 0) changes.add("snapshot_warnings", warnings);
	}

	private JsonObject compactControlJson(final AWTDialogUtils.ControlInfo control) {
		final JsonObject result = new JsonObject();
		result.addProperty("component_path", control.getPath());
		result.addProperty("role", control.getRole());
		result.addProperty("class_name", control.getClassName());
		if (control.getText() != null) result.addProperty("text", compactValue(control.getText()));
		if (control.getValue() != null) result.addProperty("value", compactValue(control
			.getValue()));
		if (control.isSelected() != null) result.addProperty("selected", control.isSelected());
		if (control.isEditable() != null) result.addProperty("editable", control.isEditable());
		result.addProperty("enabled", control.isEnabled());
		result.addProperty("showing", control.isShowing());
		if (control.getActionCommand() != null && !control.getActionCommand().isBlank())
			result.addProperty("action_command", control.getActionCommand());
		if (control.getRole().equals("menu")) result.addProperty("has_submenu", true);
		if (control.getData() != null) {
			final JsonArray selectedItems = selectedItems(control.getData());
			if (selectedItems.size() > 0) result.add("selected_items", selectedItems);
		}
		return result;
	}

	private JsonArray selectedItems(final Map<String, Object> data) {
		final JsonArray result = new JsonArray();
		final String[] keys = { "items", "rows", "nodes" };
		for (final String key : keys) {
			final Object rawItems = data.get(key);
			if (!(rawItems instanceof List<?> items)) continue;
			for (final Object rawItem : items) {
				if (!(rawItem instanceof Map<?, ?> item) || !Boolean.TRUE.equals(item.get(
					"selected"))) continue;
				final JsonObject selected = new JsonObject();
				for (final String field : List.of("index", "row", "value", "values", "path")) {
					final Object value = item.get(field);
					if (value != null) selected.add(field, GSON.toJsonTree(value));
				}
				result.add(selected);
				if (result.size() == 5) return result;
			}
		}
		return result;
	}

	private static String compactValue(final String value) {
		return value.length() <= 256 ? value : value.substring(0, 256) + "...";
	}

	private JsonObject interactionRunJson(final InteractionRun run) {
		final JsonObject result = new JsonObject();
		result.addProperty("run_id", run.runID);
		result.addProperty("status", run.status);
		final long endTime = run.finishedAt > 0 ? run.finishedAt : System.currentTimeMillis();
		result.addProperty("duration_ms", Math.max(0, endTime - run.startedAt));
		if (run.error != null && !run.error.isBlank()) result.addProperty("error", run.error);
		if (run.monitorError != null && !run.monitorError.isBlank()) result.addProperty(
			"monitor_error", run.monitorError);
		if (run.dialogs.size() > 0) result.add("dialogs", run.dialogs.deepCopy());
		if (run.snapshot != null) result.add("snapshot", run.snapshot.deepCopy());
		if ("blocked_by_dialog".equals(run.status)) ToolRecommendationUtils
			.addToolRecommendations(result, "fiji_ui_dialog_respond", "fiji_ui_dialog_close",
				run.statusToolName);
		else if ("running".equals(run.status)) ToolRecommendationUtils
			.addToolRecommendations(result, run.statusToolName);
		return result;
	}

	private static String newInteractionRunID() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	@Tool(value = { "Capture a PNG screenshot of one visible window or component using current screen pixels. The capture may include overlapping or occluding windows, and requested focus changes are best effort." }, name = "fiji_ui_screenshot")
	public List<Content> screenshot(@P(name = "window_title", value = "Exact window title from fiji_ui_windows_read") final String windowTitle,
		@P(name = "window_class_name", value = "Exact window class name for the same window; use it to distinguish windows with the same title", required = false) final String windowClassName,
		@P(name = "component_path", value = "Component path from fiji_ui_controls_read, to crop to that component", required = false) final String componentPath)
	{
		try {
			if (!imageRenderingService.isPixelSharingAllowed()) return List.of(TextContent
				.from(jsonError(ImageRenderingService.PIXEL_SHARING_DISABLED_MESSAGE)));
			final AWTDialogUtils.ScreenshotResult screenshot = AWTDialogUtils
				.captureScreenshot(windowTitle, windowClassName, componentPath);
			final JsonObject metadata = new JsonObject();
			metadata.add("window", windowJson(screenshot.getWindowInfo()));
			if (screenshot.getControlInfo() != null) metadata.add("control", controlJson(
				screenshot.getControlInfo()));
			metadata.add("capture_bounds", boundsJson(screenshot.getBounds()));
			if (screenshot.isFocusRequested()) {
				metadata.addProperty("focus_requested", true);
				metadata.addProperty("focus_restored", screenshot.isRestored());
				if (!screenshot.isRestored()) {
					metadata.addProperty("previous_window_title", screenshot
						.getPreviousWindowTitle());
					metadata.addProperty("previous_window_class_name", screenshot
						.getPreviousWindowClassName());
					metadata.addProperty("previous_focus_owner_name", screenshot
						.getPreviousFocusOwnerName());
					metadata.addProperty("previous_focus_owner_class_name", screenshot
						.getPreviousFocusOwnerClassName());
				}
			}
			return List.of(TextContent.from(metadata.toString()), ImageContent.from(Base64
				.getEncoder().encodeToString(screenshot.getPngBytes()), "image/png"));
		}
		catch (final RuntimeException e) {
			return List.of(TextContent.from(jsonError("Failed to run fiji_ui_screenshot: " + e
				.getMessage())));
		}
	}

	@Tool(value = { "List visible AWT and Swing dialogs, including each dialog's title, messages, buttons, and modal state" }, name = "fiji_ui_dialogs_read")
	public String readDialogs() {
		try {
			final JsonArray dialogs = new JsonArray();
			for (final AWTDialogUtils.DialogInfo dialog : AWTDialogUtils
				.getVisibleDialogs())
			{
				dialogs.add(dialogJson(dialog));
			}
			final JsonObject result = new JsonObject();
			if (dialogs.size() > 0) {
				result.add("dialogs", dialogs);
				ToolRecommendationUtils.addToolRecommendations(result, "fiji_ui_dialog_respond",
					"fiji_ui_dialog_close");
			}
			result.addProperty("count", dialogs.size());
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_ui_dialogs_read: " + e.getMessage());
		}
	}

	@Tool(value = { "Click an exact enabled button in one visible dialog. This tool rejects missing or ambiguous dialogs and never chooses a button implicitly" }, name = "fiji_ui_dialog_respond")
	public String respondToDialog(@P(name = "dialog_title", value = "Dialog title from fiji_ui_dialogs_read") final String dialogTitle,
		@P(name = "button_text", value = "Button text from the same dialog") final String buttonText)
	{
		if (dialogTitle == null || dialogTitle.isBlank()) {
			return jsonError("dialog_title cannot be null or blank");
		}
		if (buttonText == null || buttonText.isBlank()) {
			return jsonError("button_text cannot be null or blank");
		}

		try {
			final AWTDialogUtils.DialogResponse response = AWTDialogUtils
				.respondToDialog(dialogTitle, buttonText);
			final JsonObject result = new JsonObject();
			result.addProperty("dialog_title", response.getDialogTitle());
			result.addProperty("dialog_class_name", response.getDialogClassName());
			result.addProperty("button_text", response.getButtonText());
			if (response.getActionCommand() != null && !response.getActionCommand().isBlank())
				result.addProperty("action_command", response.getActionCommand());
			result.addProperty("dialog_visible_after", response
				.isDialogVisibleAfter());
			if (response.isDialogVisibleAfter()) ToolRecommendationUtils.addToolRecommendations(result,
				"fiji_ui_dialogs_read");
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_ui_dialog_respond: " + e.getMessage());
		}
	}

	@Tool(value = { "Request the normal window-manager close action for one visible dialog. This tool rejects missing or ambiguous dialogs, does not choose a button, and reports whether the dialog remains visible" }, name = "fiji_ui_dialog_close")
	public String closeDialog(@P(name = "dialog_title", value = "Dialog title from fiji_ui_dialogs_read") final String dialogTitle) {
		if (dialogTitle == null || dialogTitle.isBlank()) {
			return jsonError("dialog_title cannot be null or blank");
		}

		try {
			final AWTDialogUtils.DialogCloseResponse response = AWTDialogUtils
				.closeDialog(dialogTitle);
			final JsonObject result = new JsonObject();
			result.addProperty("dialog_title", response.getDialogTitle());
			result.addProperty("dialog_class_name", response.getDialogClassName());
			result.addProperty("dialog_visible_after", response
				.isDialogVisibleAfter());
			if (response.isDialogVisibleAfter()) ToolRecommendationUtils.addToolRecommendations(result,
				"fiji_ui_dialogs_read");
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_ui_dialog_close: " + e.getMessage());
		}
	}

	private JsonObject dialogJson(final AWTDialogUtils.DialogInfo dialog) {
		final JsonObject result = new JsonObject();
		result.addProperty("dialog_title", dialog.getTitle());
		result.addProperty("dialog_class_name", dialog.getClassName());
		result.addProperty("active", dialog.isActive());
		result.addProperty("modal", dialog.isModal());
		result.addProperty("modality_type", dialog.getModalityType());
		if (!dialog.getOwnerName().isBlank()) result.addProperty("owner_name", dialog
			.getOwnerName());

		final JsonArray messages = new JsonArray();
		for (final String message : dialog.getMessages()) messages.add(message);
		if (messages.size() > 0) result.add("messages", messages);

		final JsonArray buttons = new JsonArray();
		for (final AWTDialogUtils.ButtonInfo button : dialog.getButtons()) {
			final JsonObject buttonJson = new JsonObject();
			buttonJson.addProperty("button_text", button.getText());
			if (button.getActionCommand() != null && !button.getActionCommand().isBlank())
				buttonJson.addProperty("action_command", button.getActionCommand());
			buttonJson.addProperty("enabled", button.isEnabled());
			buttonJson.addProperty("visible", button.isVisible());
			buttons.add(buttonJson);
		}
		if (buttons.size() > 0) result.add("buttons", buttons);
		return result;
	}

	private JsonObject windowJson(final AWTDialogUtils.WindowInfo window) {
		final JsonObject result = new JsonObject();
		result.addProperty("window_title", window.getTitle());
		result.addProperty("window_class_name", window.getClassName());
		result.addProperty("type", window.getType());
		result.addProperty("active", window.isActive());
		result.addProperty("focused", window.isFocused());
		result.addProperty("modal", window.isModal());
		result.addProperty("modality_type", window.getModalityType());
		if (!window.getOwnerTitle().isBlank()) result.addProperty("owner_title", window
			.getOwnerTitle());
		if (window.getBounds() != null) result.add("bounds", boundsJson(window
			.getBounds()));
		return result;
	}

	private JsonObject controlJson(final AWTDialogUtils.ControlInfo control) {
		final JsonObject result = new JsonObject();
		result.addProperty("component_path", control.getPath());
		result.addProperty("role", control.getRole());
		result.addProperty("class_name", control.getClassName());
		if (control.getText() != null) result.addProperty("text", control.getText());
		if (control.getValue() != null) result.addProperty("value", control.getValue());
		if (control.isSelected() != null) result.addProperty("selected", control
			.isSelected());
		if (control.isEditable() != null) result.addProperty("editable", control
			.isEditable());
		if (control.isValueRedacted()) result.addProperty("value_redacted", true);
		result.addProperty("enabled", control.isEnabled());
		result.addProperty("showing", control.isShowing());
		if (control.getActionCommand() != null && !control.getActionCommand().isBlank())
			result.addProperty("action_command", control.getActionCommand());
		if (control.getBounds() != null) result.add("bounds", boundsJson(control
			.getBounds()));
		if (!control.getData().isEmpty()) result.add("data", GSON.toJsonTree(control
			.getData()).getAsJsonObject());
		return result;
	}

	private JsonObject boundsJson(final Rectangle bounds) {
		final JsonObject result = new JsonObject();
		result.addProperty("x", bounds.x);
		result.addProperty("y", bounds.y);
		result.addProperty("width", bounds.width);
		result.addProperty("height", bounds.height);
		return result;
	}

	@FunctionalInterface
	private interface InteractionOperation {

		void execute();
	}

	private static final class InteractionRun {

		private final String runID;
		private final String windowTitle;
		private final String windowClassName;
		private final String componentPath;
		private final String action;
		private final String statusToolName;
		private final long startedAt = System.currentTimeMillis();
		private final AtomicBoolean snapshotQueued = new AtomicBoolean();
		private volatile String status = "running";
		private volatile JsonArray dialogs = new JsonArray();
		private volatile JsonObject snapshot;
		private volatile String error;
		private volatile String monitorError;
		private volatile long finishedAt;

		private InteractionRun(final String runID, final String windowTitle,
			final String windowClassName, final String componentPath, final String action,
			final String statusToolName)
		{
			this.runID = runID;
			this.windowTitle = windowTitle;
			this.windowClassName = windowClassName;
			this.componentPath = componentPath;
			this.action = action;
			this.statusToolName = statusToolName;
		}

		private synchronized void updateDialogs(final JsonArray visibleModalDialogs) {
			if (isTerminal()) return;
			dialogs = visibleModalDialogs;
			status = dialogs.size() > 0 ? "blocked_by_dialog" : "running";
		}

		private synchronized void complete(final JsonObject resultSnapshot) {
			if (isTerminal()) return;
			snapshot = resultSnapshot;
			dialogs = new JsonArray();
			status = "completed";
			finishedAt = System.currentTimeMillis();
		}

		private synchronized void fail(final String message) {
			if (isTerminal()) return;
			error = message;
			status = "failed";
			finishedAt = System.currentTimeMillis();
		}

		private synchronized void recordMonitorError(final String message) {
			if (!isTerminal() && message != null && !message.isBlank()) monitorError = message;
		}

		private boolean isTerminal() {
			return "completed".equals(status) || "failed".equals(status);
		}
	}
}
