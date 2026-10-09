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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Interactive guide system for the Fiji Chat UI. Displays a tour through UI
 * elements with explanatory dialogs.
 */
public class InteractiveGuide {

	private static final int GUIDE_BORDER_WIDTH = 3;
	private static final Color INSPECT_BORDER_COLOR = new Color(255, 165, 0,
		100);
	private static final String INSPECT_MODE_DESCRIPTION =
		"Click on a highlighted Fiji Chat component to learn more about it. " +
			"Escape returns.";

	private final JFrame parentFrame;
	private final List<GuideElement> elements;
	private final JPanel inspectGlassPane;
	private final Map<Component, GuideElement> inspectTargets = new HashMap<>();
	private final Map<JComponent, Border> guideOriginalBorders = new HashMap<>();
	private Consumer<Boolean> inspectModeChangeListener;
	private Component inspectModeComponent;
	private Component messagePanel;
	private int largestGuideTitleWidth;
	private int currentIndex = 0;
	private JDialog currentDialog;
	private Timer flashTimer;
	private boolean isActive = false;
	private JComponent currentFlashingComponent;
	private Border currentOriginalBorder;
	private boolean inspectMode = false;
	private final Map<JComponent, Border> inspectOriginalBorders = new HashMap<>();

	public InteractiveGuide(JFrame parentFrame) {
		this.parentFrame = parentFrame;
		this.elements = new ArrayList<>();
		this.inspectGlassPane = new JPanel();
		inspectGlassPane.setOpaque(false);
		inspectGlassPane.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent event) {
				handleInspectClick(event);
			}

			@Override
			public void mouseReleased(final MouseEvent event) {
				event.consume();
			}

			@Override
			public void mouseClicked(final MouseEvent event) {
				event.consume();
			}
		});
		parentFrame.setGlassPane(inspectGlassPane);
		installEscapeBinding(parentFrame.getRootPane());
	}

	/**
	 * Add an element to the guide tour.
	 */
	public void addElement(Component component, String title,
		String description)
	{
		final GuideElement element = new GuideElement(component, title, description);
		elements.add(element);
		largestGuideTitleWidth = Math.max(largestGuideTitleWidth,
			getGuideTitleWidth(title));
		reserveGuideBorder(component);
		if (inspectMode && component != inspectModeComponent) {
			installInspectBorder(component);
			registerInspectTargets(component, element);
		}
	}

	/**
	 * Set the component that separates the upper toolbar from the message input.
	 *
	 * @param messagePanel the chat display component
	 */
	public void setMessagePanel(final Component messagePanel) {
		this.messagePanel = messagePanel;
	}

	/**
	 * Exclude the inspect-mode control from inspect highlighting while retaining
	 * it as a regular guide element.
	 *
	 * @param component the inspect-mode control
	 */
	public void setInspectModeComponent(final Component component) {
		inspectModeComponent = component;
	}

	/**
	 * Listen for inspect-mode state changes.
	 *
	 * @param listener receives the new inspect-mode state
	 */
	public void setInspectModeChangeListener(final Consumer<Boolean> listener) {
		inspectModeChangeListener = listener;
		if (listener != null) listener.accept(inspectMode);
	}

	/**
	 * Toggle the inspect view.
	 */
	public void toggleInspectMode() {
		setInspectMode(!inspectMode);
	}

	/**
	 * Enable or disable the inspect view.
	 *
	 * @param enabled whether inspect mode should be active
	 */
	public void setInspectMode(final boolean enabled) {
		if (inspectMode == enabled) return;

		closeCurrentDialog();
		isActive = false;
		inspectMode = enabled;
		if (enabled) {
			inspectGlassPane.setVisible(true);
			for (final GuideElement element : elements) {
				if (element.getComponent() == inspectModeComponent) continue;
				installInspectBorder(element.getComponent());
				registerInspectTargets(element.getComponent(), element);
			}
			if (inspectModeComponent != null) {
				showSingleton(inspectModeComponent, "Inspect Mode",
					INSPECT_MODE_DESCRIPTION);
			}
		}
		else {
			inspectGlassPane.setVisible(false);
			inspectTargets.clear();
			restoreInspectBorders();
		}
		if (inspectModeChangeListener != null) inspectModeChangeListener.accept(
			inspectMode);
	}

	/**
	 * @return whether inspect mode is active
	 */
	public boolean isInspectMode() {
		return inspectMode;
	}

	/**
	 * Start the guide tour, sorting elements by position.
	 */
	public void start() {
		if (elements.isEmpty()) {
			return;
		}

		isActive = true;
		currentIndex = 0;

		// Currently run guide in order steps were added

		showCurrentStep();
	}

	/**
	 * Show the guide dialog for the current step.
	 */
	private void showCurrentStep() {
		if (currentIndex >= elements.size()) {
			finish();
			return;
		}

		final GuideElement element = elements.get(currentIndex);

		// First, create the button panel to get its preferred width
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20,
			0));
		buttonPanel.setOpaque(false);

		// Check if this is the last element
		final boolean isLastElement = currentIndex == elements.size() - 1;

		final JButton nextButton = new JButton(isLastElement ? "Done" : "Next");
		nextButton.addActionListener(e -> {
			if (isLastElement) {
				cancel();
			}
			else {
				nextStep();
			}
		});
		nextButton.setFocusPainted(false);
		nextButton.setContentAreaFilled(false);
		nextButton.setBorder(BorderFactory.createCompoundBorder(new LineBorder(
			Color.GRAY, 1), BorderFactory.createEmptyBorder(4, 12, 4, 12)));
		buttonPanel.add(nextButton);

		final JButton cancelButton = new JButton("Stop");
		cancelButton.addActionListener(e -> cancel());
		cancelButton.setFocusPainted(false);
		cancelButton.setContentAreaFilled(false);
		cancelButton.setBorder(BorderFactory.createCompoundBorder(new LineBorder(
			Color.GRAY, 1), BorderFactory.createEmptyBorder(4, 12, 4, 12)));
		buttonPanel.add(cancelButton);

		buttonPanel.doLayout();
		if (isLastElement) {
			buttonPanel.remove(cancelButton);
		}

		// Title
		final JLabel titleLabel = new JLabel(element.getTitle() + " (" +
			(currentIndex + 1) + " of " + elements.size() + ")");
		titleLabel.setFont(titleLabel.getFont().deriveFont(14f).deriveFont(
			Font.BOLD));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JDialog dialog = createGuideDialog(element, titleLabel, buttonPanel);

		showElement(element, dialog);
	}

	/**
	 * Manually show a guide-style popup for the given element.
	 * Note that this does not retain the element as part of the guide.
	 */
	public void showSingleton(Component component, String title,
		String description)
	{
		GuideElement element = new GuideElement(component, title, description);

		// OK button which simply closes the dialog
		final JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> cancel());
		okButton.setFocusPainted(false);
		okButton.setContentAreaFilled(false);
		okButton.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.GRAY, 1),
			BorderFactory.createEmptyBorder(4, 12, 4, 12)));
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20,
			0));
		buttonPanel.setOpaque(false);
		buttonPanel.add(okButton);

		// Short title (no step counters)
		final JLabel titleLabel = new JLabel(element.getTitle());
		titleLabel.setFont(titleLabel.getFont().deriveFont(13f).deriveFont(Font.BOLD));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		buttonPanel.doLayout();
		JDialog dialog = createGuideDialog(element, titleLabel, buttonPanel);

		showElement(element, dialog);
	}

	/**
	 * Display the given element element
	 */
	private void showElement(GuideElement element, JDialog dialog) {
		final Component component = element.getComponent();

		// Close previous dialog if any
		if (currentDialog != null) {
			currentDialog.dispose();
		}
		resetCurrentComponentBorder();

		// Flash the component border briefly
		flashComponentBorder(component);
		installEscapeBinding(dialog.getRootPane());

		// Create dialog with explanation
		currentDialog = dialog;
		currentDialog.setVisible(true);
	}

	/**
	 * Create a dialog for a guide element.
	 */
	private JDialog createGuideDialog(GuideElement element, JLabel titleLabel,
		JPanel buttonPanel)
	{
		final JDialog dialog = new JDialog(parentFrame, false);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setUndecorated(true);

		// Now create the content panel with proper sizing
		final JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		// Set a warm beige background
		contentPanel.setBackground(new Color(255, 250, 240)); // Warm beige
		contentPanel.setOpaque(true);

		contentPanel.add(titleLabel);
		contentPanel.add(Box.createVerticalStrut(8));

		titleLabel.doLayout();
		final int titleWidth = Math.max(largestGuideTitleWidth, titleLabel
			.getPreferredSize().width);

		// Description - constrained to button panel width
		final JLabel descriptionLabel = new JLabel("<html><div style='width:" +
			titleWidth + "px'>" + element.getDescription() + "</div></html>");
		descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(12f));
		descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		contentPanel.add(descriptionLabel);
		contentPanel.add(Box.createVerticalStrut(12));

		// Add the button panel
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		contentPanel.add(buttonPanel);

		dialog.add(contentPanel);
		dialog.pack();

		// Position dialog near the component if possible, keeping it on-screen
		try {
			final Point componentLocation = element.getComponent()
				.getLocationOnScreen();
			final Toolkit toolkit = Toolkit.getDefaultToolkit();
			final Dimension screenSize = toolkit.getScreenSize();
			final int xOffset = 5;

			final boolean aboveMessagePanel = isAboveMessagePanel(element
				.getComponent());
			int dialogX = componentLocation.x;
			int dialogY = aboveMessagePanel ? componentLocation.y + element
				.getComponent().getHeight() : componentLocation.y - dialog.getHeight();

			if (dialogX + dialog.getWidth() > screenSize.width) {
				dialogX = screenSize.width - dialog.getWidth() - xOffset;
			}
			if (dialogX < 0) dialogX = xOffset;

			// Check if dialog would be off-screen vertically and adjust
			if (dialogY + dialog.getHeight() > screenSize.height) {
				dialogY = screenSize.height - dialog.getHeight() - 10;
			}
			if (dialogY < 0) {
				dialogY = 10;
			}

			dialog.setLocation(dialogX, dialogY);
		}
		catch (Exception e) {
			// Component not yet visible, center dialog instead
			dialog.setLocationRelativeTo(parentFrame);
		}

		// Handle dialog close button
		dialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				cancel();
			}
		});

		return dialog;
	}

	private boolean isAboveMessagePanel(final Component component) {
		if (messagePanel == null) return true;

		try {
			return component.getLocationOnScreen().y < messagePanel
				.getLocationOnScreen().y;
		}
		catch (final IllegalComponentStateException e) {
			return true;
		}
	}

	private static int getGuideTitleWidth(final String title) {
		final JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(titleLabel.getFont().deriveFont(14f).deriveFont(
			Font.BOLD));
		return titleLabel.getPreferredSize().width;
	}

	/**
	 * Flash the border of a component to highlight it.
	 */
	private void flashComponentBorder(Component component) {
		if (!(component instanceof JComponent)) {
			return;
		}

		final JComponent jComponent = (JComponent) component;
		currentFlashingComponent = jComponent;
		currentOriginalBorder = jComponent.getBorder();
		final Color highlightColor = new Color(255, 165, 0, 200); // Semi-transparent
																															// orange

		if (flashTimer != null && flashTimer.isRunning()) {
			flashTimer.stop();
		}

		final int[] flashCount = { 0 };
		final Border originalComponentBorder = guideOriginalBorders.containsKey(
			jComponent) ? guideOriginalBorders.get(jComponent) : currentOriginalBorder;
		Border flashBorder = createGuideBorder(highlightColor,
			originalComponentBorder);
		flashTimer = new Timer(150, e -> {
			if (flashCount[0] % 2 == 0) {
				jComponent.setBorder(flashBorder);
			}
			else {
				jComponent.setBorder(currentOriginalBorder);
			}
			flashCount[0]++;

			if (flashCount[0] >= 7) {
				((Timer) e.getSource()).stop();
				jComponent.setBorder(flashBorder);
			}
		});

		flashTimer.start();
	}

	/**
	 * Advance to the next step in the guide.
	 */
	private void nextStep() {
		// Stop the flash timer if running
		if (flashTimer != null && flashTimer.isRunning()) {
			flashTimer.stop();
		}

		// Reset the border after stopping the timer
		resetCurrentComponentBorder();

		currentIndex++;
		showCurrentStep();
	}

	/**
	 * Cancel the guide tour.
	 */
	private void cancel() {
		resetCurrentComponentBorder();

		if (currentDialog != null) {
			currentDialog.dispose();
			currentDialog = null;
		}

		if (flashTimer != null && flashTimer.isRunning()) {
			flashTimer.stop();
		}

		isActive = false;
	}

	/**
	 * Finish the guide tour.
	 */
	private void finish() {
		if (currentDialog != null) {
			currentDialog.dispose();
			currentDialog = null;
		}

		if (flashTimer != null && flashTimer.isRunning()) {
			flashTimer.stop();
		}
		resetCurrentComponentBorder();

		isActive = false;
	}

	/**
	 * Reset the border of the current component being highlighted.
	 */
	private void resetCurrentComponentBorder() {
		if (currentFlashingComponent != null) {
			currentFlashingComponent.setBorder(currentOriginalBorder);
			currentFlashingComponent = null;
			currentOriginalBorder = null;
		}
	}

	private void closeCurrentDialog() {
		resetCurrentComponentBorder();

		if (currentDialog != null) {
			currentDialog.dispose();
			currentDialog = null;
		}

		if (flashTimer != null && flashTimer.isRunning()) {
			flashTimer.stop();
		}
	}

	private void installInspectBorder(final Component component) {
		if (!(component instanceof JComponent jComponent) ||
			inspectOriginalBorders.containsKey(jComponent)) return;

		final Border reservedBorder = jComponent.getBorder();
		final Border originalBorder = guideOriginalBorders.containsKey(jComponent)
			? guideOriginalBorders.get(jComponent) : reservedBorder;
		inspectOriginalBorders.put(jComponent, reservedBorder);
		jComponent.setBorder(createGuideBorder(INSPECT_BORDER_COLOR,
			originalBorder));
		jComponent.revalidate();
		jComponent.repaint();
	}

	private void reserveGuideBorder(final Component component) {
		if (!(component instanceof JComponent jComponent) ||
			guideOriginalBorders.containsKey(jComponent)) return;

		final Border originalBorder = jComponent.getBorder();
		guideOriginalBorders.put(jComponent, originalBorder);
		jComponent.setBorder(createReservedGuideBorder(originalBorder));
	}

	private static Border createReservedGuideBorder(final Border innerBorder) {
		final Border reservedBorder = new LookAndFeelBorder();
		return innerBorder == null ? reservedBorder : BorderFactory
			.createCompoundBorder(reservedBorder, innerBorder);
	}

	private static final class LookAndFeelBorder extends AbstractBorder {

		@Override
		public void paintBorder(final Component component, final Graphics graphics,
			final int x, final int y, final int width, final int height)
		{
			if (width <= 0 || height <= 0) return;

			final Color borderColor = surroundingBackground(component);
			if (borderColor == null) return;

			final Graphics borderGraphics = graphics.create();
			try {
				borderGraphics.setColor(borderColor);
				final int borderWidth = Math.min(GUIDE_BORDER_WIDTH, Math.min(width,
					height) / 2);
				borderGraphics.fillRect(x, y, width, borderWidth);
				borderGraphics.fillRect(x, y + height - borderWidth, width,
					borderWidth);
				final int innerHeight = height - (2 * borderWidth);
				if (innerHeight > 0) {
					borderGraphics.fillRect(x, y + borderWidth, borderWidth,
						innerHeight);
					borderGraphics.fillRect(x + width - borderWidth, y + borderWidth,
						borderWidth, innerHeight);
				}
			}
			finally {
				borderGraphics.dispose();
			}
		}

		private static Color surroundingBackground(final Component component) {
			Component ancestor = component.getParent();
			while (ancestor != null) {
				if (ancestor instanceof JComponent jComponent && jComponent.isOpaque()) {
					final Color background = jComponent.getBackground();
					if (background != null) return background;
				}
				ancestor = ancestor.getParent();
			}

			return UIManager.getColor("Panel.background");
		}

		@Override
		public Insets getBorderInsets(final Component component,
			final Insets insets)
		{
			insets.top = GUIDE_BORDER_WIDTH;
			insets.left = GUIDE_BORDER_WIDTH;
			insets.bottom = GUIDE_BORDER_WIDTH;
			insets.right = GUIDE_BORDER_WIDTH;
			return insets;
		}
	}

	private Border createGuideBorder(final Color color,
		final Border innerBorder)
	{
		final Border guideBorder = new LineBorder(color, GUIDE_BORDER_WIDTH);
		return innerBorder == null ? guideBorder : BorderFactory.createCompoundBorder(
			guideBorder, innerBorder);
	}

	private void registerInspectTargets(final Component component,
		final GuideElement element)
	{
		inspectTargets.put(component, element);

		if (component instanceof Container container) {
			for (final Component child : container.getComponents()) {
				registerInspectTargets(child, element);
			}
		}
	}

	private void handleInspectClick(final MouseEvent event) {
		event.consume();
		if (!inspectMode) return;

		final Point contentPoint = SwingUtilities.convertPoint(inspectGlassPane,
			event.getPoint(), parentFrame.getContentPane());
		final Component target = SwingUtilities.getDeepestComponentAt(parentFrame
			.getContentPane(), contentPoint.x, contentPoint.y);
		if (isDescendantOf(target, inspectModeComponent)) {
			setInspectMode(false);
			return;
		}

		final GuideElement element = findInspectTarget(target);
		if (element != null) {
			showSingleton(element.getComponent(), element.getTitle(), element
				.getDescription());
		}
	}

	private GuideElement findInspectTarget(Component component) {
		while (component != null) {
			final GuideElement element = inspectTargets.get(component);
			if (element != null) return element;
			component = component.getParent();
		}
		return null;
	}

	private static boolean isDescendantOf(Component component, Component ancestor) {
		while (component != null) {
			if (component == ancestor) return true;
			component = component.getParent();
		}
		return false;
	}

	private void restoreInspectBorders() {
		for (final Map.Entry<JComponent, Border> entry : inspectOriginalBorders
			.entrySet())
		{
			final JComponent component = entry.getKey();
			component.setBorder(entry.getValue());
			component.revalidate();
			component.repaint();
		}
		inspectOriginalBorders.clear();
	}

	private void installEscapeBinding(final JComponent component) {
		final String actionKey = "exitInspectMode";
		component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke
			.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
		final ActionMap actionMap = component.getActionMap();
		actionMap.put(actionKey, new javax.swing.AbstractAction() {

			@Override
			public void actionPerformed(final java.awt.event.ActionEvent event) {
				setInspectMode(false);
			}
		});
	}

	/**
	 * Check if the guide is currently active.
	 */
	public boolean isActive() {
		return isActive;
	}
}
