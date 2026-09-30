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

package sc.fiji.llm.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sc.fiji.llm.provider.AnthropicProvider;
import sc.fiji.llm.provider.Gemma4Provider;
import sc.fiji.llm.provider.LLMProvider;

public class Fiji_ChatTest {

	@Test
	public void testChoiceLabelMarksOnlyCuratedProviders() {
		final Gemma4Provider curated = new Gemma4Provider();
		assertEquals("Gemma4 (Ollama)", curated.getName());
		assertEquals("*Gemma4 (Ollama)", Fiji_Chat.choiceLabel(curated));
		assertEquals("Claude", Fiji_Chat.choiceLabel(new AnthropicProvider()));
	}

	@Test
	public void testFormatCost() {
		final String message = Fiji_Chat.formatCost(new LLMProvider.ModelCost(0.2,
			1.2));
		assertTrue(message.contains("Approximate API Cost"));
		assertTrue(message.contains("$0.20"));
		assertTrue(message.contains("$1.20"));
		assertTrue(message.contains("per 1M tokens"));
	}
}
