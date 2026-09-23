# Task: Spring AI training curriculum — code per thematic unit

Source scope: `docs/spring-ai-scope.md` (14 training hours, 7 thematic units incl. the closing
showcase). Builds on the Spring Boot wave (`.claude/tasks/todo-spring-boot.md`) but ships as its
own module — no code here should be required for the Spring Boot units to build or pass.

## Agreed decisions

- **Modules**: three new runnable modules alongside `spring-boot-training`:
	- `spring-ai-training` (`gr.codelearn.springbootai.ai`) — U1–U5 core AI topics, plus the MCP *client* half of U6.
	- `spring-ai-mcp-server-training` (`gr.codelearn.springbootai.ai.mcp`) — minimal MCP server for
	  U6's server basics, called by `spring-ai-training`'s client.
	- `spring-ai-mcp-server-showcase` (`gr.codelearn.springbootai.ai.mcp` — same root package as
	  `spring-ai-mcp-server-training`, intentional: they're separate modules/jars, never on the same
	  classpath together) — U7's standalone showcase server, connected to Claude Code.
- **Infra reuse**: `spring-ai-training` reuses `spring-boot-training`'s Oracle 23ai instance and
  `catalog` domain data for the RAG unit (U5) — no separate datasource/compose service for the core
  module. Also settles the vector store question: Oracle 23ai AI Vector Search (`spring-ai-starter-vector-store-oracle`), same instance, per
  CLAUDE.md's existing default.
- **Cloud/online model runtime (U2)**: Anthropic Claude (Sonnet), via `spring-ai-starter-model-anthropic`
  and an `ANTHROPIC_API_KEY` env var (never committed — per CLAUDE.md secrets rule).
- **Offline/local model runtime (U2)**: Ollama, via the Spring AI Ollama starter + a `compose.yaml`
  service.
- **MCP transport**: HTTP (not stdio) for both `spring-ai-mcp-server-training` and
  `spring-ai-mcp-server-showcase`.
- **U7 is vanilla**: `spring-ai-mcp-server-showcase` does not reuse sample code from
  `spring-ai-training` / `spring-ai-mcp-server-training` — built fresh when U7 starts.
- **Claude Code wiring for U7**: a project-scoped `.mcp.json` in a specific (separate) Claude Code
  project, not the user's global config.

## Open questions (decide at U7)

- **U7 tool content.** What real capability the showcase server exposes to Claude Code — deferred
  on purpose until U7 starts; only constraint so far is *U7 is vanilla* above.

## Pre-step

- [ ] Scaffold `spring-ai-training` via the `maven-scaffold` skill (runnable module:
  `spring-boot-maven-plugin`, register in parent `<modules>`), Spring AI BOM/starter baseline,
  compile + `./mvnw verify` green with just the bootstrap app. `spring-ai-mcp-server-training` and
  `spring-ai-mcp-server-showcase` are scaffolded later, at U6/U7 respectively — no need to stand
  them up before their units start.

## U1 — Spring AI Core Concepts (10h)

- [ ] **1.1 Spring AI architecture** — starter/BOM overview, model-provider abstraction, a short
  doc/README section on the `ChatModel` → `ChatClient` → `Advisor` layering, where prompts (`src/main/resources/prompts/*.st`) and config
  (`application.yml` `spring.ai.*`) live.
- [ ] **1.2 ChatClient** — a central `ChatClient` bean built from `ChatClient.Builder` in
  `config/ai` (per CLAUDE.md — no `new`-ing model clients in features), a first `*Tools`-free
  chat endpoint exercising it end to end.
- [ ] **1.3 Models / messages** — `SystemMessage` / `UserMessage` / `AssistantMessage` /
  `ToolResponseMessage` walkthrough, `Prompt` construction, streaming vs blocking `call()`
  response handling.
- [ ] **1.4 System vs user instructions** — system prompt template loaded as a `Resource`,
  contrasted with per-request user instructions, a demo showing system-prompt drift/override
  pitfalls.

## U2 — Offline & Hybrid LLM Deployment

- [ ] **2.1 Cloud vs offline models** — comparison notes (latency, cost, data residency) contrasting
  Anthropic Claude (cloud) vs Ollama (offline), a `ChatModel` bean selection driven by config
  (`spring.ai.model.chat` / a custom property) rather than hardcoded provider.
- [ ] **2.2 Model selection & quantization** — a written note on model size/quantization
  trade-offs for local inference, picking a small instruct model for the offline demo.
- [ ] **2.3 Docker deployment** — an Ollama service wired into `compose.yaml`, health-checked like
  the existing Oracle service, plus the Spring AI Ollama starter dependency.
- [ ] **2.4 Switching between cloud / local** — a `@ConfigurationProperties`-driven profile switch
  (`application-local.yml` on Ollama vs a cloud profile on Anthropic) that swaps the active
  `ChatModel` without code changes, plus a resilience note (Spring Retry / timeouts) for the
  Anthropic-backed path.

## U3 — Prompt Engineering

- [ ] **3.1 Prompt templates** — `PromptTemplate` / `.st` resource files under
  `src/main/resources/prompts`, variable substitution, a reusable template per use case.
- [ ] **3.2 Instruction vs context** — separating fixed instructions from per-call retrieved/user
  context in the same prompt, a demo contrasting a context-stuffed vs instruction-only prompt.
- [ ] **3.3 Structured outputs** — Spring AI's structured output converter (`BeanOutputConverter`
  or `.entity(...)` on `ChatClient`) mapping a completion straight to a record, with a validation
  failure/retry path.

## U4 — Conversational AI & Memory

- [ ] **4.1 Stateless vs stateful conversations** — a stateless single-turn endpoint next to a
  stateful multi-turn one, showing the difference explicitly in the request/response shape.
- [ ] **4.2 Memory lifecycle** — `ChatMemory` (message-window or repository-backed) wired as an
  `Advisor`, memory eviction/window-size config, a note on where memory is persisted (in-memory
  vs a DB-backed `ChatMemoryRepository`) and its lifecycle across sessions.

## U5 — RAG Foundations & Basic Monitoring

- [ ] **5.1 RAG pipeline overview** — an end-to-end query → retrieval → context → generation
  slice using `QuestionAnswerAdvisor`, documented as the reference flow.
- [ ] **5.2 Chunking strategies** — a document ingestion step (`TokenTextSplitter` or similar),
  comparing at least two chunking configs and their effect on retrieval quality.
- [ ] **5.3 Logging** — structured logging around the RAG pipeline stages (retrieval hits, chunk
  count) at DEBUG/local only, honoring the "no full prompts/completions at INFO outside local"
  rule from CLAUDE.md.
- [ ] **5.4 Token usage monitoring** — `spring.ai.chat.observations.*` enabled, token-usage
  metrics/spans flowing to the existing Grafana LGTM stack (reusing the Boot module's
  observability wiring where possible).

## U6 — Tool Calling & MCP

- [ ] **6.1 Tools vs APIs** — a `@Tool`-annotated method on a dedicated `*Tools` component,
  contrasted with calling the same capability as a plain REST endpoint, to make the distinction
  concrete.
- [ ] **6.2 Validation** — Jakarta Bean Validation on tool-call input records, a rejected-call
  demo showing the model receiving a validation error back.
- [ ] **6.3 Error handling** — a tool that can fail (e.g. an external lookup), translated into a
  model-legible error response rather than an unhandled exception.
- [ ] **6.4 MCP client / server basics** — scaffold `spring-ai-mcp-server-training` (Spring AI MCP
  server starter, HTTP transport, one or two toy tools), then an MCP client in `spring-ai-training`
  (Spring AI MCP client starter, HTTP) that calls it. Run both concurrently and demo the round trip.

## U7 — Showcase: Create an MCP Server (4h)

- [ ] **7.1 MCP server** — scaffold `spring-ai-mcp-server-showcase` (own module, HTTP transport),
  exposing real tools per the *Open questions* (U7 tool content), and connect it to Claude Code (per the *Open questions* — Claude Code
  wiring) for a live demo.

## Definition of done (per topic and overall)

- `./mvnw verify` passes; touched files reformatted via the `idea` MCP (no Spotless — see
  lessons.md).
- Every `ChatClient` built centrally (`config/ai`), never `new`-ed in a feature; every `@Tool`
  method lives on a dedicated `*Tools` component.
- No full prompts/completions logged at INFO outside the `local` profile.
- Code follows CLAUDE.md conventions (records vs Lombok, package-by-feature, constructor
  injection, no entities/domain objects leaking across feature boundaries).

## Review

_(empty until the first unit is done — see `.claude/tasks/todo-spring-boot.md` for the entry
format.)_