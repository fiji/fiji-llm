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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;

import sc.fiji.llm.chat.ActivityRecord.Step;
import sc.fiji.llm.chat.SerializedConversation.SerializedConversationMessage;

public class ActivityRecordTest {

	@Test
	public void testRecordsThinkingAndToolSteps() {
		final ActivityRecord record = new ActivityRecord();
		record.appendThinking("Let me ");
		record.appendThinking("check.");
		record.toolStarted("fiji_image_list", "{}");
		record.appendThinking("Found it.");

		final List<Step> steps = record.getSteps();
		assertEquals(3, steps.size());
		assertEquals("Let me check.", steps.get(0).getText());
		assertEquals("fiji_image_list", steps.get(1).getTool());
		assertFalse(steps.get(1).isFinished());
		assertTrue(steps.get(2).isThinking());

		record.toolFinished("fiji_image_list", true, 12, "boom");
		assertTrue(steps.get(1).isFinished());
		assertTrue(steps.get(1).isFailed());
		assertEquals(Long.valueOf(12), steps.get(1).getMillis());
		assertEquals("boom", steps.get(1).getText());
	}

	@Test
	public void testTruncatesToolResults() {
		final ActivityRecord record = new ActivityRecord();
		record.toolStarted("fiji_log_read", "{}");
		record.toolFinished("fiji_log_read", false, 1, "x".repeat(
			ActivityRecord.MAX_RESULT_LENGTH + 5));
		assertEquals("x".repeat(ActivityRecord.MAX_RESULT_LENGTH) +
			"\n... (5 more characters)", record.getSteps().get(0).getText());
	}

	@Test
	public void testSerializedMessageRoundTrip() {
		final ActivityRecord record = new ActivityRecord();
		record.appendThinking("Hmm.");
		record.toolStarted("fiji_image_list", "{}");
		record.toolFinished("fiji_image_list", false, 7, "[]");
		record.setElapsedSeconds(3);
		final SerializedConversationMessage message =
			new SerializedConversationMessage();
		message.setDisplayMessage("Done");
		message.setMemoryMessage(new SerializedMessage("AI", "Done"));
		message.setActivity(record);

		final Gson gson = new Gson();
		final SerializedConversationMessage restored = gson.fromJson(gson.toJson(
			message), SerializedConversationMessage.class);
		assertEquals(message, restored);
		assertEquals(3, restored.getActivity().getElapsedSeconds());
	}

	@Test
	public void testMessageWithoutActivityLoads() {
		final SerializedConversationMessage restored = new Gson().fromJson(
			"{\"displayMessage\":\"Hi\",\"memoryMessage\":{\"type\":\"AI\"," +
				"\"content\":\"Hi\"}}", SerializedConversationMessage.class);
		assertEquals("Hi", restored.getDisplayMessage());
		assertNull(restored.getActivity());
	}
}
