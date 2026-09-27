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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Swing implementation of the MCP tool viewer.
 */
@Plugin(type = Service.class)
public class SwingMCPToolViewer extends AbstractService implements MCPToolViewer {

	@Override
	public void show(final List<String> toolNames) {
		if (SwingUtilities.isEventDispatchThread()) {
			showOnEdt(toolNames);
			return;
		}

		try {
			SwingUtilities.invokeAndWait(() -> showOnEdt(toolNames));
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while showing MCP tools", e);
		}
		catch (final InvocationTargetException e) {
			throw new IllegalStateException("Could not show MCP tools", e.getCause());
		}
	}

	private void showOnEdt(final List<String> toolNames) {
		final Window owner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getActiveWindow();
		if (toolNames == null || toolNames.isEmpty()) {
			JOptionPane.showMessageDialog(owner, "No MCP tools are currently exposed.",
				"Fiji MCP Tools", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode(
			"Fiji MCP tools");
		final Map<String, List<String>> toolsByCategory = new TreeMap<>();
		for (final String toolName : toolNames) {
			final String category = categoryFor(toolName);
			toolsByCategory.computeIfAbsent(category, key -> new ArrayList<>())
				.add(toolName);
		}

		for (final Map.Entry<String, List<String>> entry : toolsByCategory.entrySet()) {
			final List<String> categoryTools = entry.getValue();
			categoryTools.sort(String::compareTo);
			final DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(
				entry.getKey() + " (" + categoryTools.size() + ")");
			for (final String toolName : categoryTools) {
				categoryNode.add(new DefaultMutableTreeNode(toolName));
			}
			root.add(categoryNode);
		}

		final JTree toolsTree = new JTree(root);
		toolsTree.setRootVisible(false);
		toolsTree.setShowsRootHandles(true);
		final JScrollPane scrollPane = new JScrollPane(toolsTree);
		scrollPane.setPreferredSize(new Dimension(420, 360));

		final JDialog dialog = new JDialog(owner, "Fiji MCP Tools",
			Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.add(scrollPane);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
	}

	private String categoryFor(final String toolName) {
		final String prefix = "fiji_";
		if (!toolName.startsWith(prefix)) return "Other";
		final int separator = toolName.indexOf('_', prefix.length());
		return separator > prefix.length() ? toolName.substring(prefix.length(),
			separator) : "Other";
	}
}
