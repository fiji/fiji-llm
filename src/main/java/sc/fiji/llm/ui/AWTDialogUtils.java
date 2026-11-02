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

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.TextComponent;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.accessibility.AccessibleContext;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;

/** Utilities for inspecting and responding to visible AWT and Swing UI. */
public final class AWTDialogUtils {

	private static final int MAX_CONTROL_DATA_ITEMS = 100;

	private AWTDialogUtils() {
		// utility class
	}

	/** Returns descriptions of all visible dialogs, reading their components on the EDT. */
	@SuppressWarnings( "unchecked" )
	public static List<DialogInfo> getVisibleDialogs() {
		if (SwingUtilities.isEventDispatchThread()) return collectVisibleDialogs();

		final List<DialogInfo>[] result = new List[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = collectVisibleDialogs());
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not inspect AWT dialogs", e);
		}
		return result[0];
	}

	/** Returns descriptions of all visible AWT and Swing windows on the EDT. */
	public static List<WindowInfo> getVisibleWindows() {
		return invokeOnEdt(AWTDialogUtils::collectVisibleWindowInfos);
	}

	/** Returns controls in the uniquely selected visible window. */
	public static List<ControlInfo> getVisibleControls(final String windowTitle,
		final String windowClassName)
	{
		return invokeOnEdt(() -> collectControls(findVisibleWindow(windowTitle,
			windowClassName)));
	}

	/** Returns one level of menus from a visible Swing menu bar. */
	public static List<ControlInfo> getVisibleMenuItems(final String windowTitle,
		final String windowClassName, final String parentPath)
	{
		return invokeOnEdt(() -> collectVisibleMenuItems(findVisibleWindow(windowTitle,
			windowClassName), parentPath));
	}

	/** Captures a visible window or component, restoring the previous focus afterward. */
	public static ScreenshotResult captureScreenshot(final String windowTitle,
		final String windowClassName, final String componentPath)
	{
		final CaptureTarget target = invokeOnEdt(() -> prepareCapture(windowTitle,
			windowClassName, componentPath));
		byte[] pngBytes;
		boolean restored = !target.focusRequested;
		try {
			final Robot robot = new Robot();
			robot.setAutoWaitForIdle(true);
			robot.waitForIdle();
			final BufferedImage image = robot.createScreenCapture(target.bounds);
			pngBytes = encodePng(image);
		}
		catch (final AWTException | IOException | RuntimeException e) {
			throw new IllegalStateException("Could not capture the target UI", e);
		}
		finally {
			if (target.focusRequested) restored = invokeOnEdt(() -> restoreFocus(target));
		}
		return new ScreenshotResult(pngBytes, target.windowInfo, target.controlInfo,
			target.bounds, target.previousWindow, target.previousFocusOwner,
			target.focusRequested, restored);
	}

	private static <T> T invokeOnEdt(final EdtOperation<T> operation) {
		if (SwingUtilities.isEventDispatchThread()) return operation.run();

		final AtomicReference<T> result = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> result.set(operation.run()));
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while accessing the AWT UI", e);
		}
		catch (final InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
			throw new IllegalStateException("Could not access the AWT UI", e.getCause());
		}
		return result.get();
	}

	private static List<WindowInfo> collectVisibleWindowInfos() {
		final List<WindowInfo> windows = new ArrayList<>();
		for (final Window window : Window.getWindows()) {
			if (window.isShowing()) windows.add(describeWindow(window));
		}
		return Collections.unmodifiableList(windows);
	}

	private static Window findVisibleWindow(final String windowTitle,
		final String windowClassName)
	{
		final String title = normalize(windowTitle);
		final String className = normalize(windowClassName);
		if (title.isEmpty()) {
			final Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.getActiveWindow();
			if (active != null && active.isShowing() && matchesWindow(active, className)) return active;
			throw new IllegalStateException(
				"No active visible window is available; provide window_title");
		}

		final List<Window> matches = new ArrayList<>();
		for (final Window window : Window.getWindows()) {
			if (window.isShowing() && title.equals(windowTitle(window)) && matchesWindow(
				window, className)) matches.add(window);
		}
		if (matches.isEmpty()) throw new IllegalStateException(
			"No visible window has title: " + title);
		if (matches.size() > 1) throw new IllegalStateException(
			"Multiple visible windows have title: " + title);
		return matches.get(0);
	}

	private static boolean matchesWindow(final Window window,
		final String windowClassName)
	{
		return windowClassName.isEmpty() || windowClassName.equals(window.getClass().getName());
	}

	private static List<ControlInfo> collectVisibleMenuItems(final Window window,
		final String parentPath)
	{
		if (!(window instanceof RootPaneContainer rootPaneContainer)) throw new IllegalStateException(
			"Target window does not have a Swing menu bar");
		final JMenuBar menuBar = rootPaneContainer.getRootPane().getJMenuBar();
		if (menuBar == null || !menuBar.isShowing()) throw new IllegalStateException(
			"Target window has no showing Swing menu bar");

		final List<ControlInfo> menus = new ArrayList<>();
		if (parentPath == null || parentPath.isBlank()) {
			for (int index = 0; index < menuBar.getMenuCount(); index++) {
				final JMenu menu = menuBar.getMenu(index);
				if (menu != null && menu.isShowing()) menus.add(describeControl(menu,
					"menu_bar/menu[" + index + "]"));
			}
		}
		else {
			final JMenu parentMenu = resolveSwingMenu(menuBar, parentPath);
			if (!parentMenu.getPopupMenu().isShowing()) throw new IllegalStateException(
				"Open the menu before reading its entries: " + parentPath);
			for (int index = 0; index < parentMenu.getItemCount(); index++) {
				final JMenuItem item = parentMenu.getItem(index);
				if (item != null && item.isShowing()) menus.add(describeControl(item,
					parentPath + "/item[" + index + "]"));
			}
		}
		return Collections.unmodifiableList(menus);
	}

	private static JMenu resolveSwingMenu(final JMenuBar menuBar,
		final String menuPath)
	{
		final String[] segments = menuPath.split("/");
		if (segments.length < 2 || !"menu_bar".equals(segments[0])) throw new IllegalArgumentException(
			"parent_path must be a menu path returned by fiji_ui_menu_read");

		final int menuIndex = parseMenuIndex(segments[1], "menu", menuBar.getMenuCount());
		JMenu menu = menuBar.getMenu(menuIndex);
		if (menu == null || !menu.isShowing()) throw new IllegalStateException(
			"Menu is not showing: " + menuPath);

		for (int segmentIndex = 2; segmentIndex < segments.length; segmentIndex++) {
			if (!menu.getPopupMenu().isShowing()) throw new IllegalStateException(
				"Open the menu before reading its entries: " + menuPath);
			final int itemIndex = parseMenuIndex(segments[segmentIndex], "item", menu
				.getItemCount());
			final JMenuItem item = menu.getItem(itemIndex);
			if (!(item instanceof JMenu childMenu) || !item.isShowing()) throw new IllegalStateException(
				"Menu path does not identify a showing submenu: " + menuPath);
			menu = childMenu;
		}
		return menu;
	}

	private static int parseMenuIndex(final String segment, final String prefix,
		final int itemCount)
	{
		if (!segment.startsWith(prefix + "[") || !segment.endsWith("]")) throw new IllegalArgumentException(
			"Invalid menu path segment: " + segment);
		final int index;
		try {
			index = Integer.parseInt(segment.substring(prefix.length() + 1, segment.length() - 1));
		}
		catch (final NumberFormatException e) {
			throw new IllegalArgumentException("Invalid menu path index: " + segment, e);
		}
		if (index < 0 || index >= itemCount) throw new IllegalArgumentException(
			"Menu path index is out of range: " + segment);
		return index;
	}

	private static List<ControlInfo> collectControls(final Window window) {
		final List<ControlInfo> controls = new ArrayList<>();
		collectControls(window, "root", controls, Collections.newSetFromMap(
			new IdentityHashMap<>()));
		if (window instanceof Frame frame && frame.getMenuBar() != null) {
			collectTopLevelMenuControls(frame.getMenuBar(), "menu_bar", controls);
		}
		return Collections.unmodifiableList(controls);
	}

	private static void collectControls(final Component component, final String path,
		final List<ControlInfo> controls, final Set<Component> visited)
	{
		if (!visited.add(component) || !component.isShowing()) return;
		final ControlInfo control = describeControl(component, path);
		if (control != null) controls.add(control);
		if (component instanceof Container container) {
			final Component[] children = container.getComponents();
			for (int i = 0; i < children.length; i++) {
				collectControls(children[i], path + "/child[" + i + "]", controls, visited);
			}
		}
	}

	private static void collectTopLevelMenuControls(final MenuBar menuBar,
		final String path,
		final List<ControlInfo> controls)
	{
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			final Menu menu = menuBar.getMenu(i);
			if (menu == null) continue;
			controls.add(new ControlInfo(path + "/menu[" + i + "]", "menu", menu
				.getClass().getName(), menu.getLabel(), null, null, null, menu.isEnabled(),
				true, true, false, menu.getActionCommand(), null));
		}
	}

	static ControlInfo describeControl(final Component component,
		final String path)
	{
		final Rectangle bounds = screenBounds(component);
		if (component instanceof JTree tree) return new ControlInfo(path, "tree",
			component.getClass().getName(), null, null, null, null, component.isEnabled(),
			component.isVisible(), component.isShowing(), false, null, bounds, treeData(tree));
		if (component instanceof JList<?> list) return new ControlInfo(path, "list",
			component.getClass().getName(), null, null, null, null, component.isEnabled(),
			component.isVisible(), component.isShowing(), false, null, bounds, listData(list));
		if (component instanceof JTable table) return new ControlInfo(path, "table",
			component.getClass().getName(), null, null, null, null, component.isEnabled(),
			component.isVisible(), component.isShowing(), false, null, bounds, tableData(table));
		if (component instanceof JLabel label) return new ControlInfo(path, "label",
			component.getClass().getName(), label.getText(), null, null, null, component
				.isEnabled(), component.isVisible(), component.isShowing(), false, null,
				bounds);
		if (component instanceof Label label) return new ControlInfo(path, "label",
			component.getClass().getName(), label.getText(), null, null, null, component
				.isEnabled(), component.isVisible(), component.isShowing(), false, null,
				bounds);
		if (component instanceof Checkbox checkbox) return new ControlInfo(path,
			"checkbox", component.getClass().getName(), checkbox.getLabel(), null, checkbox
				.getState(), null, checkbox.isEnabled(), checkbox.isVisible(), checkbox
				.isShowing(), false, null, bounds);
		if (component instanceof Button button) {
			final String text = buttonLabel(button.getLabel(), button);
			final String actionCommand = button.getActionCommand();
			if (isAnonymousButton(text, actionCommand)) return null;
			return new ControlInfo(path, "button", component.getClass().getName(), text, null,
				null, null, button.isEnabled(), button.isVisible(), button.isShowing(), false,
				actionCommand, bounds);
		}
		if (component instanceof JComboBox<?> combo) return new ControlInfo(path,
			"combo_box", component.getClass().getName(), null, selectedItem(combo
				.getSelectedItem()), null, null, combo.isEnabled(), combo.isVisible(), combo
				.isShowing(), false, null, bounds, comboBoxData(combo));
		if (component instanceof Choice choice) return new ControlInfo(path,
			"choice", component.getClass().getName(), null, choice.getSelectedItem(), null,
			null, choice.isEnabled(), choice.isVisible(), choice.isShowing(), false, null,
			bounds, choiceData(choice));
		if (component instanceof AbstractButton button) return describeButton(button,
			path, bounds);
		if (component instanceof JTextComponent textComponent) {
			final boolean redacted = textComponent instanceof JPasswordField;
			return new ControlInfo(path, "text_field", component.getClass().getName(),
				null, redacted ? null : textComponent.getText(), null, textComponent
				.isEditable(), textComponent.isEnabled(), textComponent.isVisible(),
				textComponent.isShowing(), redacted, null, bounds);
		}
		if (component instanceof TextComponent textComponent) {
			final boolean redacted = textComponent instanceof TextField textField && textField
				.getEchoChar() != '\0';
			return new ControlInfo(path, component instanceof TextArea ? "text_area" :
				"text_field", component.getClass().getName(), null, redacted ? null :
				textComponent.getText(), null, textComponent.isEditable(), textComponent
				.isEnabled(), textComponent.isVisible(), textComponent.isShowing(), redacted,
				null, bounds);
		}
		if (component instanceof JMenuBar) return new ControlInfo(path, "menu_bar",
			component.getClass().getName(), null, null, null, null, component.isEnabled(),
			component.isVisible(), component.isShowing(), false, null, bounds);
		return null;
	}

	private static ControlInfo describeButton(final AbstractButton button,
		final String path, final Rectangle bounds)
	{
		final String role;
		if (button instanceof JMenu) role = "menu";
		else if (button instanceof JMenuItem) role = "menu_item";
		else if (button instanceof JCheckBox) role = "checkbox";
		else if (button instanceof JRadioButton) role = "radio_button";
		else if (button instanceof JToggleButton) role = "toggle_button";
		else role = "button";
		final boolean hasSelectedState = button instanceof JToggleButton || button instanceof
			JCheckBoxMenuItem || button instanceof JRadioButtonMenuItem;
		final String text = buttonLabel(button.getText(), button);
		final String actionCommand = button.getActionCommand();
		if (isAnonymousButton(text, actionCommand)) return null;
		return new ControlInfo(path, role, button.getClass().getName(), text, null,
			hasSelectedState ? button.isSelected() : null, null, button.isEnabled(), button
				.isVisible(), button.isShowing(), false, actionCommand, bounds);
	}

	private static String buttonLabel(final String text, final Component component) {
		if (text != null && !text.isBlank()) return text;
		final AccessibleContext accessibleContext = component.getAccessibleContext();
		if (accessibleContext != null) {
			final String accessibleName = accessibleContext.getAccessibleName();
			if (accessibleName != null && !accessibleName.isBlank()) return accessibleName;
		}
		if (component instanceof AbstractButton button) {
			final String toolTip = button.getToolTipText();
			if (toolTip != null && !toolTip.isBlank()) return toolTip;
		}
		return null;
	}

	private static boolean isAnonymousButton(final String text,
		final String actionCommand)
	{
		return (text == null || text.isBlank()) && (actionCommand == null || actionCommand
			.isBlank());
	}

	private static Map<String, Object> comboBoxData(final JComboBox<?> combo) {
		final List<String> options = new ArrayList<>();
		for (int index = 0; index < combo.getItemCount(); index++) options.add(String.valueOf(
			combo.getItemAt(index)));
		return optionData(options, combo.getSelectedIndex());
	}

	private static Map<String, Object> choiceData(final Choice choice) {
		final List<String> options = new ArrayList<>();
		for (int index = 0; index < choice.getItemCount(); index++) options.add(choice.getItem(
			index));
		return optionData(options, choice.getSelectedIndex());
	}

	private static Map<String, Object> optionData(final List<String> options,
		final int selectedIndex)
	{
		final List<Map<String, Object>> items = new ArrayList<>();
		for (int index = 0; index < Math.min(options.size(), MAX_CONTROL_DATA_ITEMS); index++) {
			final Map<String, Object> item = new LinkedHashMap<>();
			item.put("index", index);
			item.put("value", options.get(index));
			item.put("selected", index == selectedIndex);
			items.add(item);
		}

		final Map<String, Object> data = new LinkedHashMap<>();
		data.put("item_count", options.size());
		data.put("items", items);
		data.put("truncated", options.size() > items.size());
		return data;
	}

	private static Map<String, Object> listData(final JList<?> list) {
		final int itemCount = list.getModel().getSize();
		final List<Map<String, Object>> items = new ArrayList<>();
		for (int index = 0; index < Math.min(itemCount, MAX_CONTROL_DATA_ITEMS); index++) {
			final Map<String, Object> item = new LinkedHashMap<>();
			item.put("index", index);
			item.put("value", String.valueOf(list.getModel().getElementAt(index)));
			item.put("selected", list.isSelectedIndex(index));
			items.add(item);
		}

		final Map<String, Object> data = new LinkedHashMap<>();
		data.put("item_count", itemCount);
		data.put("items", items);
		data.put("truncated", itemCount > items.size());
		return data;
	}

	private static Map<String, Object> tableData(final JTable table) {
		final int rowCount = table.getRowCount();
		final int columnCount = table.getColumnCount();
		final List<String> columns = new ArrayList<>();
		for (int column = 0; column < Math.min(columnCount, MAX_CONTROL_DATA_ITEMS); column++) {
			columns.add(table.getColumnName(column));
		}

		final List<Map<String, Object>> rows = new ArrayList<>();
		for (int row = 0; row < Math.min(rowCount, MAX_CONTROL_DATA_ITEMS); row++) {
			final List<String> values = new ArrayList<>();
			for (int column = 0; column < Math.min(columnCount, MAX_CONTROL_DATA_ITEMS); column++) {
				values.add(String.valueOf(table.getValueAt(row, column)));
			}
			final Map<String, Object> rowData = new LinkedHashMap<>();
			rowData.put("index", row);
			rowData.put("selected", table.isRowSelected(row));
			rowData.put("values", values);
			rows.add(rowData);
		}

		final Map<String, Object> data = new LinkedHashMap<>();
		data.put("row_count", rowCount);
		data.put("column_count", columnCount);
		data.put("columns", columns);
		data.put("rows", rows);
		data.put("truncated", rowCount > rows.size() || columnCount > columns.size());
		return data;
	}

	private static Map<String, Object> treeData(final JTree tree) {
		final int rowCount = tree.getRowCount();
		final List<Map<String, Object>> nodes = new ArrayList<>();
		for (int row = 0; row < Math.min(rowCount, MAX_CONTROL_DATA_ITEMS); row++) {
			final TreePath treePath = tree.getPathForRow(row);
			if (treePath == null) continue;

			final Object[] pathComponents = treePath.getPath();
			final List<String> labels = new ArrayList<>();
			final List<Integer> pathIndices = new ArrayList<>();
			for (int index = 0; index < pathComponents.length; index++) {
				labels.add(String.valueOf(pathComponents[index]));
				pathIndices.add(index == 0 ? 0 : tree.getModel().getIndexOfChild(
					pathComponents[index - 1], pathComponents[index]));
			}

			final Object node = pathComponents[pathComponents.length - 1];
			final Map<String, Object> nodeData = new LinkedHashMap<>();
			nodeData.put("row", row);
			nodeData.put("value", labels.get(labels.size() - 1));
			nodeData.put("path", labels);
			nodeData.put("path_indices", pathIndices);
			nodeData.put("selected", tree.isPathSelected(treePath));
			nodeData.put("expanded", tree.isExpanded(treePath));
			nodeData.put("leaf", tree.getModel().isLeaf(node));
			nodes.add(nodeData);
		}

		final Map<String, Object> data = new LinkedHashMap<>();
		data.put("visible_row_count", rowCount);
		data.put("nodes", nodes);
		data.put("truncated", rowCount > nodes.size());
		return data;
	}

	private static Component findComponent(final Window window, final String componentPath) {
		final String path = normalize(componentPath);
		if (path.isEmpty() || "root".equals(path)) return window;
		final Component[] match = new Component[1];
		findComponent(window, "root", path, match, Collections.newSetFromMap(
			new IdentityHashMap<>()));
		if (match[0] == null) throw new IllegalStateException(
			"No visible component has path: " + path);
		return match[0];
	}

	private static JMenuItem findSwingMenuComponent(final Window window,
		final String menuPath)
	{
		if (!(window instanceof RootPaneContainer rootPaneContainer)) throw new IllegalStateException(
			"Target window does not have a Swing menu bar");
		final JMenuBar menuBar = rootPaneContainer.getRootPane().getJMenuBar();
		if (menuBar == null || !menuBar.isShowing()) throw new IllegalStateException(
			"Target window has no showing Swing menu bar");

		final int lastItemSegment = menuPath.lastIndexOf("/item[");
		if (lastItemSegment < 0) return resolveSwingMenu(menuBar, menuPath);

		final String parentPath = menuPath.substring(0, lastItemSegment);
		final JMenu parentMenu = resolveSwingMenu(menuBar, parentPath);
		if (!parentMenu.getPopupMenu().isShowing()) throw new IllegalStateException(
			"Open the menu before interacting with its entries: " + parentPath);
		final String itemSegment = menuPath.substring(lastItemSegment + 1);
		final int itemIndex = parseMenuIndex(itemSegment, "item", parentMenu.getItemCount());
		final JMenuItem item = parentMenu.getItem(itemIndex);
		if (item == null || !item.isShowing()) throw new IllegalStateException(
			"Menu item is not showing: " + menuPath);
		return item;
	}

	private static void findComponent(final Component component, final String path,
		final String targetPath, final Component[] match, final Set<Component> visited)
	{
		if (match[0] != null || !visited.add(component)) return;
		if (path.equals(targetPath)) {
			match[0] = component;
			return;
		}
		if (component instanceof Container container) {
			final Component[] children = container.getComponents();
			for (int i = 0; i < children.length; i++) findComponent(children[i], path +
				"/child[" + i + "]", targetPath, match, visited);
		}
	}

	private static CaptureTarget prepareCapture(final String windowTitle,
		final String windowClassName, final String componentPath)
	{
		final Window targetWindow = findVisibleWindow(windowTitle, windowClassName);
		final Component targetComponent = findComponent(targetWindow, componentPath);
		if (!targetComponent.isShowing()) throw new IllegalStateException(
			"Target component is not showing: " + normalize(componentPath));
		final Window previousWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getActiveWindow();
		final Component previousFocusOwner = KeyboardFocusManager
			.getCurrentKeyboardFocusManager().getFocusOwner();
		final boolean focusRequested = !targetWindow.isActive();
		if (focusRequested) {
			targetWindow.toFront();
			if (targetComponent != targetWindow && targetComponent.isFocusable()) targetComponent
				.requestFocusInWindow();
			else targetWindow.requestFocus();
		}
		final Rectangle bounds = screenBounds(targetComponent);
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0) throw new IllegalStateException(
			"Target component has no usable screen bounds");
		return new CaptureTarget(bounds, describeWindow(targetWindow), targetComponent ==
			targetWindow ? null : describeControl(
				targetComponent, normalize(componentPath)), previousWindow, previousFocusOwner,
			focusRequested);
	}

	private static boolean restoreFocus(final CaptureTarget target) {
		if (target.previousWindow == null || !target.previousWindow.isShowing()) return false;
		target.previousWindow.toFront();
		boolean focused = false;
		if (target.previousFocusOwner != null && target.previousFocusOwner.isShowing()) focused = target
			.previousFocusOwner.requestFocusInWindow();
		if (!focused) target.previousWindow.requestFocus();
		return focused || KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getActiveWindow() == target.previousWindow;
	}

	private static Rectangle screenBounds(final Component component) {
		try {
			final Point location = component.getLocationOnScreen();
			return new Rectangle(location.x, location.y, component.getWidth(), component
				.getHeight());
		}
		catch (final IllegalComponentStateException e) {
			return null;
		}
	}

	private static byte[] encodePng(final BufferedImage image) throws IOException {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (!ImageIO.write(image, "png", output)) throw new IOException(
			"No PNG writer is available");
		return output.toByteArray();
	}

	private static WindowInfo describeWindow(final Window window) {
		final Rectangle bounds = screenBounds(window);
		final Window owner = window.getOwner();
		final String modalityType = window instanceof Dialog dialog ? dialog
			.getModalityType().name() : "";
		return new WindowInfo(windowTitle(window), window.getClass().getName(), window
			instanceof Dialog ? "dialog" : window instanceof Frame ? "frame" : "window",
			window.isVisible(), window.isShowing(), window.isActive(), window.isFocused(),
			window instanceof Dialog dialog && dialog.isModal(), modalityType, owner == null ?
			"" : windowTitle(owner), bounds);
	}

	private static String windowTitle(final Window window) {
		if (window instanceof Frame frame) return normalize(frame.getTitle());
		if (window instanceof Dialog dialog) return normalize(dialog.getTitle());
		return normalize(window.getName());
	}

	private static String selectedItem(final Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static String normalize(final String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * Clicks an exact, visible button in the unique visible dialog with the given
	 * title.
	 *
	 * @throws IllegalArgumentException if either argument is {@code null}
	 * @throws IllegalStateException if the dialog or button is missing, ambiguous,
	 *         or disabled
	 */
	public static DialogResponse respondToDialog(final String dialogTitle,
		final String buttonText)
	{
		if (dialogTitle == null || buttonText == null) {
			throw new IllegalArgumentException("Dialog title and button text are required");
		}

		if (SwingUtilities.isEventDispatchThread()) return respondOnEdt(dialogTitle,
			buttonText);

		final DialogResponse[] result = new DialogResponse[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = respondOnEdt(dialogTitle,
				buttonText));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while responding to dialog", e);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException("Could not respond to dialog", e.getCause());
		}
		return result[0];
	}

	/**
	 * Performs a supported interaction on one control selected by its visible window and path.
	 *
	 * @param windowTitle exact title of the visible target window
	 * @param windowClassName optional exact class name to disambiguate windows
	 * @param componentPath exact component path from {@link #getVisibleControls(String, String)}
	 * @param action one of {@code click}, {@code set_text}, {@code set_checked}, or {@code select}
	 * @param text new text for {@code set_text}; {@code null} otherwise
	 * @param checked desired state for {@code set_checked}; {@code null} otherwise
	 * @param index zero-based item or row index for {@code select}; {@code null} otherwise
	 * @return the target control's description after the interaction
	 * @throws IllegalArgumentException if the target or action arguments are missing or invalid
	 * @throws IllegalStateException if the window or component is missing or ambiguous, or the
	 *         target is hidden, disabled, or incompatible with the action
	 */
	public static ControlInfo interactWithControl(final String windowTitle,
		final String windowClassName, final String componentPath, final String action,
		final String text, final Boolean checked, final Integer index)
	{
		if (windowTitle == null || windowTitle.isBlank()) throw new IllegalArgumentException(
			"Window title is required");
		if (componentPath == null || componentPath.isBlank()) throw new IllegalArgumentException(
			"Component path is required");
		if (action == null || action.isBlank()) throw new IllegalArgumentException(
			"Action is required");

		final String normalizedAction = action.trim();
		switch (normalizedAction) {
			case "click":
				if (text != null || checked != null || index != null) throw new IllegalArgumentException(
					"click does not accept text, checked, or index");
				break;
			case "set_text":
				if (text == null || checked != null || index != null) throw new IllegalArgumentException(
					"set_text requires text and does not accept checked or index");
				break;
			case "set_checked":
				if (checked == null || text != null || index != null) throw new IllegalArgumentException(
					"set_checked requires checked and does not accept text or index");
				break;
			case "select":
				if (index == null || text != null || checked != null) throw new IllegalArgumentException(
					"select requires index and does not accept text or checked");
				break;
			default:
				throw new IllegalArgumentException("Unsupported action: " + normalizedAction);
		}

		return invokeOnEdt(() -> {
			final Window window = findVisibleWindow(windowTitle, windowClassName);
			final Component component = componentPath.startsWith("menu_bar/") ?
				findSwingMenuComponent(window, componentPath) : findComponent(window,
					componentPath);
			if (!component.isShowing()) throw new IllegalStateException(
				"Target component is not showing: " + componentPath);
			if (!component.isEnabled()) throw new IllegalStateException(
				"Target component is disabled: " + componentPath);

			switch (normalizedAction) {
				case "click":
					if (buttonInfo(component) == null) throw new IllegalStateException(
						"Target component is not a supported button: " + componentPath);
					clickButton(component);
					break;
				case "set_text":
					setControlText(component, text);
					break;
				case "set_checked":
					setControlChecked(component, checked);
					break;
				case "select":
					selectControlItem(component, index);
					break;
				default:
					throw new IllegalArgumentException("Unsupported action: " + normalizedAction);
			}
			return describeControl(component, normalize(componentPath));
		});
	}

	private static void setControlText(final Component component, final String text) {
		if (component instanceof JPasswordField || component instanceof TextField textField &&
			textField.getEchoChar() != '\0') throw new IllegalStateException(
				"Setting password fields is not supported");
		if (component instanceof JTextComponent textComponent) {
			if (!textComponent.isEditable()) throw new IllegalStateException(
				"Target text component is not editable");
			textComponent.setText(text);
			return;
		}
		if (component instanceof TextComponent textComponent) {
			if (!textComponent.isEditable()) throw new IllegalStateException(
				"Target text component is not editable");
			textComponent.setText(text);
			return;
		}
		throw new IllegalStateException("Target component is not an editable text field or area");
	}

	private static void setControlChecked(final Component component,
		final boolean checked)
	{
		if (component instanceof Checkbox checkbox) {
			checkbox.setState(checked);
			return;
		}
		if (component instanceof JCheckBoxMenuItem checkBoxMenuItem) {
			if (checkBoxMenuItem.isSelected() != checked) checkBoxMenuItem.doClick();
			return;
		}
		if (component instanceof JToggleButton toggleButton && !(component instanceof
			JRadioButton))
		{
			if (toggleButton.isSelected() != checked) toggleButton.doClick();
			return;
		}
		throw new IllegalStateException("Target component is not a checkbox or toggle button");
	}

	private static void selectControlItem(final Component component, final int index) {
		if (component instanceof JComboBox<?> comboBox) {
			validateSelectionIndex(index, comboBox.getItemCount(), "combo box item");
			comboBox.setSelectedIndex(index);
			return;
		}
		if (component instanceof Choice choice) {
			validateSelectionIndex(index, choice.getItemCount(), "choice item");
			choice.select(index);
			return;
		}
		if (component instanceof JList<?> list) {
			validateSelectionIndex(index, list.getModel().getSize(), "list item");
			list.setSelectedIndex(index);
			list.ensureIndexIsVisible(index);
			return;
		}
		if (component instanceof JTable table) {
			validateSelectionIndex(index, table.getRowCount(), "table row");
			if (!table.getRowSelectionAllowed()) throw new IllegalStateException(
				"Target table does not allow row selection");
			table.clearSelection();
			table.addRowSelectionInterval(index, index);
			if (table.getColumnCount() > 0) table.scrollRectToVisible(table.getCellRect(index, 0,
				true));
			return;
		}
		if (component instanceof JTree tree) {
			validateSelectionIndex(index, tree.getRowCount(), "visible tree row");
			tree.setSelectionRow(index);
			tree.scrollRowToVisible(index);
			return;
		}
		throw new IllegalStateException(
			"Target component does not support indexed selection");
	}

	private static void validateSelectionIndex(final int index, final int size,
		final String itemDescription)
	{
		if (index < 0 || index >= size) throw new IllegalArgumentException(
			"index must be between 0 and " + (size - 1) + " for the target " +
				itemDescription);
	}

	/**
	 * Requests the normal window-manager close behavior for the unique visible
	 * dialog with the given title.
	 *
	 * @throws IllegalArgumentException if the title is {@code null}
	 * @throws IllegalStateException if the dialog is missing or ambiguous
	 */
	public static DialogCloseResponse closeDialog(final String dialogTitle) {
		if (dialogTitle == null) throw new IllegalArgumentException(
			"Dialog title is required");

		if (SwingUtilities.isEventDispatchThread()) return closeOnEdt(dialogTitle);

		final DialogCloseResponse[] result = new DialogCloseResponse[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = closeOnEdt(dialogTitle));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while closing dialog", e);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException("Could not close dialog", e.getCause());
		}
		return result[0];
	}

	/**
	 * Requests normal window-manager close handling for one visible frame.
	 *
	 * @param windowTitle exact title of the visible frame
	 * @param windowClassName optional exact class name to disambiguate frames
	 * @return the frame description and whether it remains visible after the request
	 * @throws IllegalArgumentException if the title is null or blank
	 * @throws IllegalStateException if the frame is missing, ambiguous, or the target is not a frame
	 */
	public static WindowCloseResponse closeWindow(final String windowTitle,
		final String windowClassName)
	{
		if (windowTitle == null || windowTitle.isBlank()) throw new IllegalArgumentException(
			"Window title is required");

		return invokeOnEdt(() -> {
			final Window window = findVisibleWindow(windowTitle, windowClassName);
			if (!(window instanceof Frame)) throw new IllegalStateException(
				"Target window is not a frame; use fiji_ui_dialog_close for dialogs");
			final String title = windowTitle(window);
			final String className = window.getClass().getName();
			window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
			return new WindowCloseResponse(title, className, window.isShowing());
		});
	}

	private static DialogResponse respondOnEdt(final String dialogTitle,
		final String buttonText)
	{
		final Dialog dialog = findVisibleDialog(dialogTitle);
		final List<Component> matchingButtons = new ArrayList<>();
		collectMatchingButtons(dialog, buttonText, matchingButtons, Collections
			.newSetFromMap(new IdentityHashMap<>()));
		if (matchingButtons.isEmpty()) {
			throw new IllegalStateException("No button has text '" + buttonText +
				"' in dialog: " + dialogTitle);
		}
		if (matchingButtons.size() > 1) {
			throw new IllegalStateException("Multiple buttons have text '" + buttonText +
				"' in dialog: " + dialogTitle);
		}

		final Component button = matchingButtons.get(0);
		if (!button.isShowing()) {
			throw new IllegalStateException("Button is not showing: " + buttonText);
		}
		if (!button.isEnabled()) {
			throw new IllegalStateException("Button is disabled: " + buttonText);
		}

		final ButtonInfo buttonInfo = buttonInfo(button);
		clickButton(button);
		return new DialogResponse(dialog.getTitle(), dialog.getClass().getName(),
			buttonInfo.getText(), buttonInfo.getActionCommand(), dialog.isVisible());
	}

	private static DialogCloseResponse closeOnEdt(final String dialogTitle) {
		final Dialog dialog = findVisibleDialog(dialogTitle);
		dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
		return new DialogCloseResponse(dialog.getTitle(), dialog.getClass().getName(), dialog
			.isVisible());
	}

	private static Dialog findVisibleDialog(final String dialogTitle) {
		final List<Dialog> matchingDialogs = new ArrayList<>();
		for (final Window window : Window.getWindows()) {
			if (window instanceof Dialog dialog && dialog.isVisible() && dialogTitle
				.equals(dialog.getTitle())) matchingDialogs.add(dialog);
		}

		if (matchingDialogs.isEmpty()) {
			throw new IllegalStateException("No visible dialog has title: " + dialogTitle);
		}
		if (matchingDialogs.size() > 1) {
			throw new IllegalStateException("Multiple visible dialogs have title: " +
				dialogTitle);
		}
		return matchingDialogs.get(0);
	}

	private static void collectMatchingButtons(final Component component,
		final String buttonText, final List<Component> matchingButtons,
		final Set<Component> visited)
	{
		if (!visited.add(component)) return;

		final ButtonInfo button = buttonInfo(component);
		if (button != null && buttonText.equals(button.getText())) matchingButtons.add(
			component);

		if (component instanceof Container container) {
			for (final Component child : container.getComponents()) {
				collectMatchingButtons(child, buttonText, matchingButtons, visited);
			}
		}
	}

	private static void clickButton(final Component component) {
		if (component instanceof Button button) {
			button.dispatchEvent(new ActionEvent(button, ActionEvent.ACTION_PERFORMED,
				button.getActionCommand()));
		}
		else if (component instanceof AbstractButton button) {
			button.doClick();
		}
		else {
			throw new IllegalStateException("Unsupported dialog button component: " +
				component.getClass().getName());
		}
	}

	private static List<DialogInfo> collectVisibleDialogs() {
		final List<DialogInfo> dialogs = new ArrayList<>();
		for (final Window window : Window.getWindows()) {
			if (!(window instanceof Dialog dialog) || !dialog.isVisible()) continue;
			dialogs.add(describe(dialog));
		}
		return Collections.unmodifiableList(dialogs);
	}

	private static DialogInfo describe(final Dialog dialog) {
		final List<String> messages = new ArrayList<>();
		final List<ButtonInfo> buttons = new ArrayList<>();
		final Set<Component> visited = Collections.newSetFromMap(
			new IdentityHashMap<>());
		collectComponents(dialog, messages, buttons, visited);

		final Window owner = dialog.getOwner();
		return new DialogInfo(dialog.getTitle(), dialog.getClass().getName(), dialog
			.isVisible(), dialog.isActive(), dialog.isModal(), dialog.getModalityType()
			.name(), owner == null ? "" : owner.getName(), messages, buttons);
	}

	private static void collectComponents(final Component component,
		final List<String> messages, final List<ButtonInfo> buttons,
		final Set<Component> visited)
	{
		if (!visited.add(component)) return;

		final String message = messageText(component);
		if (message != null && !message.isBlank() && !messages.contains(message)) {
			messages.add(message);
		}

		final ButtonInfo button = buttonInfo(component);
		if (button != null) buttons.add(button);

		if (component instanceof Container container) {
			for (final Component child : container.getComponents()) {
				collectComponents(child, messages, buttons, visited);
			}
		}
	}

	private static String messageText(final Component component) {
		if (component instanceof Label label) return label.getText();
		if (component instanceof JLabel label) return label.getText();
		if (component instanceof TextArea textArea && !textArea.isEditable()) {
			return textArea.getText();
		}
		if (component instanceof JTextComponent textComponent && !textComponent
			.isEditable()) return textComponent.getText();
		if ("ij.gui.MultiLineLabel".equals(component.getClass().getName())) {
			return multiLineLabelText(component);
		}
		return null;
	}

	private static String multiLineLabelText(final Component component) {
		for (Class<?> type = component.getClass(); type != null; type = type
			.getSuperclass())
		{
			try {
				final Field linesField = type.getDeclaredField("lines");
				if (!linesField.trySetAccessible()) return null;
				final Object lines = linesField.get(component);
				if (!(lines instanceof String[] strings)) return null;
				return String.join("\n", strings);
			}
			catch (NoSuchFieldException e) {
				// Check the next superclass.
			}
			catch (ReflectiveOperationException | RuntimeException e) {
				return null;
			}
		}
		return null;
	}

	private static ButtonInfo buttonInfo(final Component component) {
		if (component instanceof Button button) {
			return new ButtonInfo(button.getLabel(), button.getActionCommand(), button
				.isEnabled(), button.isVisible());
		}
		if (component instanceof AbstractButton button) {
			return new ButtonInfo(button.getText(), button.getActionCommand(), button
				.isEnabled(), button.isVisible());
		}
		return null;
	}

	@FunctionalInterface
	private interface EdtOperation<T> {

		T run();
	}

	private static final class CaptureTarget {

		private final Rectangle bounds;
		private final WindowInfo windowInfo;
		private final ControlInfo controlInfo;
		private final Window previousWindow;
		private final Component previousFocusOwner;
		private final boolean focusRequested;

		private CaptureTarget(final Rectangle bounds, final WindowInfo windowInfo,
			final ControlInfo controlInfo, final Window previousWindow,
			final Component previousFocusOwner, final boolean focusRequested)
		{
			this.bounds = new Rectangle(bounds);
			this.windowInfo = windowInfo;
			this.controlInfo = controlInfo;
			this.previousWindow = previousWindow;
			this.previousFocusOwner = previousFocusOwner;
			this.focusRequested = focusRequested;
		}
	}

	public static final class WindowInfo {

		private final String title;
		private final String className;
		private final String type;
		private final boolean visible;
		private final boolean showing;
		private final boolean active;
		private final boolean focused;
		private final boolean modal;
		private final String modalityType;
		private final String ownerTitle;
		private final Rectangle bounds;

		private WindowInfo(final String title, final String className, final String type,
			final boolean visible, final boolean showing, final boolean active,
			final boolean focused, final boolean modal, final String modalityType,
			final String ownerTitle, final Rectangle bounds)
		{
			this.title = title;
			this.className = className;
			this.type = type;
			this.visible = visible;
			this.showing = showing;
			this.active = active;
			this.focused = focused;
			this.modal = modal;
			this.modalityType = modalityType;
			this.ownerTitle = ownerTitle;
			this.bounds = bounds == null ? null : new Rectangle(bounds);
		}

		public String getTitle() {
			return title;
		}

		public String getClassName() {
			return className;
		}

		public String getType() {
			return type;
		}

		public boolean isVisible() {
			return visible;
		}

		public boolean isShowing() {
			return showing;
		}

		public boolean isActive() {
			return active;
		}

		public boolean isFocused() {
			return focused;
		}

		public boolean isModal() {
			return modal;
		}

		public String getModalityType() {
			return modalityType;
		}

		public String getOwnerTitle() {
			return ownerTitle;
		}

		public Rectangle getBounds() {
			return bounds == null ? null : new Rectangle(bounds);
		}
	}

	public static final class ControlInfo {

		private final String path;
		private final String role;
		private final String className;
		private final String text;
		private final String value;
		private final Boolean selected;
		private final Boolean editable;
		private final boolean enabled;
		private final boolean visible;
		private final boolean showing;
		private final boolean valueRedacted;
		private final String actionCommand;
		private final Rectangle bounds;
		private final Map<String, Object> data;

		private ControlInfo(final String path, final String role,
			final String className, final String text, final String value,
			final Boolean selected, final Boolean editable, final boolean enabled,
			final boolean visible, final boolean showing, final boolean valueRedacted,
			final String actionCommand, final Rectangle bounds)
		{
			this(path, role, className, text, value, selected, editable, enabled, visible,
				showing, valueRedacted, actionCommand, bounds, Collections.emptyMap());
		}

		private ControlInfo(final String path, final String role,
			final String className, final String text, final String value,
			final Boolean selected, final Boolean editable, final boolean enabled,
			final boolean visible, final boolean showing, final boolean valueRedacted,
			final String actionCommand, final Rectangle bounds,
			final Map<String, Object> data)
		{
			this.path = path;
			this.role = role;
			this.className = className;
			this.text = text;
			this.value = value;
			this.selected = selected;
			this.editable = editable;
			this.enabled = enabled;
			this.visible = visible;
			this.showing = showing;
			this.valueRedacted = valueRedacted;
			this.actionCommand = actionCommand;
			this.bounds = bounds == null ? null : new Rectangle(bounds);
			this.data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
		}

		public String getPath() {
			return path;
		}

		public String getRole() {
			return role;
		}

		public String getClassName() {
			return className;
		}

		public String getText() {
			return text;
		}

		public String getValue() {
			return value;
		}

		public Boolean isSelected() {
			return selected;
		}

		public Boolean isEditable() {
			return editable;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean isVisible() {
			return visible;
		}

		public boolean isShowing() {
			return showing;
		}

		public boolean isValueRedacted() {
			return valueRedacted;
		}

		public String getActionCommand() {
			return actionCommand;
		}

		public Rectangle getBounds() {
			return bounds == null ? null : new Rectangle(bounds);
		}

		Map<String, Object> getData() {
			return data;
		}
	}

	public static final class ScreenshotResult {

		private final byte[] pngBytes;
		private final WindowInfo windowInfo;
		private final ControlInfo controlInfo;
		private final Rectangle bounds;
		private final Window previousWindow;
		private final Component previousFocusOwner;
		private final boolean focusRequested;
		private final boolean restored;

		private ScreenshotResult(final byte[] pngBytes, final WindowInfo windowInfo,
			final ControlInfo controlInfo, final Rectangle bounds,
			final Window previousWindow, final Component previousFocusOwner,
			final boolean focusRequested, final boolean restored)
		{
			this.pngBytes = pngBytes.clone();
			this.windowInfo = windowInfo;
			this.controlInfo = controlInfo;
			this.bounds = new Rectangle(bounds);
			this.previousWindow = previousWindow;
			this.previousFocusOwner = previousFocusOwner;
			this.focusRequested = focusRequested;
			this.restored = restored;
		}

		public byte[] getPngBytes() {
			return pngBytes.clone();
		}

		public WindowInfo getWindowInfo() {
			return windowInfo;
		}

		public ControlInfo getControlInfo() {
			return controlInfo;
		}

		public Rectangle getBounds() {
			return new Rectangle(bounds);
		}

		public boolean isFocusRequested() {
			return focusRequested;
		}

		public String getPreviousWindowTitle() {
			return previousWindow == null ? "" : windowTitle(previousWindow);
		}

		public String getPreviousWindowClassName() {
			return previousWindow == null ? "" : previousWindow.getClass().getName();
		}

		public String getPreviousFocusOwnerClassName() {
			return previousFocusOwner == null ? "" : previousFocusOwner.getClass().getName();
		}

		public String getPreviousFocusOwnerName() {
			return previousFocusOwner == null ? "" : normalize(previousFocusOwner.getName());
		}

		public boolean isRestored() {
			return restored;
		}
	}

	public static final class DialogInfo {

		private final String title;
		private final String className;
		private final boolean visible;
		private final boolean active;
		private final boolean modal;
		private final String modalityType;
		private final String ownerName;
		private final List<String> messages;
		private final List<ButtonInfo> buttons;

		private DialogInfo(final String title, final String className,
			final boolean visible, final boolean active, final boolean modal,
			final String modalityType, final String ownerName,
			final List<String> messages, final List<ButtonInfo> buttons)
		{
			this.title = title == null ? "" : title;
			this.className = className;
			this.visible = visible;
			this.active = active;
			this.modal = modal;
			this.modalityType = modalityType;
			this.ownerName = ownerName == null ? "" : ownerName;
			this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
			this.buttons = Collections.unmodifiableList(new ArrayList<>(buttons));
		}

		public String getTitle() {
			return title;
		}

		public String getClassName() {
			return className;
		}

		public boolean isVisible() {
			return visible;
		}

		public boolean isActive() {
			return active;
		}

		public boolean isModal() {
			return modal;
		}

		public String getModalityType() {
			return modalityType;
		}

		public String getOwnerName() {
			return ownerName;
		}

		public List<String> getMessages() {
			return messages;
		}

		public List<ButtonInfo> getButtons() {
			return buttons;
		}
	}

	public static final class ButtonInfo {

		private final String text;
		private final String actionCommand;
		private final boolean enabled;
		private final boolean visible;

		private ButtonInfo(final String text, final String actionCommand,
			final boolean enabled, final boolean visible)
		{
			this.text = text == null ? "" : text;
			this.actionCommand = actionCommand == null ? "" : actionCommand;
			this.enabled = enabled;
			this.visible = visible;
		}

		public String getText() {
			return text;
		}

		public String getActionCommand() {
			return actionCommand;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean isVisible() {
			return visible;
		}
	}

	public static final class DialogResponse {

		private final String dialogTitle;
		private final String dialogClassName;
		private final String buttonText;
		private final String actionCommand;
		private final boolean dialogVisibleAfter;

		private DialogResponse(final String dialogTitle, final String dialogClassName,
			final String buttonText, final String actionCommand,
			final boolean dialogVisibleAfter)
		{
			this.dialogTitle = dialogTitle == null ? "" : dialogTitle;
			this.dialogClassName = dialogClassName == null ? "" : dialogClassName;
			this.buttonText = buttonText == null ? "" : buttonText;
			this.actionCommand = actionCommand == null ? "" : actionCommand;
			this.dialogVisibleAfter = dialogVisibleAfter;
		}

		public String getDialogTitle() {
			return dialogTitle;
		}

		public String getDialogClassName() {
			return dialogClassName;
		}

		public String getButtonText() {
			return buttonText;
		}

		public String getActionCommand() {
			return actionCommand;
		}

		public boolean isDialogVisibleAfter() {
			return dialogVisibleAfter;
		}
	}

	public static final class DialogCloseResponse {

		private final String dialogTitle;
		private final String dialogClassName;
		private final boolean dialogVisibleAfter;

		private DialogCloseResponse(final String dialogTitle,
			final String dialogClassName, final boolean dialogVisibleAfter)
		{
			this.dialogTitle = dialogTitle == null ? "" : dialogTitle;
			this.dialogClassName = dialogClassName == null ? "" : dialogClassName;
			this.dialogVisibleAfter = dialogVisibleAfter;
		}

		public String getDialogTitle() {
			return dialogTitle;
		}

		public String getDialogClassName() {
			return dialogClassName;
		}

		public boolean isDialogVisibleAfter() {
			return dialogVisibleAfter;
		}
	}

	public static final class WindowCloseResponse {

		private final String windowTitle;
		private final String windowClassName;
		private final boolean windowVisibleAfter;

		private WindowCloseResponse(final String windowTitle, final String windowClassName,
			final boolean windowVisibleAfter)
		{
			this.windowTitle = windowTitle == null ? "" : windowTitle;
			this.windowClassName = windowClassName == null ? "" : windowClassName;
			this.windowVisibleAfter = windowVisibleAfter;
		}

		public String getWindowTitle() {
			return windowTitle;
		}

		public String getWindowClassName() {
			return windowClassName;
		}

		public boolean isWindowVisibleAfter() {
			return windowVisibleAfter;
		}
	}
}
