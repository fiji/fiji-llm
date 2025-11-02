package sc.fiji.llm.ui;

import javax.swing.JTextField;

/**
 * A JTextField that displays placeholder text when empty and unfocused.
 * The placeholder text is drawn in pale gray and disappears when the user
 * starts typing or focuses the field.
 */
public class PlaceholderTextField extends JTextField {

	private final String placeholder;

	public PlaceholderTextField(final String placeholder) {
		this.placeholder = placeholder;
	}

	@Override
	protected void paintComponent(final java.awt.Graphics g) {
		super.paintComponent(g);

		if (getText().isEmpty() && !hasFocus()) {
			g.setColor(java.awt.Color.GRAY);
			g.setFont(getFont());
			final int padding = getInsets().left;
			final int height = (getHeight() + g.getFontMetrics().getAscent()) / 2;
			g.drawString(placeholder, padding, height);
		}
	}
}
