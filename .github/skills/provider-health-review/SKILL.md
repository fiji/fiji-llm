---
name: provider-health-review
description: 'Review the GitHub provider-health findings for one or more hosted LLM providers and produce a concise, evidence-based report. Use when a user names providers such as OpenAI, Anthropic, Claude, ChatGPT, or Gemini and asks what needs review or what should change. Do not modify provider code or clear review flags unless the user explicitly requests implementation.'
argument-hint: 'Name the provider(s) to review, such as "OpenAI and Anthropic"'
user-invocable: true
disable-model-invocation: false
---

# Provider Health Review

Use this skill to investigate the named provider(s) and prepare a small report
that lets the user decide whether provider metadata should change. This is a
review workflow, not an implementation workflow.

## Provider resolution

Resolve common names to the provider names used by the health JSON and source
code:

| User name | Health key | Provider implementation |
| --- | --- | --- |
| OpenAI, ChatGPT | `ChatGPT` | `OpenAIProvider` |
| Anthropic, Claude | `Claude` | `AnthropicProvider` |
| Google AI, Google, Gemini | `Gemini` | `GeminiProvider` |

If the user names Ollama, explain that the workflow intentionally excludes
Ollama from hosted-provider health checks because it does not require an API
key. Its README badge is static and does not provide a review finding.

If a name is ambiguous or does not map to a known provider, ask the user to
clarify rather than silently reviewing a different provider.

## Evidence to collect

1. Read the latest `provider-status.json` from the `provider-status` branch
   when it is available:
   `https://raw.githubusercontent.com/fiji/fiji-llm/provider-status/provider-status.json`
   If it cannot be retrieved, state that the report is based on repository
   source only and do not present the status as current.
2. For each requested provider, inspect its status entry and relevant entries
   in `failures`. Report the provider health, changed fields, request errors,
   content errors, HTTP status, final URL, content selector, and review flags
   only when they are present.
3. Inspect the provider implementation and identify the configured model
   aliases, model IDs, model list, pricing, vision assumptions, and recommended
   model where applicable. Use the actual implementation as the source of
   truth rather than assuming that every provider represents models the same
   way.
4. Open the provider's current models documentation when web access is
   available. Compare the documented model names, IDs, availability, pricing,
   image support, and relevant lifecycle or replacement information with the
   configured provider metadata. Do not copy large sections of provider
   documentation into the report.
5. If the finding concerns an API-key URL, verify whether the URL still
   identifies the intended key-management page. This is a URL-maintenance
   finding, not automatically a model-list finding.

The health probe treats changes to documentation content or
`Last-Modified` metadata as a reason for human review; it does not determine
whether the configured model list is correct. A changed documentation hash
therefore requires a comparison, not an automatic model update.

## Provider-specific comparison

- **ChatGPT**: check the user-facing aliases returned by
  `getAvailableModels()`, the aliases-to-provider-ID mapping, model pricing,
  and the vision-support assumptions in `OpenAIProvider`.
- **Claude**: check the user-facing aliases returned by
  `getAvailableModels()`, the aliases-to-provider-ID mapping, model pricing,
  and the vision-support assumptions in `AnthropicProvider`.
- **Gemini**: check the hard-coded available model list, vision-model set,
  recommended model, and any model naming or lifecycle changes in
  `GeminiProvider`.

Also verify the documentation URL and content selector when the health finding
indicates a redirect, URL change, or content-check failure.

## Report format

Keep the report concise and organize it by requested provider:

```text
## Provider health review

### ChatGPT — review_required
- Workflow evidence: ...
- Documentation comparison: ...
- Configured metadata: ...
- Assessment: ...
- Recommended next step: ...

### Claude — healthy
- Workflow evidence: ...
- Assessment: no current health finding; no metadata change is indicated.

## Decision point
...
```

Include direct links to the provider source and documentation pages. Distinguish
clearly between:

- **Confirmed mismatch**: the documentation contradicts configured metadata.
- **Possible mismatch**: the documentation is ambiguous or the live page could
  not be inspected.
- **Maintenance-only change**: a URL, redirect, selector, or API-key page
  changed without evidence that model metadata is stale.
- **Unhealthy check**: the page could not be checked or returned a broken
  response; do not infer model changes from that alone.

End with a decision-oriented recommendation. If changes appear warranted, name
the exact provider file and metadata that would need updating, but do not edit
the files, update `getModelsDocumentationLastModified()`, or claim that a
review flag is cleared. Wait for the user to choose whether to implement the
recommended changes.
