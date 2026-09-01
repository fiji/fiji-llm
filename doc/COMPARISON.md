# Fiji LLM Integration: Comparative Analysis

**Comparing:** fiji-llm · CopilotJ · Agentic-J  
**Audience:** Scientists and developers evaluating LLM integration options for Fiji/ImageJ  
**Assumption:** Familiarity with Fiji as a tool, but not necessarily with AI frameworks or software architecture

---

## What Each Project Is Trying to Do

Before comparing details, it helps to understand each project's core philosophy, because they are solving
slightly different problems:

| Project | Core idea |
|---|---|
| **fiji-llm** | Add an LLM chat assistant *into* Fiji as a native plugin, using the same extension system Fiji already uses for everything else |
| **CopilotJ** | Run a sophisticated Python AI backend *alongside* Fiji, connected over a network bridge, with a modern web interface |
| **Agentic-J** | Deploy a Docker application that *contains* both the AI system and a Fiji installation, where the primary goal is producing and running actual analysis scripts |

This distinction matters: fiji-llm is the most "embedded," Agentic-J is the most "self-contained," and
CopilotJ sits in between.

---

## Capability Comparison

The table below shows whether each capability is fully present (✅), partially present or limited (⚠️),
disabled but scaffolded (🔧), or absent (❌).

| Capability | fiji-llm | CopilotJ | Agentic-J |
|---|:---:|:---:|:---:|
| **Chat with an LLM** | ✅ | ✅ | ✅ |
| **Streaming responses** (text appears as it's generated) | ✅ | ✅ | ❌ |
| **Conversation history** across sessions | ✅ | ✅ | ✅ |
| **Local / private models** (Ollama, no cloud needed) | ✅ | ✅ | ⚠️ via OpenRouter only |
| **Multiple cloud providers** (OpenAI, Anthropic, Gemini, etc.) | ✅ | ✅ | ⚠️ OpenAI/Anthropic only |
| **Tool use** (AI calls Fiji functions) | ✅ | ✅ | ✅ |
| **Multi-agent orchestration** (multiple specialized AIs working together) | ❌ | ✅ | ✅ |
| **Generates and runs scripts automatically** | ⚠️ limited | ⚠️ macros only | ✅ core feature |
| **Debugs failing scripts** | ❌ | ❌ | ✅ |
| **Document retrieval / RAG** (searches a knowledge base to answer questions) | ❌ | ✅ | ✅ |
| **Improves over time** (learns from successful/failed runs) | ❌ | ⚠️ auto-ingest only | ✅ active Librarian agent |
| **Plugin discovery and installation** | ❌ | ❌ | ✅ |
| **Statistical analysis and plotting** | ❌ | ⚠️ via Python execution | ✅ dedicated Analyst agent |
| **Batch processing workflows** | ❌ | ⚠️ | ✅ |
| **Vision / image inspection by the AI** | ❌ | ✅ opt-in | 🔧 disabled |
| **Exposes Fiji tools to *external* AI clients** (e.g. Claude Desktop) | ✅ MCP server | ✅ self-loopback | ⚠️ MCP client only |
| **Cost/usage tracking** | ❌ | ✅ Langfuse | ✅ built-in per-session |
| **Extensibility for Java developers** | ✅ excellent | ❌ | ❌ |
| **Extensibility for Python developers** | ❌ | ✅ | ✅ |

### What None of Them Have (Yet)

- A graphical workflow builder — workflows are described in natural language, not drawn visually
- Deep integration with Bio-Formats metadata (beyond basic image info)
- Built-in version control for generated scripts
- User permission / role management

---

## Getting Started: How Hard Is It?

This is one of the most practical differences between the three.

### fiji-llm — Easiest to install, hardest to extend

**For a scientist who just wants to use it:**
1. Add the Fiji update site
2. Open Fiji → run `Fiji Chat`
3. Enter an API key for a cloud provider (or configure Ollama for local use)
4. Start chatting

This is the only project that installs like a normal Fiji plugin. No Python environment, no Docker, no
terminal commands.

**For a developer who wants to add features:**
You need to be comfortable with Java and the SciJava plugin framework. The extension pattern is clean and
well-established, but it is Java-first — Python developers cannot easily contribute.

### CopilotJ — Moderate setup, most flexible once running

**For a scientist:**
- Requires a Nix/uv environment or Docker, plus the Java Fiji plugin running simultaneously
- Three components must all be running and connected
- The web interface is modern and polished once everything is up
- A `USER_MANUAL.md` and `just` task runner help, but some comfort with the terminal is required

**For a developer:**
- Python-first, well-organized codebase with a comprehensive `AGENTS.md`
- Adding a new specialized agent takes only a TOML configuration file — no Python code required
- Adding a new tool is straightforward Python
- The three-component architecture (server + plugin + web) means changes can touch multiple systems

### Agentic-J — Highest setup friction, most self-contained once built

**For a scientist:**
- Intended to run via Docker: `docker compose up` and open the desktop app
- Once the Docker image is built, the app is self-contained (Fiji is included)
- A `user_guide/` directory is present
- The PySide6 desktop app feels like a normal application, but the Docker requirement may be unfamiliar
- Scientists without Docker experience may need IT assistance for first-time setup

**For a developer:**
- LangChain/LangGraph are the most widely documented AI frameworks in the Python ecosystem, so external
  documentation and community support are abundant
- Adding a new agent is code-based (vs. CopilotJ's TOML approach)
- Adding new plugin documentation is purely additive — drop a `SKILL.md` in the `skills/` folder

---

## Perceived Strengths

### fiji-llm

**Native Fiji citizen.** Because it uses the same SciJava plugin system Fiji uses for everything else,
it is the most natural fit for the existing Fiji developer ecosystem. Extensions follow patterns Fiji
developers already know.

**Best local model support.** Ollama is a first-class provider alongside cloud options. Local models get
the same feature set as cloud models — no degraded experience.

**Platform potential.** The MCP server it exposes means other AI tools (Claude Desktop, VS Code Copilot,
etc.) can discover and call Fiji's capabilities. It is not just a chatbot — it is a service layer.

**Lowest barrier to adoption.** For a lab that wants "LLM assistance in Fiji" with minimal infrastructure
change, this is the fastest path.

### CopilotJ

**Most architectural flexibility.** The custom ReAct loop gives fine-grained control over how agents
reason and retry. No external framework constrains the design.

**TOML-driven agent configuration.** New specialized AI agents are a config file, not a code change.
This is unusually accessible for a Python project of this complexity.

**Vision support.** It is the only project with working vision model integration — the AI can optionally
inspect the actual images being analyzed.

**Clean streaming architecture.** The NDJSON event stream means the web UI feels responsive and
live, with tool calls, agent handoffs, and reasoning all surfaced in real time.

**Observability.** Langfuse integration means API costs, latency, and model behavior are visible — 
important when running experiments or managing a shared deployment.

### Agentic-J

**Most capable at the actual analysis task.** The end goal — running a bioimage analysis pipeline — is
most directly served here. The system writes a script, runs it against a real Fiji instance, reads the
error, fixes the script, and tries again. This loop is what separates "advice" from "execution."

**Learns from experience.** The Librarian agent actively curates a growing wiki of verified patterns and
failure modes, organized by language (Groovy vs. Python) and promoted/demoted based on frequency and
reliability. Over many runs, the system gets better at the specific workflows it has seen before.

**Richest knowledge base.** The `skills/` folder contains documentation for ~25 Fiji plugins
(Cellpose, StarDist, TrackMate, BigStitcher, MorphoLibJ, ilastik, DeepImageJ, and more). This
structured knowledge is retrieved during script generation, not just general web search.

**Hybrid RAG.** Combining keyword (BM25) and semantic (embedding) search with score fusion gives more
reliable retrieval than either approach alone, especially for technical terms that embeddings may not
handle well.

**Structured workflow.** Pydantic handoff schemas between agents mean the supervisor always knows exactly
what a subagent produced (script path, success/failure, error type, which class failed, etc.). This makes
the multi-agent loop reliable rather than brittle.

---

## Usability Concerns

### fiji-llm
- **Capability ceiling.** There is no multi-agent system and no RAG. The assistant can answer questions and
  call tools, but it cannot iteratively write and debug a script, retrieve relevant documentation, or
  coordinate multiple AI specialists.
- **Single context window.** Long conversations hit token limits without summarization. Memory is managed
  by a sliding window — old context falls off.
- **No learning.** The system has no mechanism to improve based on past successes or failures.
- **Java-only extensibility.** The clean plugin system is only accessible to Java developers, which
  excludes most of the scientific Python community.

### CopilotJ
- **Complex deployment.** Keeping the aiohttp server, the Java plugin, and the web frontend in sync
  requires understanding of the bridge protocol. Failures in one component can be hard to diagnose.
- **Appose threading constraints.** When running inside Fiji's JVM (managed mode), the asyncio event loop
  runs on a daemon thread, which prevents use of certain OS APIs. This is a subtle footgun for contributors.
- **FAISS is not persistent across index rebuilds.** The RAG index must be rebuilt when documents change;
  there is no incremental update.
- **No structured handoffs.** Agents communicate via text, not typed schemas. This makes the interaction
  between agents less reliable than Agentic-J's Pydantic-typed approach.

### Agentic-J
- **Docker dependency.** The intended deployment is Docker. This is standard in software engineering but
  is a meaningful barrier for scientists managing their own Fiji installations.
- **OpenAI/OpenRouter-centric.** While Anthropic is wired in, local models (Ollama) are not directly
  supported — only via OpenRouter, which still requires an internet connection and API key.
- **No streaming.** Responses from the PySide6 GUI appear when complete, not incrementally. Long agent
  runs with no visible progress can feel unresponsive.
- **PyImageJ heap constraint.** The JVM is initialized with `-Xmx6g`. Large image datasets may exceed
  this, and the JVM heap cannot be expanded at runtime without restarting.
- **VLM is disabled.** The scaffolding exists but image inspection by the AI is not operational,
  despite being the most natural capability for a bioimage analysis tool.

---

## User and Developer Support

The following describes the state of documentation, community infrastructure, and maintainability signals
visible in the repositories. This is not an endorsement of project activity or future direction.

### fiji-llm

| | |
|---|---|
| **Maintainer org** | Fiji / ImageJ2 ecosystem (scijava, github.com/fiji) |
| **Build system** | Standard Maven + pom-scijava parent POM — well-documented across the ImageJ community |
| **Documentation for users** | README with Quick Start, User Guide, MCP setup; docs/ folder |
| **Documentation for developers** | Clear plugin interfaces with Javadoc; established SciJava extension patterns |
| **CI** | GitHub Actions |
| **Community** | image.sc forum (standard Fiji support channel) |
| **Framework support** | LangChain4j has active development and docs at langchain4j.dev |

### CopilotJ

| | |
|---|---|
| **Maintainer org** | neurogeom (github.com/neurogeom) |
| **Build system** | uv + Nix flake — reproducible but less familiar to most scientists |
| **Documentation for users** | `USER_MANUAL.md`, `README.md` |
| **Documentation for developers** | `AGENTS.md` is unusually detailed; TOML configs are self-documenting |
| **CI** | GitHub Actions |
| **Community** | Not publicly listed |
| **Framework support** | Custom framework — no external community; everything is documented in-repo |
| **Observability** | Langfuse tracing aids debugging and cost analysis |

### Agentic-J

| | |
|---|---|
| **Maintainer org** | MMV Lab / ISAS e.V. (github.com/mmv-lab) |
| **Build system** | conda + Docker — Docker Compose handles most deployment complexity |
| **Documentation for users** | `user_guide/` directory; `README.md`; intro message in the GUI itself |
| **Documentation for developers** | Skills folder doubles as developer-readable docs for each plugin |
| **CI** | Not evident from repository |
| **Community** | `agentj.help@gmail.com` contact; not yet on image.sc |
| **Framework support** | LangChain/LangGraph have the largest community and documentation of any AI agent framework; `deepagents` is less established |

---

## Summary: Which Should You Use?

There is no single right answer — the choice depends on your goal.

**Use fiji-llm if:**
- You want the simplest possible integration with the least infrastructure change
- Your users are already Fiji users who expect things to install like other plugins
- You want to expose Fiji capabilities to external AI tools via MCP
- You or your team writes Java and wants to extend the tool within the Fiji ecosystem
- Local/private model support is important

**Use CopilotJ if:**
- You want a flexible, multi-agent system with fine-grained control over reasoning
- You need vision model integration (AI inspecting actual images)
- You want a modern web interface and real-time streaming output
- You are comfortable with Python and want to extend via code or TOML config
- Observability (cost tracking, latency, model behavior) matters to your use case

**Use Agentic-J if:**
- Your primary goal is *automated* script generation and execution, not just chat
- You want the system to improve over time from accumulated runs
- You need rich plugin documentation built into the AI's knowledge base
- You want the most capable multi-agent system for end-to-end bioimage analysis
- Docker deployment is acceptable in your environment

**None of them are a clear choice if:**
- You need a fully local/private system with no cloud dependency (only fiji-llm with Ollama comes close)
- You need VLM/vision features working out of the box (CopilotJ requires enabling; Agentic-J needs re-enabling)
- You need a graphical workflow builder rather than natural-language description
