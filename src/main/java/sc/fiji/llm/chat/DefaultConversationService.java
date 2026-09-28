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

package sc.fiji.llm.chat;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
	private final Map<String, Conversation> conversationsById = new HashMap<>();
	private final List<Conversation> conversations = new ArrayList<>();

	private File conversationDir;
	private Gson gson;

	@Override
	public List<Conversation> getConversations() {
		return Collections.unmodifiableList(conversations);
	}

	@Override
	public Conversation getConversation(String id) {
		return conversationsById.get(id);
	}

	@Override
	public Conversation createConversation(String displayName,
		SystemMessage systemMessage)
	{
		Conversation conversation = new Conversation(UUID.randomUUID().toString(),
			displayName, systemMessage);
		addConversation(conversation);
		return conversation;
	}

	@Override
	public synchronized Conversation nameConversation(final String id,
		final String displayName)
	{
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("Conversation ID cannot be blank");
		}
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Conversation display name cannot be blank");
		}

		final Conversation conversation = conversationsById.get(id);
		if (conversation == null) {
			throw new IllegalArgumentException("No conversation found with ID: " + id);
		}
		if (conversation.displayName() != null && !conversation.displayName().isBlank()) {
			throw new IllegalArgumentException("Conversation already has a display name: " +
				conversation.displayName());
		}

		conversation.setDisplayName(displayName.trim());
		saveConversation(conversation);
		return conversation;
	}

	@Override
	public boolean addConversation(Conversation newConversation) {
		final Conversation previous = conversationsById.put(newConversation.id(),
			newConversation);
		if (previous != null) conversations.remove(previous);
		conversations.add(0, newConversation);
		return true;
	}

	@Override
	public boolean removeConversation(String id) {
		Conversation conversation = conversationsById.remove(id);
		if (conversation != null) {
			conversations.remove(conversation);
			conversationLengths.remove(id);
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteConversation(String id) {
		if (removeConversation(id)) {
			File file = conversationFile(id);
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
	 * Saves pending changes, then switches to another directory and loads its
	 * conversations. For testing.
	 */
	void setConversationDirectory(File dir) {
		saveConversations();
		conversationDir = dir;
		conversations.clear();
		conversationsById.clear();
		conversationLengths.clear();
		loadConversations();
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
						// Note: older files have no ID; their file name serves as one.
						final String id = isValidId(serialized.getId()) ? serialized.getId()
							: file.getName().replaceFirst("\\.json$", "");
						Conversation conversation = new Conversation(id, serialized
							.getDisplayName(), systemMessage);

						for (SerializedConversation.SerializedConversationMessage msg : serialized
							.getMessages())
						{
							ChatMessage memoryMessage = ChatMessageConverter.fromSerialized(
								msg.getMemoryMessage());
							conversation.addMessage(msg.getDisplayMessage(), memoryMessage,
								msg.getActivity());
						}

						conversationsById.put(conversation.id(), conversation);
						conversations.add(conversation);
						conversationLengths.put(conversation.id(), conversation.messages()
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
			int previousLength = conversationLengths.getOrDefault(conversation.id(),
				-1);

			// Only save if changed or new
			if (previousLength != currentLength) {
				saveConversation(conversation);
				conversationLengths.put(conversation.id(), currentLength);
			}
		}
	}

	/**
	 * Save a single conversation to disk.
	 */
	private void saveConversation(Conversation conversation) {
		try {
			File file = conversationFile(conversation.id());

			SerializedConversation serialized = new SerializedConversation();
			serialized.setId(conversation.id());
			serialized.setDisplayName(conversation.displayName());
			serialized.setSystemMessage(conversation.systemMessage().text());

			List<SerializedConversation.SerializedConversationMessage> messages =
				new ArrayList<>();
			for (Conversation.Message msg : conversation.messages()) {
				SerializedConversation.SerializedConversationMessage serializedMsg =
					new SerializedConversation.SerializedConversationMessage();
				serializedMsg.setDisplayMessage(msg.display());
				serializedMsg.setMemoryMessage(ChatMessageConverter.toSerialized(msg
					.memory()));
				serializedMsg.setActivity(msg.activity());
				messages.add(serializedMsg);
			}
			serialized.setMessages(messages);

			try (FileWriter writer = new FileWriter(file)) {
				gson.toJson(serialized, writer);
			}
		}
		catch (IOException e) {
			getContext().getService(org.scijava.log.LogService.class).error(
				"Failed to save conversation: " + conversation.displayName(), e);
		}
	}

	/** IDs name files, so they must not contain path characters. */
	private static boolean isValidId(String id) {
		return id != null && id.matches("[A-Za-z0-9_-]+");
	}

	private File conversationFile(String id) {
		return new File(conversationDir, id + ".json");
	}
}
