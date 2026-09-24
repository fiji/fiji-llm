# Fiji-LLM Prompt Suite

This is a human-facing collection of prompts for exploring Fiji-aware agent
behavior. It covers orientation, capability discovery, guided analysis,
reproducibility, error recovery, and trust. The prompts are intentionally
open-ended and are not an authoritative specification or a required tool
sequence.

Run prompts with the Fiji state and preconditions that make sense for the
scenario. When evaluating an agent, preserve the tool trace and compare the
agent's claims with the state and results actually observed in Fiji.

## Orientation
```text
What images are currently open in Fiji and which one is active? Describe the active image, separating observed facts from anything you are inferring.
```

```text
What are the dimensions and calibration of this image? What plane am I currently viewing? Is there an active selection?
```

```text
I'm new to Fiji. Explain what I'm looking at without assuming I know image-analysis terminology.
```

## Discovery
```text
I want to inspect the distribution of pixel values. What command would I use and what would it show?
```

```text
What installed tools could help me separate bright objects from a darker background? Give me two simple options and explain the tradeoffs.
```

```text
What’s the difference between setting a threshold and actually converting the image to a binary mask?
```

```text
I want to count bright, separated objects and measure their areas. Before doing anything, what do you need to know to choose a reasonable workflow?
```

```text
Show me the histogram of the active image and tell me if there’s evidence of saturation. What would a reasonable threshold be?
```

## Guided Analysis

```text
Help me segment and analyze the Blobs sample image.
```
 
```text
What is your opinion of the quality of the segmentation?
```

```text
Summarize what we did in a form I could include in my analysis notes.
```

## Error recovery
```text
The following ImageJ macro fails when I run it on the active image. Use the
error output and current Fiji state to diagnose it. Explain the failure and
propose the smallest correction; do not silently rewrite the whole workflow.

run("8-bit");
setAutoThreshold("Default");
run("Convert to Mask");
run("Analyze Particles...", "size=50-Infinity display summarize");
saveAs("Results", output_path);

Why did this fail?
```

```text
Generate a summary of this error I can post to the Fiji forum
```

## Metacognition and trust
```text
Explain what you can do here that a general web chatbot cannot.
```
