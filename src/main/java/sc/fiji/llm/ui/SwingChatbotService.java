/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package sc.fiji.llm.ui;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import sc.fiji.llm.context.ContextItemService;

/**
 * Swing implementation of ChatbotService that launches a SimpleChatWindow.
 */
@Plugin(type = Service.class)
public class SwingChatbotService extends AbstractService implements
	ChatbotService
{

	@Override
	public void launchChat(String title, String providerName, String modelName) {
		SwingUtilities.invokeLater(() -> {
			FijiAssistantChat chatWindow = new FijiAssistantChat(getContext(), title,
				providerName, modelName);
			chatWindow.show();
		});
	}
}
