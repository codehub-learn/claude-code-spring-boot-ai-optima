# Task: Spring AI training curriculum — code per thematic unit

Source scope: `docs/spring-ai-scope.md` (14 training hours, 7 thematic units incl. the closing
showcase). Builds on the Spring Boot wave (`.claude/tasks/todo-spring-boot.md`) but ships as its
own module — no code here should be required for the Spring Boot units to build or pass.

## Agreed decisions

- **Modules**: three new runnable modules alongside `spring-boot-training`:
	- `spring-ai-training` (`gr.codelearn.springbootai.ai`) — U6–U10 core AI topics, plus the MCP *client* half of U11.
	- `spring-ai-mcp-server-training` (`gr.codelearn.springbootai.ai.mcp`) — minimal MCP server for
	  U11's server basics, called by `spring-ai-training`'s client.
	- `spring-ai-mcp-server-showcase` (`gr.codelearn.springbootai.ai.mcp` — same root package as
	  `spring-ai-mcp-server-training`, intentional: they're separate modules/jars, never on the same
	  classpath together) — U12's standalone showcase server, connected to Claude Code.
- **Infra reuse**: `spring-ai-training` reuses `spring-boot-training`'s Oracle 23ai instance and
  `catalog` domain data for the RAG unit (U10) — no separate datasource/compose service for the core
  module. Also settles the vector store question: Oracle 23ai AI Vector Search (`spring-ai-starter-vector-store-oracle`), same instance, per
  CLAUDE.md's existing default.
- **Cloud/online model runtime (U7)**: Anthropic Claude (Sonnet), via `spring-ai-starter-model-anthropic`
  and an `ANTHROPIC_API_KEY` env var (never committed — per CLAUDE.md secrets rule).
- **Offline/local model runtime (U7)**: Ollama, via the Spring AI Ollama starter + a `compose.yaml`
  service.
- **MCP transport**: HTTP (not stdio) for both `spring-ai-mcp-server-training` and
  `spring-ai-mcp-server-showcase`.
- **U12 is vanilla**: `spring-ai-mcp-server-showcase` does not reuse sample code from
  `spring-ai-training` / `spring-ai-mcp-server-training` — built fresh when U12 starts.
- **Claude Code wiring for U12**: a project-scoped `.mcp.json` in a specific (separate) Claude Code
  project, not the user's global config.

## Open questions (decide at U12)

- **U12 tool content.** What real capability the showcase server exposes to Claude Code — deferred
  on purpose until U12 starts; only constraint so far is *U12 is vanilla* above.

## U5 — Pre-step

- [x] **5 Module scaffold** — Scaffold `spring-ai-training` via the `maven-scaffold` skill (runnable module:
  `spring-boot-maven-plugin`, register in parent `<modules>`), Spring AI BOM/starter baseline,
  compile + `./mvnw verify` green with just the bootstrap app. `spring-ai-mcp-server-training` and
  `spring-ai-mcp-server-showcase` are scaffolded later, at U11/U12 respectively — no need to stand
  them up before their units start.

## U6 — Spring AI Core Concepts (10h)

- [x] **6.1 Spring AI architecture** — starter/BOM overview, model-provider abstraction, a short
  doc/README section on the `ChatModel` → `ChatClient` → `Advisor` layering, where prompts (`src/main/resources/prompts/*.st`) and config
  (`application.yml` `spring.ai.*`) live.
- [ ] **6.2 ChatClient** — a central `ChatClient` bean built from `ChatClient.Builder` in
  `config/ai` (per CLAUDE.md — no `new`-ing model clients in features), a first `*Tools`-free
  chat endpoint exercising it end to end.
- [ ] **6.3 Models / messages** — `SystemMessage` / `UserMessage` / `AssistantMessage` /
  `ToolResponseMessage` walkthrough, `Prompt` construction, streaming vs blocking `call()`
  response handling.
- [ ] **6.4 System vs user instructions** — system prompt template loaded as a `Resource`,
  contrasted with per-request user instructions, a demo showing system-prompt drift/override
  pitfalls.

## U7 — Offline & Hybrid LLM Deployment

- [ ] **7.1 Cloud vs offline models** — comparison notes (latency, cost, data residency) contrasting
  Anthropic Claude (cloud) vs Ollama (offline), a `ChatModel` bean selection driven by config (`spring.ai.model.chat` / a custom property)
  rather than hardcoded provider.
- [ ] **7.2 Model selection & quantization** — a written note on model size/quantization
  trade-offs for local inference, picking a small instruct model for the offline demo.
- [ ] **7.3 Docker deployment** — an Ollama service wired into `compose.yaml`, health-checked like
  the existing Oracle service, plus the Spring AI Ollama starter dependency.
- [ ] **7.4 Switching between cloud / local** — a `@ConfigurationProperties`-driven profile switch (`application-local.yml` on Ollama vs a
  cloud profile on Anthropic) that swaps the active
  `ChatModel` without code changes, plus a resilience note (Spring Retry / timeouts) for the
  Anthropic-backed path.

## U8 — Prompt Engineering

- [ ] **8.1 Prompt templates** — `PromptTemplate` / `.st` resource files under
  `src/main/resources/prompts`, variable substitution, a reusable template per use case.
- [ ] **8.2 Instruction vs context** — separating fixed instructions from per-call retrieved/user
  context in the same prompt, a demo contrasting a context-stuffed vs instruction-only prompt.
- [ ] **8.3 Structured outputs** — Spring AI's structured output converter (`BeanOutputConverter`
  or `.entity(...)` on `ChatClient`) mapping a completion straight to a record, with a validation
  failure/retry path.

## U9 — Conversational AI & Memory

- [ ] **9.1 Stateless vs stateful conversations** — a stateless single-turn endpoint next to a
  stateful multi-turn one, showing the difference explicitly in the request/response shape.
- [ ] **9.2 Memory lifecycle** — `ChatMemory` (message-window or repository-backed) wired as an
  `Advisor`, memory eviction/window-size config, a note on where memory is persisted (in-memory
  vs a DB-backed `ChatMemoryRepository`) and its lifecycle across sessions.

## U10 — RAG Foundations & Basic Monitoring

- [ ] **10.1 RAG pipeline overview** — an end-to-end query → retrieval → context → generation
  slice using `QuestionAnswerAdvisor`, documented as the reference flow.
- [ ] **10.2 Chunking strategies** — a document ingestion step (`TokenTextSplitter` or similar),
  comparing at least two chunking configs and their effect on retrieval quality.
- [ ] **10.3 Logging** — structured logging around the RAG pipeline stages (retrieval hits, chunk
  count) at DEBUG/local only, honoring the "no full prompts/completions at INFO outside local"
  rule from CLAUDE.md.
- [ ] **10.4 Token usage monitoring** — `spring.ai.chat.observations.*` enabled, token-usage
  metrics/spans flowing to the existing Grafana LGTM stack (reusing the Boot module's
  observability wiring where possible).

## U11 — Tool Calling & MCP

- [ ] **11.1 Tools vs APIs** — a `@Tool`-annotated method on a dedicated `*Tools` component,
  contrasted with calling the same capability as a plain REST endpoint, to make the distinction
  concrete.
- [ ] **11.2 Validation** — Jakarta Bean Validation on tool-call input records, a rejected-call
  demo showing the model receiving a validation error back.
- [ ] **11.3 Error handling** — a tool that can fail (e.g. an external lookup), translated into a
  model-legible error response rather than an unhandled exception.
- [ ] **11.4 MCP client / server basics** — scaffold `spring-ai-mcp-server-training` (Spring AI MCP
  server starter, HTTP transport, one or two toy tools), then an MCP client in `spring-ai-training`
  (Spring AI MCP client starter, HTTP) that calls it. Run both concurrently and demo the round trip.

## U12 — Showcase: Create an MCP Server (4h)

- [ ] **12.1 MCP server** — scaffold `spring-ai-mcp-server-showcase` (own module, HTTP transport),
  exposing real tools per the *Open questions* (U12 tool content), and connect it to Claude Code (per the *Open questions* — Claude Code
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

### 5 Module scaffold (done)

- Registered `spring-ai-training` in the parent `<modules>` and scaffolded it as a runnable code
  module (`gr.codelearn.springbootai.ai`), modeled on `spring-boot-training`'s pom/app/resources
  shape but stripped down: `spring-boot-starter-webmvc` + `-actuator` + devtools only, no JPA,
  Flyway, Oracle driver, or Spring AI starter yet — the `spring-ai-bom` is already available via
  the parent's `<dependencyManagement>` for when U6.2/U7.1 add a model starter.
- Deviation from a literal reading of the checklist item: per user decision, no Spring AI model
  starter (e.g. `spring-ai-starter-model-anthropic`) was added in this step, to avoid a provider
  autoconfiguration / API-key dependency in the bootstrap's context-loading smoke test. That lands
  with U6.2 (`ChatClient`) / U7.1 (cloud vs offline model selection).
- Added `AiTrainingApplication` + `StartupListener` (mirrors the sibling module's bootstrap
  pattern), `application.yml` / `-local` / `-test`, `banner.txt`, and a plain `@SpringBootTest`
  `AiTrainingApplicationTests.contextLoads()` (no `AbstractOracleIntegrationTest` base — no
  datasource in this module yet).
- Verified: `./mvnw -pl spring-ai-training -am verify` → `BUILD SUCCESS`, 1 test run, 0
  failures/errors. All new/touched files reformatted via the `idea` MCP.
- Follow-up found while checking `./mvnw verify` from the repo root (not just `-pl
  spring-ai-training`): `spring-boot-training`'s `LoggingBeanPostProcessorTest` failed
  deterministically. Root cause was leftover fallout from the earlier base-package rename (`gr.codelearn.springbootai.training` →
  `gr.codelearn.springbootai.boot`, commit `9c8e9b7`) that
  the rename commit missed in five spots: `LoggingBeanPostProcessor`'s package filter (the actual
  test failure), the `logging.level` keys in `application-local.yml` and `application-test.yml`
  (silently meant those overrides never applied), springdoc's `packages-to-scan` in
  `application.yml` (would scan the wrong package for controllers), and a stale Javadoc example in
  `ProductPriceView`. All five fixed to `gr.codelearn.springbootai.boot`. Confirmed unrelated to
  the new module: reproduced with `spring-ai-training` fully `git stash`-ed out. A separate,
  one-off Testcontainers Oracle startup timeout (Docker resource contention from a concurrently
  running `compose.yaml` Oracle) was transient and cleared on retry — not a code issue.
- Full-reactor `./mvnw verify` (repo root, all three modules) now passes: `BUILD SUCCESS`.

### 6.1 Spring AI architecture (done)

- Per user decision: README plus a provider-free test, no provider starter yet (that stays with 6.2 / 7.1).
- `spring-ai-training/pom.xml`: added `spring-ai-client-chat` + `spring-ai-autoconfigure-model-chat-client` (BOM-managed,
  2.0.1), exactly the provider-neutral pair every `spring-ai-starter-model-*` pulls transitively. `<description>` renumbered to
  U6-U10 / U11.
- `SpringAiArchitectureTest` (`ApplicationContextRunner` + `ChatClientAutoConfiguration` + a stub `ChatModel` lambda): builder
  auto-configured over the model; a call traces `advisor:before → model → advisor:after` through a recording `CallAdvisor` +
  `SimpleLoggerAdvisor`; the model receives a `Prompt` of one `SystemMessage` + one `UserMessage`; `spring.ai.chat.client.enabled=false`
  removes the builder.
- Finding (checked with `javap` on the 2.0.1 jar): `ChatClientAutoConfiguration.chatClientBuilder` is `@Scope("prototype")` with
  no `@ConditionalOnBean(ChatModel)`. Without a `ChatModel` the context still starts (so `contextLoads` needed no exclude), but
  the first `ChatClient.Builder` injection fails with `UnsatisfiedDependencyException`. Asserted in the test, noted in the README.
- New `spring-ai-training/README.md` (shape of the Boot module's): 6.1 section on the BOM, starter families, the provider
  abstraction (`ChatModel` / `StreamingChatModel` / `EmbeddingModel`, `spring.ai.model.chat`), the ChatClient → Advisor →
  ChatModel layering diagram, and where prompts / config / keys / tools live.
- Verified: `./mvnw -pl spring-ai-training verify` → 5 tests, 0 failures; full-reactor `./mvnw verify` → `BUILD SUCCESS`.