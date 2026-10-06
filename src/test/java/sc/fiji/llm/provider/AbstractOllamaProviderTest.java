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
package sc.fiji.llm.provider;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

/**
 * Tests for shared Ollama provider behavior.
 */
public class AbstractOllamaProviderTest {

	@Test
	public void testPrioritizeInstalledModels() {
		final TestOllamaProvider provider = new TestOllamaProvider();
		final String remoteXS = provider.appendRemoteString("XS");
		final String remoteM = provider.appendRemoteString("M");

		assertEquals(List.of("S", "L", remoteXS, remoteM), provider
			.prioritizeInstalledModels(List.of(remoteXS, "S", remoteM, "L")));
	}

	@Test
	public void testGemma4RemoteDownloadSizes() {
		final Gemma4Provider provider = new Gemma4Provider();

		assertEquals(Long.valueOf(6_146_493_536L), provider
			.getRemoteModelDownloadSize(
				"hf.co/google/gemma-4-E4B-it-qat-q4_0-gguf:latest").get());
		assertEquals(Long.valueOf(7_150_994_912L), provider
			.getRemoteModelDownloadSize(
				"hf.co/google/gemma-4-12B-it-qat-q4_0-gguf:latest").get());
		assertEquals(Long.valueOf(15_634_191_744L), provider
			.getRemoteModelDownloadSize(
				"hf.co/google/gemma-4-26B-A4B-it-qat-q4_0-gguf:latest").get());
		assertEquals(Long.valueOf(18_851_727_936L), provider
			.getRemoteModelDownloadSize(
				"hf.co/google/gemma-4-31B-it-qat-q4_0-gguf:latest").get());
	}

	@Test
	public void testGlimmerRemoteDownloadSize() {
		final GlimmerProviderIQ2 provider = new GlimmerProviderIQ2();

		assertEquals(Long.valueOf(12_913_433_344L), provider
			.getRemoteModelDownloadSize(
				"hf.co/unsloth/Muse-Glimmer-30B-GGUF:UD-IQ2_XS").get());
	}

	private static final class TestOllamaProvider extends AbstractOllamaProvider {

		@Override
		public String getName() {
			return "Test Ollama";
		}

		@Override
		public List<String> getAvailableModels() {
			return List.of();
		}

		@Override
		protected int getContextSize() {
			return 32 * 1024;
		}
	}
}
