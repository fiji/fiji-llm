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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sc.fiji.llm.provider.AnthropicProvider;
import sc.fiji.llm.provider.Gemma4Provider;
import sc.fiji.llm.provider.LLMProvider;

public class Fiji_ChatTest {

	@Test
	public void testServiceInfoMatchesProviderType() {
		final String hosted = Fiji_Chat.formatServiceInfo(new AnthropicProvider());
		assertTrue(hosted.contains("Cloud AI service."));
		assertTrue(hosted.contains("Powerful remotely hosted models."));
		assertTrue(hosted.contains(
			"Data handling and retention are subject to provider policies."));
		assertFalse(hosted.contains("Local AI service."));

		final String local = Fiji_Chat.formatServiceInfo(new Gemma4Provider());
		assertTrue(local.contains("Local AI service."));
		assertTrue(local.contains("Models run on your hardware."));
		assertTrue(local.contains("All data and messages stay on your computer."));
		assertFalse(local.contains("Cloud AI service."));
	}

	@Test
	public void testFormatCost() {
		assertTrue(Fiji_Chat.formatCost(new LLMProvider.ModelCost(0.2, 1.2))
			.contains("Relative API Cost"));
		final String low = Fiji_Chat.formatCost(new LLMProvider.ModelCost(0.2,
			1.2));
		assertTrue(low.contains(">Low</b>"));
		assertFalse(low.contains(">Medium</b>"));
		assertFalse(low.contains(">High</b>"));
		assertTrue(low.contains("cellspacing='0'"));
		assertTrue(low.contains("valign='middle'"));
		assertTrue(low.contains("#2e7d32"));
		assertFalse(low.contains("#c58a00"));
		assertFalse(low.contains("#c62828"));

		final String medium = Fiji_Chat.formatCost(new LLMProvider.ModelCost(2.0,
			10.0));
		assertFalse(medium.contains(">Low</b>"));
		assertTrue(medium.contains(">Medium</b>"));
		assertFalse(medium.contains(">High</b>"));
		assertTrue(medium.contains("#c58a00"));
		assertFalse(medium.contains("#2e7d32"));
		assertFalse(medium.contains("#c62828"));

		final String high = Fiji_Chat.formatCost(new LLMProvider.ModelCost(4.0,
			20.0));
		assertFalse(high.contains(">Low</b>"));
		assertFalse(high.contains(">Medium</b>"));
		assertTrue(high.contains(">High</b>"));
		assertTrue(high.contains("#c62828"));
		assertFalse(high.contains("#2e7d32"));
		assertFalse(high.contains("#c58a00"));
	}

	@Test
	public void testFormatDemand() {
		final String low = Fiji_Chat.formatDemand(new LLMProvider.ModelDemand(8.0,
			0.0));
		assertTrue(low.contains("Relative Local Demand"));
		assertTrue(low.contains(">Low</b>"));

		final String medium = Fiji_Chat.formatDemand(new LLMProvider.ModelDemand(
			14.0, 0.0));
		assertTrue(medium.contains(">Medium</b>"));

		final String high = Fiji_Chat.formatDemand(new LLMProvider.ModelDemand(19.0,
			0.0));
		assertTrue(high.contains(">High</b>"));
	}
}
