package sc.fiji.llm.provider;

import org.scijava.plugin.Parameter;

import sc.fiji.llm.auth.APIKeyService;

public abstract class AbstractLLMProvider implements LLMProvider {
    @Parameter
    private APIKeyService apiKeyService;

    protected String apiKey() {
        String apiKey = apiKeyService.getApiKey(getName());
        if (apiKey == null) {
			throw new IllegalStateException("No API key configured for provider: " + getName());
		}
        return apiKey;
    }
}
