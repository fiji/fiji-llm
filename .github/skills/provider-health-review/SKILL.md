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
   The branch is updated only when a status changes, so `checked_at` identifies
   the last published status change, not necessarily the most recent scheduled
   probe.
2. For each requested provider, inspect its status entry and relevant entries
  in `failures`. Report the provider health, changed fields, request errors,
  content errors, HTTP status, final URL, extracted facts, baseline marker,
  and review flags only when they are present.
3. When the finding needs operational clarification, inspect the executable
  [`ProviderHealthProbe`](../../../src/test/java/sc/fiji/llm/provider/ProviderHealthProbe.java)
  and the scheduled
  [`provider-health.yml`](../../workflows/provider-health.yml). The workflow
  compiles test sources and launches the probe as a Java main class; it is not
  a JUnit test. [`ProviderCapabilityTest`](../../../src/test/java/sc/fiji/llm/provider/ProviderCapabilityTest.java)
  covers provider fact fixtures, documentation
  baselines, costs, and vision assumptions, but does not perform the live URL
  checks.
4. Inspect the provider implementation and identify the configured model
  aliases, raw model IDs, available model list, pricing, vision assumptions,
  model-specific request behavior, and ordering or default selection where
  applicable. There is no formal recommended-model API. Distinguish aliases
  exposed by `getAvailableModels()` from IDs in the provider's internal mapping;
  a mapped model may not be user-selectable. Use the actual implementation as
  the source of truth rather than assuming that every provider represents
  models the same way.
5. Open the provider's current models documentation when web access is
   available. Compare the documented model names, IDs, availability, pricing,
   image support, and relevant lifecycle or replacement information with the
   configured provider metadata. Do not copy large sections of provider
   documentation into the report.
6. If the finding concerns an API-key URL, verify whether the configured URL
  currently reaches the intended key-management page. A local URL edit is
  published as diagnostic metadata but does not require review by itself;
  review the live request only when it reports an error or broken status.

The health probe asks each provider to turn raw documentation HTML into
structured model-catalog facts. Providers should retain model IDs, pricing, and
lifecycle information while excluding volatile prose. The probe compares those
facts directly and reports exact added, removed, or changed fields; it does not
determine whether the configured model list is correct.

The probe discovers providers through `ProviderService` and checks only
providers for which `LLMProvider.requiresApiKey()` is true. The shared health
hooks live on [`LLMProvider`](../../../src/main/java/sc/fiji/llm/provider/LLMProvider.java):
`getModelsDocumentationFacts` and `getModelsDocumentationLastModified`.

Probe status has specific meanings. A provider is `unhealthy` for a request or
content error, or for HTTP 400, 404, 408, 410, or 5xx. Other statuses such as
401 or 403 are recorded but do not by themselves make the provider unhealthy.
Changes in extracted documentation facts produce `review_required`; an
existing model review can also be carried forward until the provider date is
updated after review. When the provider date equals the current UTC date, the
run establishes the extracted facts as the new baseline. `Last-Modified` is
recorded for diagnostics but does not independently trigger model review. An
Actions run can therefore fail because the result is `review_required`, rather
than because the workflow itself failed.

When a provider's `getModelsDocumentationLastModified()` date equals the
current UTC run date, that run establishes a new documentation baseline. The
probe still fetches and parses the documentation body on every successful
documentation check, and it checks HTTP status, redirects, API-key URLs, and
parser errors. Published provider state keeps the accepted `baseline_facts`
separate from the latest observed `facts`; pending review runs never replace
the accepted baseline. It never uses a whole-page or opaque content hash for
model review.

The `provider-status` branch is a published change-triggered snapshot, not
necessarily the latest scheduled probe. The workflow commits it when the
overall status, a hosted provider's health, or stable check/baseline metadata
changes, so `checked_at` can lag behind recent runs. When freshness matters, inspect the latest
[provider-health workflow run](https://github.com/fiji/fiji-llm/actions/workflows/provider-health.yml)
and use the branch JSON for the published diagnostic details.

## Provider-specific comparison

- **ChatGPT**: check the user-facing aliases returned by
  `getAvailableModels()`, the aliases-to-provider-ID mapping, model pricing,
  vision-support assumptions, and model-specific request defaults such as
  reasoning effort in `OpenAIProvider`.
- **Claude**: check the user-facing aliases returned by
  `getAvailableModels()`, the aliases-to-provider-ID mapping, model pricing,
  and the vision-support assumptions in `AnthropicProvider`.
- **Gemini**: check the hard-coded available model list, vision-model set,
  model ordering, and any model naming or lifecycle changes in
  `GeminiProvider`.

Also verify the documentation URL, response status, and provider fact parser
behavior when the health finding indicates a redirect, URL change, or
content-check failure. Do not expect a shared CSS selector: parsing is
provider-specific.

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
