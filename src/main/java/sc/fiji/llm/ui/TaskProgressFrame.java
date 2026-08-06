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
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.ui;

import org.scijava.task.Task;

import javax.swing.*;
import java.awt.*;

/**
 * A simple progress frame for monitoring a single task (like model downloads).
 * Displays task name, status, progress bar, and cancel button.
 *
 * TEMPORARY: This is a minimal wrapper until
 * https://github.com/scijava/scijava-ui-swing/issues/90 is resolved, which
 * will provide a more integrated task monitoring solution.
 */
@Deprecated
public class TaskProgressFrame extends JFrame {

	private final Task task;
	private final JLabel statusLabel;
	private final JProgressBar progressBar;
	private Timer updateTimer;

	/**
	 * Creates a progress frame for monitoring a task.
	 *
	 * @param task the task to monitor
	 */
	public TaskProgressFrame(Task task) {
		this.task = task;
		setTitle(task.getName());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(false);

		// Layout
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// Status label (task name + status message)
		statusLabel = new JLabel(task.getName());
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(statusLabel);
		panel.add(Box.createVerticalStrut(10));

		// Progress bar
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(progressBar);
		panel.add(Box.createVerticalStrut(5));

		// Progress label (value/max)
		panel.add(Box.createVerticalStrut(10));

		// Cancel button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		cancelButton.addActionListener(e -> task.cancel("User cancelled download"));
		panel.add(cancelButton);

		add(panel);
		pack();
		setSize(300, 180);
		setLocationRelativeTo(null); // Center on screen

		// Timer to update UI from task
		updateTimer = new Timer(100, e -> updateUI());
		updateTimer.start();
	}

	/**
	 * Updates the UI to reflect current task state.
	 */
	private void updateUI() {
		SwingUtilities.invokeLater(() -> {
			// Update status
			String status = task.getStatusMessage();
			statusLabel.setText((status != null) ? status : task.getName());

			// Update progress
			int max = (int)task.getProgressMaximum();
			int value = (int)task.getProgressValue();
			progressBar.setMaximum(max > 0 ? max : 100);
			progressBar.setValue(value);

			// Close if done
			if (task.isDone()) {
				updateTimer.stop();
				dispose();
			}
		});
	}

	/**
	 * Shows the progress frame.
	 */
	public void showFrame() {
		setVisible(true);
	}
}
