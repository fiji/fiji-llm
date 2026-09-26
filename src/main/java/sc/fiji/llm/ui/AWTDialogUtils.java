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
import java.awt.CheckboxMenuItem;
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
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.TextComponent;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/** Utilities for inspecting and responding to visible AWT and Swing UI. */
public final class AWTDialogUtils {

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

	/** Captures a visible window or component, optionally restoring focus afterward. */
	public static ScreenshotResult captureScreenshot(final String windowTitle,
		final String windowClassName, final String componentPath,
		final boolean activateAndRestore)
	{
		final CaptureTarget target = invokeOnEdt(() -> prepareCapture(windowTitle,
			windowClassName, componentPath, activateAndRestore));
		byte[] pngBytes;
		boolean restored = !activateAndRestore;
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
			if (activateAndRestore) restored = invokeOnEdt(() -> restoreFocus(target));
		}
		return new ScreenshotResult(pngBytes, target.windowInfo, target.controlInfo,
			target.bounds, target.previousWindow, target.previousFocusOwner, restored);
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

	private static List<ControlInfo> collectControls(final Window window) {
		final List<ControlInfo> controls = new ArrayList<>();
		collectControls(window, "root", controls, Collections.newSetFromMap(
			new IdentityHashMap<>()));
		if (window instanceof Frame frame && frame.getMenuBar() != null) {
			collectMenuControls(frame.getMenuBar(), "menu_bar", controls);
		}
		return Collections.unmodifiableList(controls);
	}

	private static void collectControls(final Component component, final String path,
		final List<ControlInfo> controls, final Set<Component> visited)
	{
		if (!visited.add(component)) return;
		if (component.isVisible()) {
			final ControlInfo control = describeControl(component, path);
			if (control != null) controls.add(control);
		}
		if (component instanceof Container container) {
			final Component[] children = container.getComponents();
			for (int i = 0; i < children.length; i++) {
				collectControls(children[i], path + "/child[" + i + "]", controls, visited);
			}
		}
	}

	private static void collectMenuControls(final Menu menu, final String path,
		final List<ControlInfo> controls)
	{
		for (int i = 0; i < menu.getItemCount(); i++) {
			final MenuItem item = menu.getItem(i);
			if (item == null) continue;
			final String itemPath = path + "/item[" + i + "]";
			final String role = item instanceof Menu ? "menu" : "menu_item";
			final Boolean selected = item instanceof CheckboxMenuItem checkbox ?
				checkbox.getState() : null;
			controls.add(new ControlInfo(itemPath, role, item.getClass().getName(), item
				.getLabel(), null, selected, null, item.isEnabled(), true, false, false,
				item.getActionCommand(), null));
			if (item instanceof Menu child) collectMenuControls(child, itemPath, controls);
		}
	}

	private static void collectMenuControls(final MenuBar menuBar, final String path,
		final List<ControlInfo> controls)
	{
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			final Menu menu = menuBar.getMenu(i);
			if (menu != null) collectMenuControls(menu, path + "/menu[" + i + "]", controls);
		}
	}

	private static ControlInfo describeControl(final Component component,
		final String path)
	{
		final Rectangle bounds = screenBounds(component);
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
		if (component instanceof Button button) return new ControlInfo(path, "button",
			component.getClass().getName(), button.getLabel(), null, null, null, button
				.isEnabled(), button.isVisible(), button.isShowing(), false, button
				.getActionCommand(), bounds);
		if (component instanceof JComboBox<?> combo) return new ControlInfo(path,
			"combo_box", component.getClass().getName(), null, selectedItem(combo
				.getSelectedItem()), null, null, combo.isEnabled(), combo.isVisible(), combo
				.isShowing(), false, null, bounds);
		if (component instanceof Choice choice) return new ControlInfo(path,
			"choice", component.getClass().getName(), null, choice.getSelectedItem(), null,
			null, choice.isEnabled(), choice.isVisible(), choice.isShowing(), false, null,
			bounds);
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
		return new ControlInfo(path, role, button.getClass().getName(), button.getText(),
			null, hasSelectedState ? button.isSelected() : null, null, button.isEnabled(),
			button.isVisible(), button.isShowing(), false, button.getActionCommand(), bounds);
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
		final String windowClassName, final String componentPath,
		final boolean activateAndRestore)
	{
		final Window targetWindow = findVisibleWindow(windowTitle, windowClassName);
		final Component targetComponent = findComponent(targetWindow, componentPath);
		if (!targetComponent.isShowing()) throw new IllegalStateException(
			"Target component is not showing: " + normalize(componentPath));
		final Window previousWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getActiveWindow();
		final Component previousFocusOwner = KeyboardFocusManager
			.getCurrentKeyboardFocusManager().getFocusOwner();
		if (activateAndRestore) {
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
				targetComponent, normalize(componentPath)), previousWindow, previousFocusOwner);
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

	private static DialogResponse respondOnEdt(final String dialogTitle,
		final String buttonText)
	{
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

		final Dialog dialog = matchingDialogs.get(0);
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

		private CaptureTarget(final Rectangle bounds, final WindowInfo windowInfo,
			final ControlInfo controlInfo, final Window previousWindow,
			final Component previousFocusOwner)
		{
			this.bounds = new Rectangle(bounds);
			this.windowInfo = windowInfo;
			this.controlInfo = controlInfo;
			this.previousWindow = previousWindow;
			this.previousFocusOwner = previousFocusOwner;
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

		private ControlInfo(final String path, final String role,
			final String className, final String text, final String value,
			final Boolean selected, final Boolean editable, final boolean enabled,
			final boolean visible, final boolean showing, final boolean valueRedacted,
			final String actionCommand, final Rectangle bounds)
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
	}

	public static final class ScreenshotResult {

		private final byte[] pngBytes;
		private final WindowInfo windowInfo;
		private final ControlInfo controlInfo;
		private final Rectangle bounds;
		private final Window previousWindow;
		private final Component previousFocusOwner;
		private final boolean restored;

		private ScreenshotResult(final byte[] pngBytes, final WindowInfo windowInfo,
			final ControlInfo controlInfo, final Rectangle bounds,
			final Window previousWindow, final Component previousFocusOwner,
			final boolean restored)
		{
			this.pngBytes = pngBytes.clone();
			this.windowInfo = windowInfo;
			this.controlInfo = controlInfo;
			this.bounds = new Rectangle(bounds);
			this.previousWindow = previousWindow;
			this.previousFocusOwner = previousFocusOwner;
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
}
