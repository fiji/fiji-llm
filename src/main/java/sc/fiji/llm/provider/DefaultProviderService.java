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
package sc.fiji.llm.provider;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

import sc.fiji.llm.auth.APIKeyService;

/**
 * Default implementation of ProviderService.
 */
@Plugin(type = Service.class)
public class DefaultProviderService extends AbstractSingletonService<LLMProvider> implements ProviderService {

	// HACK - individual providers want the service so we have to ensure it's in the context
	@Parameter
	private APIKeyService apiKeyService;

	@Override
	public LLMProvider getProvider(final String providerName) {
		return getInstances().stream()
			.filter(p -> p.getName().equals(providerName))
			.findFirst()
			.orElse(null);
	}

    @Override
    public Class<LLMProvider> getPluginType() {
		return LLMProvider.class;
    }

	@Override
	public void initialize() {
		getInstances().stream().forEach(LLMProvider::initialize);
	}

	@Override
	public void dispose() {
		getInstances().stream().forEach(LLMProvider::dispose);
	}
}
