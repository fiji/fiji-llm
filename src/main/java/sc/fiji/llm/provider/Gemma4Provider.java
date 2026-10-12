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

import org.scijava.plugin.Plugin;

/**
 * LLM provider plugin for the locally hosted Gemma4 Ollama models.
 * See: https://huggingface.co/collections/google/gemma-4-69c2f5b8d6a5f
 */
@Plugin(type = LLMProvider.class, name = "Ollama (Gemma4)", priority = ProviderPriority.GEMMA4)
public class Gemma4Provider extends AbstractCuratedModelProvider {

	private static final String XS_MODEL =
		"hf.co/google/gemma-4-E4B-it-qat-q4_0-gguf:latest";
	private static final String S_MODEL =
		"hf.co/google/gemma-4-12B-it-qat-q4_0-gguf:latest";
	private static final String M_MODEL =
		"hf.co/google/gemma-4-26B-A4B-it-qat-q4_0-gguf:latest";
	private static final String L_MODEL =
		"hf.co/google/gemma-4-31B-it-qat-q4_0-gguf:latest";

	private static final String XS_ALIAS = "XS (4B)";
	private static final String S_ALIAS = "S (12B)";
	private static final String M_ALIAS = "M (26B)";
	private static final String L_ALIAS = "L (31B)";

	public Gemma4Provider() {
		super(new CuratedModel(XS_MODEL, XS_ALIAS, 6_146_493_536L, 7.76,
			0.0000153), new CuratedModel(S_MODEL, S_ALIAS, 7_150_994_912L, 8.69,
			0.0000140), new CuratedModel(M_MODEL, M_ALIAS, 15_634_191_744L, 15.88,
			0.0000209), new CuratedModel(L_MODEL, L_ALIAS, 18_851_727_936L, 20.05,
			0.0000860));
	}

	@Override
	public String getName() {
		return "Gemma4 (Ollama)";
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://ai.google.dev/gemma/docs/core";
	}

	@Override
	public ModelDisplay getModelDisplay(final String modelName) {
		final String modelAlias = resolveModelAlias(modelName);
		return new ModelDisplay("Ollama", "Gemma4 " + modelAlias);
	}

	@Override
	public String getDescription() {
		return "Local Gemma4 models with selectable memory footprints.";
	}

	@Override
	protected int getContextSize() {
		return 32 * 1024;
	}

	@Override
	protected int getContextSize(String modelName) {
		if (XS_MODEL.equals(modelName)) return 64 * 1024;
		if (S_MODEL.equals(modelName)) return 64 * 1024;
		return getContextSize();
	}
}
