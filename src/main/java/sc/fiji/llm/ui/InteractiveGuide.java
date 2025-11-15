package sc.fiji.llm.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Interactive guide system for the Fiji Chat UI.
 * Displays a tour through UI elements with explanatory dialogs.
 */
public class InteractiveGuide {
    private final JFrame parentFrame;
    private final List<GuideElement> elements;
    private int currentIndex = 0;
    private JDialog currentDialog;
    private Timer flashTimer;
    private boolean isActive = false;

    public InteractiveGuide(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.elements = new ArrayList<>();
    }

    /**
     * Add an element to the guide tour.
     */
    public void addElement(Component component, String title, String description) {
        elements.add(new GuideElement(component, title, description));
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
     * Show the dialog for the current guide step.
     */
    private void showCurrentStep() {
        if (currentIndex >= elements.size()) {
            finish();
            return;
        }

        final GuideElement element = elements.get(currentIndex);
        final Component component = element.getComponent();

        // Flash the component border briefly
        flashComponentBorder(component);

        // Close previous dialog if any
        if (currentDialog != null) {
            currentDialog.dispose();
        }

        // Create dialog with explanation
        currentDialog = createGuideDialog(element);
        currentDialog.setVisible(true);
    }

    /**
     * Create a dialog for a guide element.
     */
    private JDialog createGuideDialog(GuideElement element) {
        final JDialog dialog = new JDialog(parentFrame, false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setUndecorated(true);

        // First, create the button panel to get its preferred width
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);

        // Check if this is the last element
        final boolean isLastElement = currentIndex == elements.size() - 1;

        final JButton nextButton = new JButton(isLastElement ? "Done" : "Next");
        nextButton.addActionListener(e -> {
            if (isLastElement) {
                cancel();
            } else {
                nextStep();
            }
        });
        nextButton.setFocusPainted(false);
        nextButton.setContentAreaFilled(false);
        nextButton.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        buttonPanel.add(nextButton);

        final JButton cancelButton = new JButton("Stop");
        cancelButton.addActionListener(e -> cancel());
        cancelButton.setFocusPainted(false);
        cancelButton.setContentAreaFilled(false);
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        buttonPanel.add(cancelButton);

        buttonPanel.doLayout();
        final int buttonPanelWidth = buttonPanel.getPreferredSize().width;
        if (isLastElement) {
            buttonPanel.remove(cancelButton);
        }

        // Now create the content panel with proper sizing
        final JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        // Set a warm beige background
        contentPanel.setBackground(new Color(255, 250, 240)); // Warm beige
        contentPanel.setOpaque(true);

        // Title
        final JLabel titleLabel = new JLabel(element.getTitle() + " (" + (currentIndex + 1) + " of " + elements.size() + ")");
        titleLabel.setFont(titleLabel.getFont().deriveFont(14f).deriveFont(Font.BOLD));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(8));

        // Description - constrained to button panel width
        final JLabel descriptionLabel = new JLabel("<html><div style='width:" + buttonPanelWidth + "px'>" + element.getDescription() + "</div></html>");
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
            final Point componentLocation = element.getComponent().getLocationOnScreen();
            final Toolkit toolkit = Toolkit.getDefaultToolkit();
            final Dimension screenSize = toolkit.getScreenSize();

            // Try to place to the right of the component
            int dialogX = componentLocation.x + element.getComponent().getWidth() + 10;
            int dialogY = componentLocation.y;

            // Check if dialog would be off-screen to the right
            if (dialogX + dialog.getWidth() > screenSize.width) {
                // Try placing to the left of the component instead
                dialogX = componentLocation.x - dialog.getWidth() - 10;
                if (dialogX < 0) {
                    // If still off-screen, place it centered
                    dialogX = (screenSize.width - dialog.getWidth()) / 2;
                }
            }

            // Check if dialog would be off-screen vertically and adjust
            if (dialogY + dialog.getHeight() > screenSize.height) {
                dialogY = screenSize.height - dialog.getHeight() - 10;
            }
            if (dialogY < 0) {
                dialogY = 10;
            }

            dialog.setLocation(dialogX, dialogY);
        } catch (Exception e) {
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

    /**
     * Flash the border of a component to highlight it.
     */
    private void flashComponentBorder(Component component) {
        if (!(component instanceof JComponent)) {
            return;
        }

        final JComponent jComponent = (JComponent) component;
        final Border originalBorder = jComponent.getBorder();
        final Color highlightColor = new Color(255, 165, 0, 200); // Semi-transparent orange

        if (flashTimer != null && flashTimer.isRunning()) {
            flashTimer.stop();
        }

        final int[] flashCount = {0};
        flashTimer = new Timer(150, e -> {
            if (flashCount[0] % 2 == 0) {
                jComponent.setBorder(new LineBorder(highlightColor, 3));
            } else {
                jComponent.setBorder(originalBorder);
            }
            flashCount[0]++;

            if (flashCount[0] >= 6) {
                ((Timer) e.getSource()).stop();
                jComponent.setBorder(originalBorder);
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
        if (currentIndex < elements.size()) {
            final GuideElement element = elements.get(currentIndex);
            final Component component = element.getComponent();
            if (component instanceof JComponent) {
                final JComponent jComponent = (JComponent) component;
                jComponent.setBorder(null);
            }
        }
    }

    /**
     * Check if the guide is currently active.
     */
    public boolean isActive() {
        return isActive;
    }
}
