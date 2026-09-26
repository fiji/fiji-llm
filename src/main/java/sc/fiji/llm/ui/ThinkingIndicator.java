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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * An animated status line shown while the assistant works: a rotating star,
 * a rotating whimsical message (or the name of the running tool), and the
 * elapsed time. Must be used on the EDT.
 */
public class ThinkingIndicator extends JLabel {

	private static final String[] STAR_FRAMES = { "·", "✢", "✳", "✶", "✻", "✽",
		"✻", "✶", "✳", "✢" };
	private static final String[] ASCII_FRAMES = { "-", "\\", "|", "/" };
	private static final int FRAME_MS = 120;
	private static final int MESSAGE_MS = 4000;

	private static final String[] MESSAGES = {
		"Accomplishing",
		"Addressing",
		"Adducing",
		"Adjudicating",
		"Adjuring",
		"Adjusting",
		"Analyzing",
		"Appealing",
		"Applying",
		"Beckoning",
		"Believing",
		"Blandishing",
		"Brainstorming",
		"Building",
		"Cajoling",
		"Calculating",
		"Calibrating response",
		"Cerebrating",
		"Coaxing",
		"Conceiving",
		"Conjuring",
		"Considering",
		"Contemplating",
		"Deducing",
		"Deliberating",
		"Determining",
		"Dispatching",
		"Divining",
		"Doing the thing",
		"Effecting",
		"Effectuating",
		"Eliciting",
		"Enacting",
		"Engineering",
		"Enhancing (but ethically)",
		"Entreating",
		"Estimating",
		"Evaluating",
		"Evoking",
		"Executing",
		"Exhorting",
		"Exploring",
		"Finagling",
		"Fomenting",
		"Fostering",
		"Fulfilling",
		"Gathering",
		"Gauging",
		"Generalizing",
		"Guestimating",
		"Hastening",
		"Hypothecating",
		"Hypothesizing",
		"Imagining",
		"Impelling",
		"Impetrating",
		"Implementing",
		"Imploring",
		"Implying",
		"Importuning",
		"Imprecating",
		"Incurring",
		"Inducing",
		"Inferring",
		"Initiating",
		"Inspiring",
		"Intellectualizing",
		"Inveigling",
		"Invocating",
		"Invoking",
		"Kindling",
		"Managing expectations",
		"Managing",
		"Maneuvering",
		"Measuring twice",
		"Measuring",
		"Mediating",
		"Meditating",
		"Motivating",
		"Musing",
		"Obliging",
		"Performing",
		"Philosophizing",
		"Pondering",
		"Positing",
		"Postulating",
		"Pressing",
		"Presupposing",
		"Processing",
		"Procuring",
		"Producing",
		"Quantifying",
		"Realizing",
		"Reasoning",
		"Reckoning",
		"Reflecting",
		"Requesting",
		"Revolving",
		"Rolling the ball",
		"Rousing",
		"Ruminating",
		"Satisfying",
		"Scheming",
		"Segmenting ideas",
		"Stirring",
		"Studying",
		"Summoning",
		"Surmising",
		"Testing",
		"Theorizing",
		"Thinking",
		"Thresholding thoughts",
		"Titrating",
		"Transacting",
		"Trying my best",
		"Understanding",
		"Undertaking",
		"Wangling",
		"Weighing",
		"Wheedling",
		"Winning awards",
		"Wondering",
		"Working diligently",
	};

	private final String[] frames;
	private final List<String> messages = new ArrayList<>(List.of(MESSAGES));
	private final Timer timer;
	private int frame;
	private int messageIndex;
	private long startMillis;
	private long messageMillis;
	private String activity;

	public ThinkingIndicator(final float fontSize) {
		setFont(getFont().deriveFont(Font.ITALIC, fontSize));
		setForeground(new Color(90, 90, 90));
		frames = supportedFrames(getFont());
		setIcon(new StarIcon(getFont().deriveFont(Font.PLAIN)));
		setIconTextGap(6);
		timer = new Timer(FRAME_MS, e -> tick());
	}

	/** Starts the animation, if not already running. */
	public void start() {
		if (timer.isRunning()) return;
		Collections.shuffle(messages);
		messageIndex = 0;
		startMillis = messageMillis = System.currentTimeMillis();
		setVisible(true);
		tick();
		timer.start();
	}

	/** Stops the animation and hides the indicator. */
	public void stop() {
		timer.stop();
		setVisible(false);
	}

	public boolean isRunning() {
		return timer.isRunning();
	}

	/**
	 * Shows a specific activity, such as a running tool, instead of the rotating
	 * messages.
	 *
	 * @param activity the activity, or null to resume the rotating messages
	 */
	public void setActivity(final String activity) {
		this.activity = activity;
		if (isRunning()) tick();
	}

	/** @return the number of seconds since {@link #start()} */
	public long elapsedSeconds() {
		return (System.currentTimeMillis() - startMillis) / 1000;
	}

	/** @return a spinner frame suitable for the given font */
	static String frame(final Font font, final int index) {
		final String[] f = supportedFrames(font);
		return f[Math.floorMod(index, f.length)];
	}

	private void tick() {
		final long now = System.currentTimeMillis();
		if (now - messageMillis >= MESSAGE_MS) {
			messageMillis = now;
			messageIndex = (messageIndex + 1) % messages.size();
		}
		frame = (frame + 1) % frames.length;
		final String message = activity != null ? activity : messages.get(
			messageIndex);
		setText(message + "... (" + elapsedSeconds() + "s)");
		repaint();
	}

	private static String[] supportedFrames(final Font font) {
		return font.canDisplayUpTo(String.join("", STAR_FRAMES)) == -1
			? STAR_FRAMES : ASCII_FRAMES;
	}

	/**
	 * Draws the current star centered in a box as wide as the widest frame, so
	 * the message does not shift as the star rotates. A fixed-width font alone
	 * would not suffice, since the stars come from varying fallback fonts.
	 */
	private class StarIcon implements Icon {

		private final Font font;
		private final int width;
		private final int height;

		StarIcon(final Font font) {
			this.font = font;
			final FontMetrics metrics = getFontMetrics(font);
			int maxWidth = 0;
			for (final String f : frames) {
				maxWidth = Math.max(maxWidth, metrics.stringWidth(f));
			}
			width = maxWidth;
			height = metrics.getHeight();
		}

		@Override
		public void paintIcon(final Component c, final Graphics g, final int x,
			final int y)
		{
			final Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setFont(font);
			g2d.setColor(c.getForeground());
			final FontMetrics metrics = g2d.getFontMetrics();
			final String star = frames[frame];
			g2d.drawString(star, x + (width - metrics.stringWidth(star)) / 2, y +
				metrics.getAscent());
			g2d.dispose();
		}

		@Override
		public int getIconWidth() {
			return width;
		}

		@Override
		public int getIconHeight() {
			return height;
		}
	}
}
