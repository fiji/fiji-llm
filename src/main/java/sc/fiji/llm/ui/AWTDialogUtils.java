/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
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

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/** Utilities for inspecting and responding to visible AWT and Swing dialogs. */
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
