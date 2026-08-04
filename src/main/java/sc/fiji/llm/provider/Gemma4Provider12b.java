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
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.provider;

import java.util.Optional;

import org.scijava.plugin.Plugin;

/**
 * LLM provider plugin for Ollama gemma4:12b model.
 * See: https://huggingface.co/google/gemma-4-12B-it-qat-q4_0-gguf
 */
@Plugin(type = LLMProvider.class, name = "Ollama (Gemma4:12B)")
public class Gemma4Provider12b extends AbstractSingletonOllamaProvider {

	private static final String MODEL_NAME = "hf.co/google/gemma-4-12B-it-qat-q4_0-gguf:latest";

	public Gemma4Provider12b() {
		super(MODEL_NAME);
	}

	@Override
	public String getName() {
		return "Ollama (Gemma4 - small)";
	}

	@Override
	public String getDescription() {
		return "Local Gemma4 model - smallest parameter count and memory footprint.";
	}

	@Override
	public Optional<String> getRecommendedModel() {
		return Optional.of(MODEL_NAME);
	}

}
