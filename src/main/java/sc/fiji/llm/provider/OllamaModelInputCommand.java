package sc.fiji.llm.provider;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/** Collects an arbitrary Ollama model identifier through SciJava's UI. */
@Plugin(type = Command.class, name = "Ollama Model Input",
	label = "Enter an Ollama model", visible = false)
public class OllamaModelInputCommand implements Command {

	@Parameter(label = "", visibility = ItemVisibility.MESSAGE, persist = false,
		required = false)
	private String instructions =
	"""
<html><body><p>Enter the exact model string from ollama.com or Hugging Face. For example:</p>
<ul><li>llama3.2:3b</li>
<li>hf.co/unsloth/Qwen3.8-27B-GGUF:UD-IQ3_S</li><ul></body></html>";
""";

	@Parameter(label = "Model:", style="columns=40", persist = false)
	private String model = "";

	@Override
	public void run() {}
}