package sc.fiji.llm.ui;

import java.awt.Component;

/**
 * Represents a single element in the interactive guide tour.
 */
public class GuideElement {
    private final Component component;
    private final String title;
    private final String description;

    public GuideElement(Component component, String title, String description) {
        this.component = component;
        this.title = title;
        this.description = description;
    }

    public Component getComponent() {
        return component;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the component's approximate position for sorting.
     * Returns a comparable value based on reading order (top-to-bottom, left-to-right).
     */
    public int getPositionKey() {
        int y = component.getY();
        int x = component.getX();
        // Create a key that sorts by Y first (top-to-bottom), then by X (left-to-right)
        // Divide into horizontal zones to handle left-to-right ordering within similar Y positions
        return (y / 50) * 1000 + (x / 100);
    }
}
