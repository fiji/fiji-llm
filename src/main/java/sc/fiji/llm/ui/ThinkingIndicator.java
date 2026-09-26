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
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	private static final String[] MESSAGES = { "Thinking", "Thresholding thoughts",
		"Segmenting ideas", "Calibrating pixels", "Adjusting brightness and contrast",
		"Consulting the ROI Manager", "Stitching tiles together", "Deconvolving",
		"Z-projecting", "Counting cells", "Denoising", "Tracing neurites",
		"Measuring twice", "Registering", "Watershedding", "Rolling the ball",
		"Filling holes", "Skeletonizing", "Enhancing (but ethically)",
		"Looking up the LUTs", "Converting to 8-bit", "Pondering pixels" };

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
		setText(frames[frame] + " " + message + "... (" + elapsedSeconds() + "s)");
	}

	private static String[] supportedFrames(final Font font) {
		return font.canDisplayUpTo(String.join("", STAR_FRAMES)) == -1
			? STAR_FRAMES : ASCII_FRAMES;
	}
}
