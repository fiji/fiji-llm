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
 * LLM provider plugin for Ollama Muse Glimmer:30B IQ2_XS model.
 * See: https://huggingface.co/unsloth/Muse-Glimmer-30B-GGUF
 */
@Plugin(type = LLMProvider.class, name = "Ollama (Glimmer:30B)", priority = ProviderPriority.GLIMMER)
public class GlimmerProviderIQ2 extends AbstractCuratedModelProvider {

	private static final String XS_MODEL =
		"hf.co/unsloth/Muse-Glimmer-30B-GGUF:UD-IQ2_XS";
	private static final String M_MODEL =
		"hf.co/unsloth/Muse-Glimmer-30B-GGUF:UD-Q4_K_XL";
	private static final String XS_ALIAS = "XS (30B)";
	private static final String M_ALIAS = "M (30B)";

	public GlimmerProviderIQ2() {
		super(new CuratedModel(XS_MODEL, XS_ALIAS, 12_913_433_344L, 14.88,
			0.0000150), new CuratedModel(M_MODEL, M_ALIAS, 21_168_856_832L, 22.56,
			0.0000150));
	}

	@Override
	public String getName() {
		return "Muse Glimmer (Ollama)";
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://dev.meta.ai/docs/muse-glimmer";
	}

	@Override
	public String getDescription() {
		return "Local Muse Glimmer models with selectable memory footprints.";
	}

	@Override
	public ModelDisplay getModelDisplay(final String modelName) {
		return new ModelDisplay("Ollama", "Muse Glimmer " + resolveModelAlias(
			modelName));
	}

	@Override
	protected int getContextSize() {
		return 32 * 1024;
	}

}
