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

package sc.fiji.llm.chat;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.scijava.app.AppService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;

@Plugin(type = Service.class)
public class DefaultConversationService extends AbstractService implements
	ConversationService
{

	@Parameter
	private AppService appService;

	private final Map<String, Integer> conversationLengths = new HashMap<>();
	private final Map<String, Conversation> conversationsByName = new HashMap<>();
	private final List<Conversation> conversations = new ArrayList<>();

	private File conversationDir;
	private Gson gson;

	@Override
	public List<String> getConversationNames() {
		return conversations.stream().map(Conversation::name).collect(Collectors
			.toList());
	}

	@Override
	public Conversation getConversation(String name) {
		return conversationsByName.get(name);
	}

	@Override
	public Conversation createConversation(String name,
		SystemMessage systemMessage)
	{
		Conversation conversation = new Conversation(name, systemMessage);
		addConversation(conversation);
		return conversation;
	}

	@Override
	public boolean addConversation(Conversation newConversation) {
		if (!conversationsByName.containsKey(newConversation.name())) {
			conversations.add(newConversation);
		}
		conversationsByName.put(newConversation.name(), newConversation);
		return true;
	}

	@Override
	public boolean removeConversation(String name) {
		Conversation conversation = conversationsByName.remove(name);
		if (conversation != null) {
			conversations.remove(conversation);
			conversationLengths.remove(name);
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteConversation(String name) {
		if (removeConversation(name)) {
			File file = new File(conversationDir, sanitizeFileName(name) + ".json");
			if (file.exists()) {
				file.delete();
			}
			return true;
		}
		return false;
	}

	@Override
	public void initialize() {
		gson = new GsonBuilder().setPrettyPrinting().create();

		// Try to set up conversation directory in app config dir
		File baseDir = appService.getApp().getBaseDirectory();
		conversationDir = new File(baseDir, ".fiji-chat-history");

		if (!conversationDir.exists()) {
			if (!conversationDir.mkdirs()) {
				// Fall back to user home directory
				conversationDir = new File(System.getProperty("user.home"),
					".fiji-chat-history");
				if (!conversationDir.exists()) {
					conversationDir.mkdirs();
				}
			}
		}

		// Load existing conversations
		loadConversations();
	}

	@Override
	public void dispose() {
		// Save all conversations that have changed or are new
		saveConversations();
	}

	/**
	 * Load all conversations from the conversation directory.
	 */
	private void loadConversations() {
		if (!conversationDir.exists() || !conversationDir.isDirectory()) {
			return;
		}

		File[] files = conversationDir.listFiles((dir, name) -> name.endsWith(
			".json"));
		if (files != null) {
			// Sort by last modified date, most recent first
			Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a
				.lastModified()));
			for (File file : files) {
				try {
					SerializedConversation serialized = gson.fromJson(new FileReader(
						file), SerializedConversation.class);

					if (serialized != null) {
						SystemMessage systemMessage = new SystemMessage(serialized
							.getSystemMessage());
						Conversation conversation = new Conversation(serialized.getName(),
							systemMessage);

						for (SerializedConversation.SerializedConversationMessage msg : serialized
							.getMessages())
						{
							ChatMessage memoryMessage = ChatMessageConverter.fromSerialized(
								msg.getMemoryMessage());
							conversation.addMessage(msg.getDisplayMessage(), memoryMessage);
						}

						conversationsByName.put(conversation.name(), conversation);
						conversations.add(conversation);
						conversationLengths.put(conversation.name(), conversation.messages()
							.size());
					}
				}
				catch (IOException e) {
					getContext().getService(org.scijava.log.LogService.class).warn(
						"Failed to load conversation from " + file.getName(), e);
				}
			}
		}
	}

	/**
	 * Save all modified or new conversations to disk.
	 */
	private void saveConversations() {
		for (Conversation conversation : conversations) {
			int currentLength = conversation.messages().size();
			int previousLength = conversationLengths.getOrDefault(conversation.name(),
				-1);

			// Only save if changed or new
			if (previousLength != currentLength) {
				saveConversation(conversation);
				conversationLengths.put(conversation.name(), currentLength);
			}
		}
	}

	/**
	 * Save a single conversation to disk.
	 */
	private void saveConversation(Conversation conversation) {
		try {
			File file = new File(conversationDir, sanitizeFileName(conversation
				.name()) + ".json");

			SerializedConversation serialized = new SerializedConversation();
			serialized.setName(conversation.name());
			serialized.setSystemMessage(conversation.systemMessage().text());

			List<SerializedConversation.SerializedConversationMessage> messages =
				new ArrayList<>();
			for (Conversation.Message msg : conversation.messages()) {
				SerializedConversation.SerializedConversationMessage serializedMsg =
					new SerializedConversation.SerializedConversationMessage();
				serializedMsg.setDisplayMessage(msg.display());
				serializedMsg.setMemoryMessage(ChatMessageConverter.toSerialized(msg
					.memory()));
				messages.add(serializedMsg);
			}
			serialized.setMessages(messages);

			try (FileWriter writer = new FileWriter(file)) {
				gson.toJson(serialized, writer);
			}
		}
		catch (IOException e) {
			getContext().getService(org.scijava.log.LogService.class).error(
				"Failed to save conversation: " + conversation.name(), e);
		}
	}

	/**
	 * Sanitize conversation name for use as a filename.
	 */
	private String sanitizeFileName(String name) {
		return name.replaceAll("[^a-zA-Z0-9_-]", "_");
	}
}
