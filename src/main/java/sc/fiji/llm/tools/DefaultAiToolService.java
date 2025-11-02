package sc.fiji.llm.tools;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

/**
 * Default implementation of AiToolService.
 */
@Plugin(type = Service.class)
public class DefaultAiToolService extends AbstractSingletonService<AiToolPlugin> implements AiToolService {

    @Override
    public Class<AiToolPlugin> getPluginType() {
		return AiToolPlugin.class;
    }

}
