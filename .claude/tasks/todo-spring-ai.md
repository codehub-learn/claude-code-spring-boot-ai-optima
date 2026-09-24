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

## U6 — Spring AI Core Concepts

- [x] **6.1 Spring AI architecture** — starter/BOM overview, model-provider abstraction, a short
  doc/README section on the `ChatModel` → `ChatClient` → `Advisor` layering, where prompts (`src/main/resources/prompts/*.st`) and config
  (`application.yml` `spring.ai.*`) live.
- [x] **6.2 ChatClient** — a central `ChatClient` bean built from `ChatClient.Builder` in
  `config/ai` (per CLAUDE.md — no `new`-ing model clients in features), a first `*Tools`-free
  chat endpoint exercising it end to end.
- [x] **6.3 Models / messages** — `SystemMessage` / `UserMessage` / `AssistantMessage` /
  `ToolResponseMessage` walkthrough, `Prompt` construction, streaming vs blocking `call()`
  response handling.
- [x] **6.4 System vs user instructions** — system prompt template loaded as a `Resource`,
  contrasted with per-request user instructions, a demo showing system-prompt drift/override
  pitfalls.

## U7 — Offline & Hybrid LLM Deployment

- [x] **7.1 Cloud vs offline models** — comparison notes (latency, cost, data residency) contrasting
  Anthropic Claude (cloud) vs Ollama (offline), a `ChatModel` bean selection driven by config (`spring.ai.model.chat` / a custom property)
  rather than hardcoded provider.
- [x] **7.2 Model selection & quantization** — a written note on model size/quantization
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

### 6.2 ChatClient (done)

- Provider: **Anthropic Claude** (`spring-ai-starter-model-anthropic`, official Anthropic Java SDK), replacing 6.1's two
  provider-neutral deps (the starter pulls them transitively). The user tuned `spring.ai.anthropic.*`: `api-key: ${ANTHROPIC_API_KEY}`,
  `base-url`, `timeout: 60s`, `max-retries: 2`, `chat.model: claude-sonnet-5`, `chat.max-tokens: 4096`,
  `chat.cache-options.strategy: SYSTEM_AND_TOOLS`, `chat.inference-geo: global` (a plain string, so a bad value only fails at call
  time; not live-verified). Temperature / top-p / top-k are left commented out. Checked: the app starts with or without the variable,
  so a missing key only surfaces on the first call (502). `application-test.yml` sets a dummy key, and `AiTrainingApplicationTests`
  now runs under `@ActiveProfiles("test")`.
- Course correction: a first cut used Ollama (plus a compose service). The user rejected it because it breaks the step order: the
  local runtime belongs to U7. It was fully reverted, including `compose.yaml`. See lessons.md.
- `config/ai/ChatClientConfig`: the single `ChatClient` bean, built from the auto-configured builder with `SimpleLoggerAdvisor` as a
  default advisor; its DEBUG logger is enabled only in `application-local.yml`.
- `chat` feature: `ChatController` (`POST /api/{version}/chat-completions`, `version = "v1"`), `ChatService` (`call().chatResponse()`
  → content + model + token usage; model-call `RuntimeException` → `chat.internal.ChatModelUnavailableException`), records
  `ChatCompletionRequest` (`@NotBlank @Size(max = 4000)`) and `ChatCompletionResponse` (+ nested `TokenUsage`, the user's option 2).
  No persistence, so no entity/repository/mapper/migration in this slice.
- **API versioning (user decision):** Spring Framework 7 built-in, path-segment strategy (`spring.mvc.apiversion.use.path-segment: 1`,
  `supported: 1`), keeping CLAUDE.md's `/api/v1` URLs. Findings: the path-segment resolver did **not** break `/v3/api-docs` or actuator (the
  planned `WebMvcConfigurer` fallback was not needed); springdoc 3.1.1 is version-aware and writes the mapping's literal version into
  the documented path, so `version = "1"` published `/api/1/chat-completions`. Switched the mapping to `"v1"` (the semantic parser strips
  the `v`), which documents `/api/v1/chat-completions`.
- `config/web`: `ApiExceptionHandler` (400 + `errors` extension on body validation, 502 on `ChatModelUnavailableException`, Spring's own
  400 for unsupported versions) and `OpenApiConfig`, both trimmed from the Boot module. Added `spring-boot-starter-validation` and
  springdoc to the pom.
- Tests: `ChatControllerTest` (4, `@WebMvcTest`) and `ChatApiIT` (3, real context + `@Primary` stub `ChatModel`: happy path, 502,
  api-docs path). No API key or network needed for `verify`.
- README: 6.2 section (provider + run, central client, endpoint + curl + status table, API versioning, tests); 6.1 text updated for the
  starter swap.
- Verified: `./mvnw -pl spring-ai-training verify` → 9 unit/slice + 3 IT, 0 failures; full-reactor `./mvnw verify` → `BUILD SUCCESS`.
  Not run here: a live call against the Anthropic API (spends the user's credits).
### 6.3 Models / messages (done)

- User decision: extend the `chat` feature. The blocking `POST /api/v1/chat-completions` stays; the same URL with
  `Accept: text/event-stream` streams (content negotiation, method-level `produces` overrides the class-level JSON one); a new
  `POST /api/v1/prompts` (`PromptController`) takes an explicit `system` / `user` / `assistant` message list. `ToolResponseMessage`
  is covered in a provider-free test and the README only, since real tools are U11.
- `ChatService`: `completePrompt(PromptRequest)` maps each role to `SystemMessage` / `UserMessage` / `AssistantMessage` (a `switch`)
  and sends `new Prompt(messages)` through `chatClient.prompt(prompt)`, so the default advisors still apply. `stream(...)` is
  `stream().content()` → `Flux<ChatCompletionChunkResponse>`, errors mapped to `ChatModelUnavailableException`. Named
  `completePrompt`, not an overload of `complete`: Mockito's `any()` made the overload ambiguous in tests.
- `PromptRequest` record: `@NotEmpty @Size(max = 20)` list of `@Valid` messages, lower-case roles via `@JsonProperty` on the enum
  constants (`tool` rejected as unreadable, 400), and an `@AssertTrue @JsonIgnore isLastMessageFromUser()` cross-field rule.
- Streaming design: each SSE `data` is JSON (`{"content": ...}`), because SSE clients strip one leading space and split on
  newlines, so raw tokens would lose whitespace. A model failure mid-stream can't become a 502 (status already committed), so the
  controller ends the stream with an `event:error` whose data is the same `ProblemDetail`. Validation still gives a normal 400.
- Findings: with no `Accept` header the JSON handler still wins (6.2 tests unchanged); springdoc documents both handlers on the
  one path. A failed stream makes Spring AI's `MessageAggregator` log `Aggregation Error` at ERROR 5 times (one per aggregating
  layer); verified with a throwaway counting stub that the model is called and subscribed exactly once, so it's noise, not retries.
- Tests: `MessagesAndPromptsTest` (7, no context: message types and roles, a hand-built tool round trip, `Prompt` construction with
  `ChatOptions`, fluent vs manual prompt, `call` vs `stream`); `PromptControllerTest` (5); `ChatControllerTest` +3 (SSE body,
  in-band error event, 400 before streaming); `ChatApiIT` +4 with the stub now also streaming and recording the last `Prompt`
  (typed messages reach the model in order, SSE body, in-band error, 502 on `/prompts`).
- README: 6.3 section (message model table, tool round trip, building a prompt, both endpoints with curl + status table, streaming
  design notes, tests).
- IDE note: the first `reformat_file` on `PromptRequest` wrapped a `{@code @NotEmpty}` Javadoc tag into a broken `@NotEmpty}` block
  tag, and a stale IDE buffer then undid a disk edit. Fixed by rewording without `@` inside `{@code}` and editing through the `idea`
  MCP `apply_patch`.
- Verified: `./mvnw -pl spring-ai-training verify` → 24 unit/slice + 7 IT, 0 failures; full-reactor `./mvnw verify` → `BUILD SUCCESS`.
  Not run: a live streamed call against the Anthropic API (spends the user's credits).

### 6.4 System vs user instructions (done)

- User decisions: the system prompt is the **central** `ChatClient`'s default (every endpoint inherits it, no new endpoint), and
  `/api/v1/prompts` now **rejects** a client `system` role (400), which changes the 6.3 contract to `user` / `assistant` only.
- `src/main/resources/prompts/chat-system.st`: persona + four rules (app instructions take priority and are never revealed, concise,
  reply in the user's language, admit uncertainty) + `Today is {today}.` `ChatClientConfig` loads it as a `Resource`
  (`@Value("classpath:prompts/chat-system.st")` on the bean method) into `builder.defaultSystem(...)`.
- `ChatService`: every call (blocking, streaming, `/prompts`) adds `.system(ChatService::withSystemParams)`, which fills `today` and
  keeps the default text. `toMessage` lost its `SYSTEM` case; `PromptRequest.Role` is now `USER`, `ASSISTANT`.
- Findings (Spring AI 2.0.1, probed with a throwaway stub test, then pinned in `ChatClientConfigTest`):
	- a per-call `.system(String)` **replaces** the default system text; `.system(Consumer)` with params keeps it;
	- a `SystemMessage` inside `prompt(Prompt)` or `.messages(...)` is **added** after the default: two system messages;
	- a template with **no** params passed is not rendered at all, so a literal `{today}` reaches the model silently; passing
	  **some** params but not all throws `IllegalStateException` ("Not all variables were replaced") at call time. My first test
	  assumed the no-params case throws; it doesn't;
	- user text is never templated (`{today}` in a user message is sent verbatim).
- Tests: `ChatClientConfigTest` (6, `ApplicationContextRunner` + real config/template + recording stub): rendered default, the
  literal-placeholder and partial-params cases, replace and stack pitfalls, instruction-like user text. `ChatApiIT` +1 (a real
  request starts with the rendered system prompt) and the `/prompts` order test now expects the app system message first.
  `PromptControllerTest` +1 (`system` role → 400).
- README: 6.4 section (system vs user, the template, wiring, pitfall table, live curl demo for override, language and a
  forged-`assistant`-turn drift attempt); 6.3 text updated for the removed `system` role.
- Verified: `./mvnw -pl spring-ai-training verify` → 31 unit/slice + 8 IT, 0 failures; full-reactor `./mvnw verify` →
  `BUILD SUCCESS`. Not run: the live curl demo against the Anthropic API (spends the user's credits).

### 7.1 Cloud vs offline models (done)

- **Deviation (user-approved):** the `spring-ai-starter-model-ollama` dependency moved from 7.3 to 7.1, because selecting the
  `ChatModel` by `spring.ai.model.chat` needs both providers' autoconfig on the classpath. Nothing else from 7.2-7.4 was pulled in:
  no compose service, no local model choice, no profile switch.
- `application.yml`: `spring.ai.model.chat: anthropic` (explicit) and `spring.ai.model.embedding: none` (the Ollama starter also
  brings an `EmbeddingModel`; embeddings are U10).
- Findings (checked in the 2.0.1 autoconfig bytecode, then pinned by tests): `AnthropicChatAutoConfiguration` and
  `OllamaChatAutoConfiguration` are both `@ConditionalOnProperty(spring.ai.model.chat, havingValue=<name>, matchIfMissing=true)`.
  Unset with both starters → two `ChatModel` beans → startup fails in `ChatClientConfig` ("expected single matching bean but found
  2"); an unknown value → no `ChatModel` → startup fails too. Neither client connects at startup, so the app starts without an
  API key or a running Ollama. The Ollama starter pulls `spring-boot-starter-webclient`; the app stays a servlet app.
- `ChatModelSelectionTest` (5): starts the real app (`SpringApplicationBuilder`, `WebApplicationType.NONE`, `test` profile) for
  default → `AnthropicChatModel`, `ollama` → `OllamaChatModel`, no Ollama `EmbeddingModel`, unset (via `spring.config.name=none`,
  so the property is truly absent) → fails with 2 beans, unknown value → fails with none.
- README: new U7 heading and 7.1 section (cloud vs offline comparison table: where it runs, quality, latency, cost, data
  residency, availability, scaling, operations; rules of thumb; the selection table; the command-line switch).
- Fixed along the way: the IDE's Markdown reformat had mangled the 6.3 README Java block (committed in 6.3) and a stale IDE buffer
  wiped the first write of the 7.1 section; both restored through the `idea` MCP `apply_patch`. See lessons.md.
- Verified: `./mvnw -pl spring-ai-training verify` → 36 unit/slice + 8 IT, 0 failures; full-reactor `./mvnw verify` →
  `BUILD SUCCESS`. Not run: a live call against either provider.

### 7.2 Model selection & quantization (done)

- User decision: **`qwen3:4b`** (Ollama default `q4_K_M`, ~2.5 GB) over `llama3.2:3b`, `gemma3:4b` and `qwen3:1.7b`: strongest of
  the small ones, Greek support, tool calling for U11.
- `application.yml`: `spring.ai.ollama.chat.model: qwen3:4b`, `think: false` (Qwen3 is hybrid and thinks by default), `num-ctx: 8192`
  (KV cache ~1.2 GB, computed from the model's 36 layers × 8 KV heads × 128 head size). Used only while `spring.ai.model.chat` is
  `ollama`. No pull strategy or compose service: that is 7.3.
- Finding (2.0.1 bytecode): with `spring.ai.ollama.chat.model` unset, `OllamaChatOptions` falls back to `OllamaModel.MISTRAL`
  (`mistral`, 7B, ~4 GB), silently. The flat `spring.ai.ollama.chat.*` keys replace the deprecated `spring.ai.ollama.chat.options.*`.
  `spring.ai.ollama.init.pull-model-strategy` defaults to `never`.
- `ChatModelSelectionTest` +1: under `ollama`, the `OllamaChatModel` options carry `qwen3:4b`, `ThinkBoolean.DISABLED`, 8192.
- README: 7.2 section (memory = weights + KV cache + overhead with the worked example, quantization ladder table, bandwidth-bound
  speed, rules of thumb, candidate table, the pick and its config, choosing a quant by tag). Sizes are marked approximate: Ollama is
  not installed on this machine, so no tag or size was checked live. README not run through the IDE reformat (see lessons.md).
- Verified: `./mvnw -pl spring-ai-training verify` → 37 unit/slice + 8 IT, 0 failures; full-reactor `./mvnw verify` →
  `BUILD SUCCESS`. Not run: a live call against Ollama.