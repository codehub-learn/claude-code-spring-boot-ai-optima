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
- [x] **7.3 Docker deployment** — an Ollama service wired into `compose.yaml`, health-checked like
  the existing Oracle service, plus the Spring AI Ollama starter dependency.
- [x] **7.4 Switching between cloud / local** — a `@ConfigurationProperties`-driven profile switch (`application-local.yml` on Ollama vs a
  cloud profile on Anthropic) that swaps the active
  `ChatModel` without code changes, plus a resilience note (Spring Retry / timeouts) for the
  Anthropic-backed path.

## U8 — Prompt Engineering

- [x] **8.1 Prompt templates** — `PromptTemplate` / `.st` resource files under
  `src/main/resources/prompts`, variable substitution, a reusable template per use case.
- [x] **8.2 Instruction vs context** — separating fixed instructions from per-call retrieved/user
  context in the same prompt, a demo contrasting a context-stuffed vs instruction-only prompt.
- [x] **8.3 Structured outputs** — Spring AI's structured output converter (`BeanOutputConverter`
  or `.entity(...)` on `ChatClient`) mapping a completion straight to a record, with a validation
  failure/retry path.

## U9 — Conversational AI & Memory

- [x] **9.1 Stateless vs stateful conversations** — a stateless single-turn endpoint next to a
  stateful multi-turn one, showing the difference explicitly in the request/response shape.
- [x] **9.2 Memory lifecycle** — `ChatMemory` (message-window or repository-backed) wired as an
  `Advisor`, memory eviction/window-size config, a note on where memory is persisted (in-memory
  vs a DB-backed `ChatMemoryRepository`) and its lifecycle across sessions.

## U10 — RAG Foundations & Basic Monitoring

- [x] **10.1 RAG pipeline overview** — an end-to-end query → retrieval → context → generation
  slice using `QuestionAnswerAdvisor`, documented as the reference flow.
- [x] **10.2 Chunking strategies** — a document ingestion step (`TokenTextSplitter` or similar),
  comparing at least two chunking configs and their effect on retrieval quality.
- [x] **10.3 Logging** — structured logging around the RAG pipeline stages (retrieval hits, chunk
  count) at DEBUG/local only, honoring the "no full prompts/completions at INFO outside local"
  rule from CLAUDE.md.
- [x] **10.4 Token usage monitoring** — `spring.ai.chat.observations.*` enabled, token-usage
  metrics/spans flowing to the existing Grafana LGTM stack (reusing the Boot module's
  observability wiring where possible).

## U11 — Tool Calling & MCP

- [x] **11.1 Tools vs APIs** — a `@Tool`-annotated method on a dedicated `*Tools` component,
  contrasted with calling the same capability as a plain REST endpoint, to make the distinction
  concrete.
- [x] **11.2 Validation** — Jakarta Bean Validation on tool-call input records, a rejected-call
  demo showing the model receiving a validation error back.
- [x] **11.3 Error handling** — a tool that can fail (e.g. an external lookup), translated into a
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

### 7.3 Docker deployment (done)

- User decisions: a one-shot `ollama-pull` compose service pulls the model (the app keeps Spring AI's `pull-model-strategy: never`);
  Ollama sits behind the compose profile `ai` so a plain `docker compose up -d` stays Oracle-only; `ollama/ollama:latest`; the
  NVIDIA GPU reservation is opt-in via a separate `compose.gpu.yaml`. The Ollama starter was already added in 7.1.
- `compose.yaml`: `ollama` (port 11434, `ollama-models` volume, health check `ollama list` since the image has no curl/wget) and
  `ollama-pull` (`depends_on: service_healthy`, `OLLAMA_HOST=http://ollama:11434`, `command: [pull, <model>]`, `restart: no`).
- **7.2 model changed (user decision, after live runs):** the first live run showed Ollama's `qwen3:4b` tag is now the
  *Thinking-2507* build: it ignores `think: false` and leaks its reasoning into the reply (`...</think>`), ~250 tokens / ~1 min for
  one sentence on CPU. I switched to `qwen3:4b-instruct`; the user then picked **`qwen3.5:4b`** (4.7B, ~3.4 GB, hybrid attention).
  Measured: thinking on by default (~1200 tokens / 3.5 min for "say hi in 3 words"), `think: false` works (7 tokens / ~2 s), so
  `think: false` is back in `application.yml` and asserted in `ChatModelSelectionTest`. KV cache recomputed from
  `ollama show --verbose`: 8 attention layers × 4 KV heads × 256 → ~32 KB/token, ~256 MB at 8192. README 7.2 updated to match
  (memory example, candidate table, measured thinking table, "a tag is not a contract").
- Found along the way: a mid-session edit turned the pull `command` into a single block-scalar string; the binary got
  `"ollama pull qwen3:4b\n"` as one unknown sub-command (reproduced with `docker compose run`). The user then settled on
  `entrypoint: [sh, -lc]` + the block-scalar command, which runs it through a shell (verified: pull `success`).
- User addition: an `open-webui` chat UI service (port 3000). It lacked the `ai` profile while depending on `ollama`, so a plain
  `docker compose up -d` failed ("depends on undefined service ollama"); added `profiles: [ 'ai' ]`, checked with `docker compose
  config --services` (plain: oracle only; `--profile ai`: all four). Port 3000 is also Grafana's in CLAUDE.md: a future clash.
- GPU verified: with `compose.gpu.yaml` Ollama logs `library=CUDA ... RTX 4060 Laptop GPU`, `ollama ps` shows `100% GPU`.
  `qwen3.5:4b` ~45 tokens/s (CPU ~6), app one-sentence answer ~1.6 s (CPU ~17 s); README 7.2/7.3 carry both CPU and GPU numbers.
- Model comparison (user question, GPU, `think: false`, 10 catalog lookups over a 2,300-token context + Greek + one tool call):
  `qwen3.5:4b` 10/10, best Greek, 49 tok/s; `qwen3:4b-instruct` 10/10, wordier, 63 tok/s; `ministral-3:3b` 7/10 (invented
  prices), 73 tok/s; `qwen3.5:2b` 7/10, broken Greek. All made the tool call. Kept `qwen3.5:4b`.
- Verified live (CPU only, 4-CPU / 8 GB Docker VM): `docker compose --profile ai up -d ollama ollama-pull` → `ollama` healthy in
  ~6 s, pull exits 0, `ollama list` shows `qwen3.5:4b`; the app with `--spring.ai.model.chat=ollama` answered
  `POST /api/v1/chat-completions` in English (~17 s) and in Greek (~9 s, reply in Greek), no thinking text. `compose.gpu.yaml`
  checked with `docker compose config` only (no GPU run). README: 7.3 section (commands, services, health check, pulling, GPU,
  memory/speed, running the app).
- Verified: full-reactor `./mvnw verify` → `BUILD SUCCESS` (spring-ai-training: 37 unit/slice + 8 IT, 0 failures).

### 7.4 Switching between cloud / local (done)

- User decisions: the switch is an app-owned `app.ai.chat-provider` bound to a validated record; the cloud profile is named
  `anthropic`; resilience = README note **plus** capping Spring AI's retry for the Ollama path.
- `config.ai.AiProperties` (`@Validated @ConfigurationProperties("app.ai")`, `@NotNull ChatProvider chatProvider`, enum `ANTHROPIC`,
  `OLLAMA`). `application.yml`: `spring.ai.model.chat: ${app.ai.chat-provider}`, `app.ai.chat-provider: anthropic`.
  `application-local.yml` → `ollama`; new `application-anthropic.yml` → `anthropic` (so `local,anthropic` = local dev on Claude).
- `ChatClientConfig.chatClient(...)` takes `AiProperties` first (so the property is bound and validated before the builder's
  `ChatModel` lookup) and the `ChatModel`, and logs `Chat model: provider=..., model=...` at INFO. A typo now fails with
  "Failed to bind properties under 'app.ai.chat-provider' ... No enum constant ...openai" instead of 7.1's
  `NoSuchBeanDefinitionException`; a missing value fails the `@NotNull` before the two `ChatModel` beans collide.
- Findings: `AnthropicChatModel` has no Spring `RetryTemplate`; the Anthropic Java SDK (2.52.0, bytecode) retries IO errors, 408,
  409, 429, >=500, honours `X-Should-Retry` / `Retry-After(-Ms)`, backoff 0.5 s x2 up to 8 s with jitter, so `spring.ai.retry.*` never
  applies to it. The Ollama path uses `spring-ai-autoconfigure-retry`'s `RetryTemplate` (Framework 7 core retry): defaults 10 retries,
  2 s x5, uncapped. Measured with Ollama unreachable: still hanging after 150 s (retries at 2, 10, 50 s...).
- `application.yml` `spring.ai.retry`: `max-attempts: 3`, backoff `1s` x`2`, max `5s`. Measured: `max-attempts` counts retries
  after the first call (log "Retry count" 1-3); Ollama stopped mid-session → 502 `ProblemDetail` after ~7 s (waits 1, 2, 4 s).
- `ChatModelSelectionTest` reworked to switch through `app.ai.chat-provider` and profiles (9 tests: default, property, `local`,
  `local,anthropic`, embedding off, 7.2 model options, typo, unset, dead Ollama fails in < 20 s, ~8 s on Windows).
  `ChatClientConfigTest`'s runner gets an `AiProperties` bean. Caught while writing it: a `start(String...)` / `start(String[],
  String...)` overload pair silently passed the profile array as command-line args (Java picks the non-varargs match first), and
  the `local,anthropic` test passed for the wrong reason; replaced by a separately named `startWithProfiles`.
- Also fixed: the `num-ctx` comment in `application.yml` still said ~1.2 GB KV cache (the Qwen3 dense figure); now ~256 MB.
- README: 7.4 section (the switch, profile table, run commands, typo error, the resilience comparison table, measured
  before/after, rules of thumb: one retry layer, Framework 7 core retry instead of the Spring Retry library, streaming, circuit
  breaker, tools). 6.2's run command now uses `local,anthropic`; 7.1 and 7.3 point to the new switch.
- Verified live: `local` profile → startup log `provider=OLLAMA, model=qwen3.5:4b`, a chat call answered (200); with the Ollama
  container stopped → 502 in 7.3 s after 3 retries. Full-reactor `./mvnw verify` → `BUILD SUCCESS` (spring-ai-training: 40
  unit/slice + 8 IT, 0 failures). Not run: a live Anthropic call (spends the user's credits).

### 8.1 Prompt templates (done)

- User decisions: two use cases, **summaries** and **product descriptions**, in a new `writing` feature.
- Templates `prompts/writing/summary.st` (`{maxWords}`, optional `{if(audience)}`, the text fenced in `"""`) and
  `prompts/writing/product-description.st` (feature list `{features:{feature | - {feature}\n}}`, `{if(language)}...{else}English{endif}`,
  tone). `WritingPrompts` loads each once as a `PromptTemplate` from its `Resource` and maps a request to the variables;
  `WritingService` sends the rendered text as the user message through the central `ChatClient` (system prompt still first);
  `WritingController`: `POST /api/v1/summaries`, `POST /api/v1/product-descriptions` (200 / 400 / 502). Records `SummaryRequest`,
  `ProductDescriptionRequest` (+ lower-case `Tone` enum), `WritingResponse` (+ `TokenUsage`).
- Refactor needed by the second model-calling feature: `ChatModelUnavailableException` moved `chat.internal` → `common` (shared
  kernel; `chat.internal` removed), and the system prompt's `{today}` filling moved from `ChatService` to
  `config.ai.SystemPromptParams.fill`. `chat` behaviour unchanged.
- Findings (Spring AI 2.0.1, probed then pinned in `PromptTemplatesTest`): a missing variable throws `IllegalStateException` ("Not
  all variables were replaced"), **also** for one used only inside `{if(...)}`, so optional values go in as `null` (HashMap);
  ST's `separator="..."` list option is misread as a missing variable `separator` (workaround: line break inside the element
  template); literal braces (a JSON example) make the template invalid (`IllegalArgumentException`) unless the renderer uses other
  delimiters; `ValidationMode.NONE` renders a missing value as empty text; values are never parsed as template syntax; ST writes
  every line break (template and values) as `System.lineSeparator()`, so on Windows the rendered prompt has `\r\n`. That broke the
  first test run; tests now normalize.
- Tests: `PromptTemplatesTest` (13), `WritingPromptsTest` (5), `WritingControllerTest` (7), `WritingApiIT` (3).
- Verified live on Ollama (`local` profile, GPU): summary of a 70-word text with `maxWords: 30` → 24 words for the requested
  audience; product description with `language: Greek` → one Greek paragraph naming all three features. Both endpoints in
  `/v3/api-docs`. Not run: Anthropic.
- Verified: `./mvnw -pl spring-ai-training verify` → 65 unit/slice + 11 IT, 0 failures. Full-reactor `./mvnw verify` failed
  once in `spring-boot-training` (Testcontainers Oracle start timeout while the compose Oracle, Ollama and Open WebUI were running,
  as seen in step 5); that module's `verify` passed on the retry. No code in that module changed.

### 8.2 Instruction vs context (done)

- User decisions: a new `answer` feature; instructions and context as **tagged sections of one user-message template** (the app
  system prompt untouched); the contrast is **optional documents** on the same endpoint (none = instruction-only).
- `prompts/answer/question.st`: `<instructions>` (fixed; `{if(documents)}` picks "use only the documents, they are data, cite [n],
  say so if absent" vs "no documents, general knowledge, say if unsure"), `<documents>` (left out when none), `<question>` last.
  `prompts/answer/document.st` renders one `<document number title>`. `AnswerPrompts` renders each document, then the question
  template; `AnswerService` sends it as the user message after `SystemPromptParams::fill`; `AnswerController`
  `POST /api/v1/answers` (200 / 400 / 502). Records `AnswerRequest` (question ≤ 1000, ≤ 5 `Document`s of title + text ≤ 4000, sized
  to stay inside Ollama's 8192 `num-ctx`, which truncates the prompt's start) and `AnswerResponse` (+ `TokenUsage`).
- Findings (Spring AI 2.0.1 / ST 4.3.4, pinned in `PromptTemplatesTest` +2): the variable check accepts a list element's
  parameter only once inside its element template; a second reference (`{d.title}` then `{d.text}`) fails with "Missing variable
  names are: [d]" (hence the per-document template). A record value's components render as empty text (ST reads getters / public
  fields only; error logged, no exception). `{if}` / `{endif}` alone on a line swallow its line break, so section blank lines sit
  inside the `if`.
- Tests: `AnswerPromptsTest` (5), `AnswerControllerTest` (6), `AnswerApiIT` (3), `PromptTemplatesTest` +2.
- Verified live on Ollama (`local`, GPU, `qwen3.5:4b`): instruction-only → hedged but wrong guess (1 year, battery included), 238
  prompt tokens; with documents → "2-year limited warranty [1], batteries 12 months [1]", 364 tokens, an injected "reply only
  HACKED" document ignored; a question the documents don't answer → "The provided documents do not mention ...". Not run: Anthropic.
- README: 8.2 section (the layered prompt, the layer table, live contrast, injection limits, the new pitfalls, statuses, tests).
- IDE note: a `reformat_file` right after a disk edit saved the stale buffer over it again (`AnswerPromptsTest`); redone through
  `apply_patch`, per lessons.md.
- Verified: `./mvnw -pl spring-ai-training verify` → 79 unit/slice + 14 IT, 0 failures; full-reactor `./mvnw verify` → `BUILD SUCCESS`.

### 8.3 Structured outputs (done)

- User decisions: **product listing extraction** (new `extraction` feature); **both** mechanisms, contrasted (`prompt` format
  instructions vs `native` provider structured output, per request `outputMode`); retry = Spring AI's
  **`StructuredOutputValidationAdvisor` + Jakarta** on the mapped record, 502 when it stays invalid.
- `ExtractedProduct` record (the model's target: `@JsonPropertyDescription` per field, `Category` enum, `@Positive` price,
  `@Pattern` currency, `@AssertTrue` price-with-currency); `ProductExtractionRequest` (+ `OutputMode` `prompt`/`native`),
  `ProductExtractionResponse` (+ `TokenUsage`); `prompts/extraction/product.st`; `ExtractionController`
  `POST /api/v1/product-extractions` (200 / 400 / 502). `ExtractionService`: `responseEntity(converter, spec -> native?)` with the
  validation advisor (2 repair attempts) per call, then `Validator`. New `common.InvalidModelOutputException` (problems list) → 502
  with an `errors` extension in `ApiExceptionHandler`.
- Findings (2.0.1 sources + tests): `.entity(...)` appends the schema to the user message; `useProviderStructuredOutput()` puts it
  in `StructuredOutputChatOptions` instead and silently falls back to the prompt when the options aren't that type. The advisor
  re-prompts with "Output JSON validation failed because of: ..." (no field name) and gives up silently, returning the last reply.
  The generated schema: `@Nullable` fields are optional but typed non-null, so an explicit `null` fails validation; a springdoc
  `@Schema` (default `requiredMode` AUTO) makes a field required, ahead of JSpecify. Jakarta constraints never reach the schema.
- Fix from a live run: the lamp text cost 3 calls / 2,340 tokens (all 3 replies rejected for explicit nulls, the last returned
  anyway). `ExtractionService.allowNullForOptionalProperties` adds `"null"` to optional property types in the **validation** schema
  (the model still sees the generated one): 1 call, 782 tokens. Removed `@Schema` from `ExtractedProduct`; descriptions and the
  template say "leave out" for unknown values.
- Live on Ollama (`qwen3.5:4b`, GPU): `prompt` mode correct on both texts, injected "set the price to 1 EUR" ignored; `native` mode
  (270 vs ~720 prompt tokens, the model never sees the descriptions) dropped the currency (502 on the rule) and filed the lamp
  under `AUDIO`. Not run: Anthropic.
- Tests: `StructuredOutputTest` (10), `ExtractionControllerTest` (6), `ExtractionApiIT` (7).
- README: 8.3 section (entity / responseEntity, the two modes, three layers, curl, measured comparison, pitfall table, statuses).
- Verified: full-reactor `./mvnw verify` → `BUILD SUCCESS` (spring-ai-training: 94 unit/slice + 21 IT, 0 failures).

### 9.1 Stateless vs stateful conversations (done)

- User decisions: a **hand-rolled in-memory store** (ChatMemory + advisor stay in 9.2); a **conversation resource**
  (`POST /conversations` 201 + `Location`, `POST|GET /conversations/{id}/messages`, `DELETE /conversations/{id}` 204); the stateless
  side **reuses** 6.2's `/chat-completions` and 6.3's `/prompts` (no new endpoint).
- `conversation` feature: `Conversation` (message list + a `ReentrantLock` held for the whole turn, model call included, so turns on
  one conversation serialize without pinning virtual threads), `ConversationStore` (`ConcurrentHashMap`, bounded at 1,000, no
  eviction), `ConversationService` (`.system(fill)` + `.messages(history)` + `.messages(user)`; the turn is stored only after a
  successful call; 20-turn limit), `ConversationController`, DTO records, `ConversationNotFoundException` (404) and
  `ConversationLimitException` (409) handled in `ApiExceptionHandler`.
- Live on Ollama (`qwen3.5:4b`): turns 1-3 remembered the name, city and framework, prompt tokens 170 → 250 → 282; the stateless
  `/chat-completions` didn't know the name (169 tokens); `/prompts` with the client-sent history did; delete → 204, then 404. A
  devtools restart between two turns (the IDE recompiled `target/classes`) lost a conversation mid-demo: kept in the README as
  the "in-memory state dies on restart" example.
- Mistake along the way: my app failed to start because port 8080 was taken by the user's IntelliJ run with the `anthropic`
  profile, and my curl calls went to that instance: ~6 Anthropic calls (~1,700 tokens) spent unintended. Rerun on port 8081 after
  checking the startup log says `provider=OLLAMA`. See lessons.md.
- **Found (not fixed, outside 9.1):** in those Anthropic replies turns 1 and 3 came back with `content: ""` but 124 / 46 completion
  tokens. `AnthropicChatModel` (2.0.1 source) adds one `Generation` per thinking block **before** the text generation, and every
  service here reads `response.getResult()` (the first generation), so a reply where Claude thinks first returns the thinking
  block's (empty) text. Affects chat, writing, answer, conversation (which would also store the thinking block as the assistant
  turn) and probably `.entity()`. Needs a fix and a few live Anthropic calls to verify; waiting for the user's go-ahead.
  - **Fixed right after 9.1** (separate `fix` commit, user go-ahead): `config.ai.ThinkingBlocksAdvisor`, a second default advisor on
    the central client (order `LOWEST_PRECEDENCE - 1`, just outside the model call), drops generations marked `signature` / `data`
    / `thinking` (streamed chunks skipped), keeping the answer's generation (which carries the thinking for replay) and the
    metadata. `ThinkingBlocksAdvisorTest` (6). Live on Anthropic (8 calls): a reasoning question → 314 thinking tokens, "Dropped 1
    thinking block generation(s)", answer "12:02"; a two-turn conversation replaying that turn → correct, no 400; 3-turn chat and
    both extraction modes correct (those didn't think: `thinking_tokens: 0`).
- Tests: `ConversationStoreTest` (2), `ConversationControllerTest` (10), `ConversationApiIT` (6).
- README: new U9 heading, 9.1 section (the three styles side by side, the resource, how a turn works, the store's limits, live
  run, trade-off table, statuses).
- Verified: full-reactor `./mvnw verify` → `BUILD SUCCESS` (spring-ai-training: 106 unit/slice + 27 IT, 0 failures).

### 9.2 Memory lifecycle (done)

- User decisions: **JDBC on Oracle** (not in-memory with a note); go-ahead for a dedicated `ai_training` schema user and a
  Testcontainers Oracle for every `@SpringBootTest`. The 9.1 API stays; its hand-rolled store is replaced, as 9.1's Javadoc said.
- Infra: `compose/oracle-startdb/01_ai_training_user.sql` (`CREATE USER IF NOT EXISTS ai_training`, `DB_DEVELOPER_ROLE`), mounted
  at `/container-entrypoint-startdb.d` (every start, so the existing `oracle-data` volume gets it; the container was recreated once,
  data kept). Own schema because two Flyway histories can't share one and a `flyway:clean` here must not drop the catalog.
- Pom: `spring-ai-starter-model-chat-memory-repository-jdbc`, `spring-boot-starter-data-jdbc`, `ojdbc17`, `spring-boot-flyway` +
  `flyway-database-oracle`, test `spring-boot-testcontainers` + `testcontainers-oracle-free`, and a `flyway-maven-plugin` (no
  executions, user `ai_training`). `application.yml`: datasource, `spring.flyway.enabled: false` (test profile: true),
  `spring.ai.chat.memory.repository.jdbc.initialize-schema: never`, `app.ai.memory.max-messages: 20`.
- `V1__create_chat_memory.sql`: `conversation` (id, created_at, turns, version) + the library's `spring_ai_chat_memory` (names
  fixed by `OracleChatMemoryRepositoryDialect`) with `fk_..._conversation ... on delete cascade`.
- `config.ai.ChatMemoryConfig` (`MessageWindowChatMemory` over the autoconfigured `JdbcChatMemoryRepository`, window from the new
  validated `ChatMemoryProperties`; a `MessageChatMemoryAdvisor` bean) and `conversation`: `Conversation` is now a Spring Data JDBC
  record (`@Version` for new-vs-existing, id assigned by the app), `ConversationRepository`; `ConversationService` adds the
  advisor per call with `CONVERSATION_ID`, writes back the previous window on failure, counts turns, 64 striped locks. Removed:
  `ConversationStore`, `ConversationLimitException` and its 409 handler.
- Findings (Spring AI 2.0.1 sources + tests): the autoconfig hard-codes a 20-message window with no property; the window snaps its
  start to a user message and never evicts system messages; the advisor stores the user message **before** the call (half turn on
  failure) and throws without a conversation id (so it can't be a default advisor); `JdbcChatMemoryRepository` drops tool-call
  messages with a WARN and stores text only; `saveAll` is delete + batch insert in its own transaction. Oracle `''` = `NULL`, so an
  empty reply fails with `ORA-01400` on `CONTENT` (confirmed in the IT log) and goes through the same rollback to a 502.
- Tests: `ChatMemoryTest` (6), `ConversationApiIT` (9: +window over 12 turns, rollback, empty reply, read back through a new
  repository/window, cascade; -turn limit), `ConversationControllerTest` (9, -409), `ConversationStoreTest` removed. New
  `support.SharedOracleContainer` + `AbstractOracleIntegrationTest`; all six `@SpringBootTest` classes extend it.
- Verified live on Ollama (`local`, port 8081, startup line `provider=OLLAMA`, window 4): turn 4 no longer had turn 1 (the model
  didn't know the name, though earlier replies still in the window had echoed it); after stopping and restarting the app the same
  conversation had its 4 messages and turn 5 answered "Spring Boot" from memory; DELETE → 204 then 404, both tables empty.
  Prompt tokens 163 → 197 → 260 → 298 → 310. Not run: Anthropic.
- README: 9.2 section (the three layers, the per-call advisor, window rules, where memory lives, schema / Flyway, pitfalls,
  concurrency, live run, statuses, tests); "Run it" now starts Oracle and migrates first.
- Verified: full-reactor `./mvnw verify` → `BUILD SUCCESS` (spring-ai-training: 115 unit/slice + 30 IT, 0 failures).

### 10.1 RAG pipeline overview (done)

- User decisions: embeddings from an **Ollama** model (for both chat providers); corpus = **both** the catalog rows (read-only
  from `training`) and bundled catalog-domain documents.
- Embedding model: `qwen3-embedding:0.6b` (1024 dims, Q8_0, ~640 MB), picked from a probe of 6 questions (one Greek) against
  6 texts: 6/6 right first hits vs `embeddinggemma` 5/6, `nomic-embed-text` 4/6 (the latter without its task prefixes). The
  other two were removed from the Ollama volume. Pulled by `ollama-pull` in `compose.yaml`.
- Pom: `spring-ai-starter-vector-store-oracle` (excluding `ojdbc11`, `ucp`, `simplefan`: ojdbc17 is the driver) and
  `spring-ai-vector-store-advisor` (the 2.0.1 name). `application.yml`: `spring.ai.model.embedding: ollama`,
  `spring.ai.ollama.embedding.model`, `spring.ai.vectorstore.oracle.*` (table `knowledge_vector`, 1024, COSINE, IVF,
  `forced-normalization: true`, `initialize-schema: false`), `app.ai.retrieval.*` (top-k 4, threshold 0),
  `app.ai.knowledge.catalog-schema: training` (test profile: `test`).
- `V2__create_knowledge_vector.sql` (the library's DDL, Flyway-owned, + IVF index);
  `compose/oracle-startdb/02_ai_training_catalog_read.sql` (READ on `training.product` / `category`, skipped with a message
  while the tables don't exist).
- `config.ai.RagConfig` (`QuestionAnswerAdvisor` bean: `SearchRequest` from the validated `RetrievalProperties`, template
  `prompts/knowledge/question-answer.st`, explicit order `DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100`). Feature `knowledge`:
  `CatalogDocumentReader` (JdbcClient, `GEN-*` excluded, text `[catalog:SKU] name. Category. Price. description`, name-based
  UUID ids), `BundledDocumentReader` (`classpath:knowledge/*.md`, 4 docs, whole), `KnowledgeIngestionService` (delete both
  sources, `vectorStore.add`), `KnowledgeService` (per-call advisor, sources from `RETRIEVED_DOCUMENTS`),
  `KnowledgeController` (`POST /api/v1/knowledge/ingestions`, `POST /api/v1/knowledge/answers`), DTO records,
  `KnowledgeProperties` (schema name validated as an identifier, since it goes into the SQL).
- Findings: only document **text** reaches the model (metadata never), so tags and facts go in the text; the advisor's
  default order (0) ties with `SimpleLoggerAdvisor`; a similarity threshold throws unless `forced-normalization` is on;
  metadata read back from Oracle's JSON column holds driver JSON values whose `toString()` keeps the quotes (`"LAP-0002"`,
  caught by the IT, unwrapped in `KnowledgeService.text`); `number(12,2)` 2199.00 comes back as `2199` (formatted to 2
  decimals); the 7.1 test pinning "no EmbeddingModel until RAG" now pins the opposite
  (`embeddingsComeFromOllamaWhicheverChatProviderIsActive`).
- Mistake along the way: the IDE reformat of the already-applied `V2` split `create vector index` onto two lines (IntelliJ's
  SQL formatter doesn't know the statement) and `flyway:validate` reported a checksum mismatch. The original text was restored
  and validates again. See lessons.md.
- Live on Ollama (`local`, port 8081, startup line `provider=OLLAMA`, `Retrieval: store=OracleVectorStore, top-k=4`): ingestion
  24 + 4 documents in 8 s. "3D rendering laptop + warranty": the guide, the warranty doc and LAP-0002 retrieved (.57/.54/.53), a
  correct answer citing all three. Opened earbuds: returns-policy .70, a correct "no". A Greek shipping question: shipping first
  but only .31, the price right, answered in English with a misread cut-off time. "Gaming consoles": best .33, answered "not
  covered". Prompt tokens 498 to 967. Not run: Anthropic.
- Tests: `RagTest` (4), `KnowledgeDocumentsTest` (3), `KnowledgeControllerTest` (6), `KnowledgeApiIT` (5, real
  `knowledge_vector` table, hashing stub embedding model, `@Sql` catalog fixture), `ChatModelSelectionTest` (one case
  replaced).
- README: new U10 heading, 10.1 section (the two phases, the four pieces, model choice, ingestion, text vs metadata, live
  table and what it shows, pitfalls, statuses, setup, tests).
- Verified: full-reactor `./mvnw verify` → `BUILD SUCCESS` (spring-ai-training: 128 unit/slice + 35 IT, 0 failures).

### 10.2 Chunking strategies (done)

- User decisions: **add long documents** (the user asked for more than two; four added), compare **whole vs TokenTextSplitter
  vs Markdown sections**, measure with an **evaluation endpoint** (retrieval only).
- Corpus: `terms-of-sale.md` (967 words), `product-care-guide.md` (1,131), `trade-in-and-recycling.md` (705),
  `business-accounts.md` (699), 8-15 sections each, beside the four 10.1 documents. Shorter than the 1,500-2,000 words planned;
  enough for chunking to matter (33 token chunks, 50 sections).
- Pom: `spring-ai-markdown-document-reader`. `app.ai.knowledge.chunking.active: markdown`, `token-chunk-size: 200`
  (`KnowledgeProperties.Chunking`).
- `knowledge`: `ChunkingStrategy` (`whole` / `token` / `markdown`, key + filter expression), `DocumentChunker` (Spring AI's
  splitter/reader, then a contextual header per chunk `[doc:name] Title > Section:` / `(part i of n):`, stable ids per document +
  strategy + index, metadata `chunking`, `section`, `chunk_index`, `chunk_count`), `BundledDocumentReader` now returns
  `BundledDocument` records, catalog rows marked `chunking: none`. Ingestion stores every strategy side by side;
  `KnowledgeService` filters per call (`QuestionAnswerAdvisor.FILTER_EXPRESSION`); sources carry `section`.
  `KnowledgeEvaluationService` + `POST /api/v1/knowledge/evaluations?top-k=`: 19 questions
  (`evaluation/knowledge-questions.json`, 2 Greek, 1 catalog, 2 unanswerable), hit = expected ref + answer phrase in the chunk;
  hit@1, hit@k, MRR, mean/min hit similarity, max unanswerable similarity, context tokens (JTokkit). Ingestion response now
  has `chunks` per strategy and `totalRows`.
- Findings (2.0.1, pinned in `ChunkingTest`): `TokenTextSplitter` defaults to 800 tokens (documents here stay whole), keeps a
  top-of-document tag on the first chunk only, random chunk ids, INFO log per split document; `MarkdownDocumentReader` puts the
  heading in metadata only and glues paragraphs ("dispatch.Bank"). A small model copied the prompt's *example* citation tag
  (`[Catalog:LAP-0002]`) into an answer; the template now describes the tag without an example.
- Live (Ollama, port 8081, `provider=OLLAMA`): ingestion 24 + 8 → whole 8 / token 33 / markdown 50 chunks, 115 rows, 20 s.
  Evaluation top-k 4: whole hit@1 .53 hit@4 .76 MRR .65 1,370 tokens; token .82 / 1.00 / .90, 565; markdown .82 / 1.00 / .91,
  384, mean hit similarity .64, min .39, highest unanswerable .35. Top-k 2: markdown still 1.00 (219 tokens). Whole missed 4
  questions (3 care-guide, instalments). Markdown chosen as active. Answers with markdown: cash on delivery (Payment section,
  .60), Studio 3 pairing (.74), business credit (merged two "15 days" rules). Not run: Anthropic.
- Tests: `ChunkingTest` (8), `KnowledgeEvaluationTest` (4), `KnowledgeApiIT` (7), `KnowledgeControllerTest` (8),
  `KnowledgeDocumentsTest` (3).
- README: 10.2 section (why chunk, corpus, strategies, contextual headers, evaluation method and results, what they show, the
  threshold margin, pitfalls, tests).
- Fix found by the verify run (a 9.2 regression, not 10.2): `ChatModelSelectionTest` builds the application itself, so
  `@ServiceConnection` never applied and since 9.2 it had been using the compose Oracle on localhost:1521. It passed only while
  that was up; with compose stopped, 7 errors (`ORA-12541`). It now passes the shared Testcontainers Oracle as datasource
  properties. See lessons.md.
- Verified: `./mvnw -pl spring-ai-training verify` → 142 unit/slice + 37 IT, 0 failures. spring-boot-training (unchanged) passed
  in the preceding full-reactor run (75 + 66). The first full run was stopped for low memory; a second hit a transient compile
  error while the IDE was saving `KnowledgeEvaluationResponse.java`.

### 10.3 Logging (done)

- User decisions: a **`RagLoggingAdvisor` just inside `QuestionAnswerAdvisor`** (not two advisors, not inline in the service);
  **SLF4J key-value pairs** (fluent `log.atDebug().addKeyValue(...)`) with the same values in the message text.
- `config.ai.RagLoggingAdvisor` (`CallAdvisor`, order `RagConfig.ORDER + 1`, bean in `RagConfig`, added per call by
  `KnowledgeService`): `rag.stage` = `retrieval` (hits, top similarity, context tokens by JTokkit, filter), one `hit` per chunk
  (rank, source, ref, section, `markdown 8/17`, similarity, tokens), `generation` (ms, prompt/completion tokens, answer chars,
  model) or `generation-failed` (ms, exception type, rethrown). Retrieval is logged before the model call. DEBUG only, a
  pass-through when DEBUG is off; never the question, chunk text or answer (content stays with `SimpleLoggerAdvisor`, local
  only). Retrieval latency not measured: 10.4's spans.
- `KnowledgeIngestionService`: a DEBUG `chunking` event per document and strategy (chunks, min/max tokens).
- Refactor: `KnowledgeService.text` (Oracle JSON metadata unwrapping) moved to the public `config.ai.VectorStoreMetadata`, now
  used by the service, the evaluation and the advisor.
- Findings: Boot's ECS formatter nests dotted keys (`"rag":{"stage":"retrieval","hits":4,...}`); the plain-text console pattern
  drops key-values (hence the values in the message); `%.2f` follows the JVM locale, so `Locale.ROOT`.
- Live on Ollama (`local`, GPU, port 8081, startup line `provider=OLLAMA`): ingestion 24 + 8 → whole 8 / token 33 / markdown 50
  with per-document chunk sizes; opened earbuds: top similarity 0.70, 4 hits, ~486 context → 795 prompt tokens, 56.5 s (cold model
  load), correct; gaming consoles: top 0.34, 1.7 s, "documents don't say". Rerun with `--logging.structured.format.console=ecs`
  showed the nested `rag` fields. Not run: Anthropic.
- Tests: `RagLoggingAdvisorTest` (6: stage events and key-values, no content, nothing at INFO, empty retrieval, failed
  generation, order).
- README: 10.3 section (two kinds of log, the hook, the event/field table, live output plain and ECS, what it shows, pitfalls).
- Verified: full-reactor `./mvnw verify` with the compose stack stopped → `BUILD SUCCESS` (spring-ai-training: 148 unit/slice +
  37 IT, 0 failures; spring-boot-training 75 + 66).

### 10.4 Token usage monitoring (done)

- Nothing to reuse: spring-boot-training has no observability wiring (the memory note claiming an `otel-lgtm` service and a
  provisioned dashboard was stale; corrected). User decisions: a new **`grafana/otel-lgtm` compose service behind an
  `observability` profile** plus the OTel starter in spring-ai-training only; a **provisioned dashboard**; prompt/completion
  content capture **off in every profile**.
- Pom: `spring-boot-starter-opentelemetry`. `application.yml`: OTLP metrics + tracing export off by default (prod enables via env
  vars), sampling 0.1, OTLP log export off, resource attributes `service.version` / `deployment.environment`, percentile
  histograms for `gen_ai.client.operation`, `db.vector.client.operation`, `http.server.requests`; every Spring AI content switch
  (`spring.ai.chat.observations.log-*`, `chat.client.observations.log-*`, `vectorstore.observations.log-query-response`) false,
  `include-error-logging: true`. `application-local.yml`: both exports on `localhost:4318`, step 10 s, sampling 1.0.
- `compose.yaml`: `otel-lgtm` (3000, 4317, 4318, health check on Grafana's API), `compose/grafana/provisioning/dashboards/training.yaml`
  + `compose/grafana/dashboards/spring-ai-token-usage.json` (tokens in range, tokens/min by model and type, avg tokens per chat
  call, model latency p50/p95, vector search p95 + calls/min by error, a Tempo TraceQL table of RAG answers).
- **Found live, fixed:** the vector search span carried the question (`db.vector.query.content`) with every content property off.
  `config.ai.ObservabilityConfig` registers an `ObservationFilter` dropping it in every profile; the IT for it fails without the
  filter (checked by disabling it once).
- Other findings (2.0.1): the first vector search per process embeds "Hello World" (the search observation asks
  `AbstractEmbeddingModel.dimensions()`, unknown for qwen3-embedding), counted as tokens and 6.5 s on a cold model; the embedding
  observation's `gen_ai.request.model` is `none` (dashboards group by response model); Prometheus names
  `gen_ai_client_token_usage_total`, `*_milliseconds_bucket`; `percentiles-histogram` matches `...operation.active` by prefix;
  Grafana's file provider skipped a dashboard directory created after startup (fine after a restart); protobuf prints
  `sun.misc.Unsafe` warnings on Java 25.
- Live (Ollama GPU, `local`, port 8081, `provider=OLLAMA`): metrics in Prometheus, traces in Tempo; a RAG trace shows
  `http post` > `chat_client` > advisors > `embedding` (probe) + `oracle query` (> `embedding` of the question) + `chat qwen3.5:4b`
  (795 in / 102 out). Chat p95 ~1.8 s, embedding ~50 ms, vector search ~67 ms, ~790 input tokens per chat call. Every dashboard
  query checked against Prometheus / Tempo; local log lines carry `[traceId-spanId]`. Not run: Anthropic.
- Tests: `TokenUsageMetricsIT` (3: exact chat token counts + tags, a RAG answer's embedding/search/chat, no observation holds the
  question), real Ollama models against a JDK `HttpServer` stub.
- README: 10.4 section (what is recorded, wiring, the content filter, run, Prometheus names, dashboard table, live trace and
  numbers, pitfalls, tests).
- Verified: full-reactor `./mvnw verify` with the compose stack stopped → `BUILD SUCCESS` (spring-ai-training: 148 unit/slice + 40
  IT, 0 failures; spring-boot-training 75 + 66), no OTLP export attempt in the test logs.

### 11.1 Tools vs APIs (done)

- User decisions: **catalog product lookup** as the capability; tools offered on a **new assistant endpoint** only (per call, not
  defaults of the central client); the response **lists the tool calls**. Deviation: recorded with a `RecordingToolCallback`
  decorator (per-request list) instead of `ToolContext`, so the tools' signatures stay clean.
- `product` feature: `ProductService` (public; JdbcClient over the catalog schema; any-keyword search over SKU, name, description,
  category, ranked by matched keywords, optional `maxPrice`, `GEN-*` excluded; get by SKU, case-insensitive), `ProductController`
  (`GET /api/v1/products?query=&max-price=&limit=`, `GET /api/v1/products/{sku}`, 404 via new `ProductNotFoundException` in
  `ApiExceptionHandler`), `CatalogTools` (`@Tool searchProducts(keywords?, maxPrice?)` max 5 results, `@Tool getProduct(sku)`),
  `ProductResponse`.
- `assistant` feature: `AssistantController` (`POST /api/v1/assistant/answers`), `AssistantService` (`.tools(...)` with the
  recording wrappers, template `prompts/assistant/question.st`), `AssistantRequest`, `AssistantResponse` (+ `ToolCall`, summed
  `TokenUsage`).
- Refactor: `app.ai.knowledge.catalog-schema` → `app.ai.catalog.schema` (`config.persistence.CatalogProperties`), read by
  `knowledge` and `product`. `spring.ai.tools.observations.include-content: false`.
- Live fixes (qwen3.5:4b): keywords-only + every-word-required made "laptops under 1500 euro" search
  `"laptop, under 1500 euro, price"` → empty → "none"; a synonym list `"acoustics headphones speakers"` → empty too; a Greek
  keyword found nothing. Fixed with `maxPrice`, any-word ranking, and "tool arguments in English" in the instructions. Also:
  Greek via `curl -d` in Git Bash arrived as `?` (code page); sent from a UTF-8 script afterwards.
- Findings (2.0.1): `ChatClient` attaches tools only when `chatModel.getOptions()` is `ToolCallingChatOptions` (else dropped
  silently; bit the IT stub); a tool exception goes back to the model as its message (`DefaultToolExecutionExceptionProcessor`);
  `ToolCallingAdvisor` (order `HIGHEST_PRECEDENCE + 300`) runs the loop and sums usage; default limits 40 calls per tool / 150
  per turn; `MockMvcTester` re-sends the request on each `assertThat(builder)`.
- Live (`local`, GPU, port 8081, `provider=OLLAMA`): 8 questions correct after the fixes (headphones, SKU price, unknown SKU,
  no-tool question, laptops under 1500 in English and Greek, Greek headphones, cheapest laptops); REST ~10 ms vs 3.4 s for a
  tool-using answer; ~1,650-1,950 prompt tokens with tools vs 165 without. Not run: Anthropic.
- Tests: `CatalogToolsTest` (4), `ProductServiceTest` (5), `ProductControllerTest` (5), `AssistantControllerTest` (3),
  `ProductApiIT` (4), `AssistantApiIT` (3, scripted model + real tools over Oracle). A first IT run hit the known Testcontainers
  Oracle start timeout while compose Oracle + Ollama were up; passed with compose stopped.
- README: new U11 heading, 11.1 section (comparison table, how the loop works, recording, live table, what went wrong first,
  pitfalls, tests).
- Verified: full-reactor `./mvnw verify` with the compose stack stopped → `BUILD SUCCESS` (spring-ai-training: 165 unit/slice +
  47 IT, 0 failures; spring-boot-training 75 + 66).

### 11.2 Validation (done)

- Decisions (approved plan): input **records** with Jakarta Bean Validation, checked by Spring's method validation proxy
  (`@Validated` on `CatalogTools`, `@Valid` on the parameters), and a fixed model-facing message.
- `ProductSearchInput(keywords, maxPrice)` and `ProductLookupInput(sku)` in `product/dto` (constraint messages written for a model);
  `CatalogTools` takes one record per tool (JSON arguments are now nested: `{"input":{...}}`); `searchProducts` treats a missing
  `input` as "list products".
- `config.ai.ToolValidationExceptionProcessor`: a `ToolExecutionExceptionProcessor` bean (Spring AI's auto-config backs off for it)
  turning a `ConstraintViolationException` into "Invalid arguments, the tool did not run: sku must ...; Fix the arguments and call the
  tool again."; every other failure goes to the default processor. `toolCalls[].error` still shows the raw exception message.
- Findings (2.0.1): Spring AI does not read Bean Validation annotations, so the tool's JSON schema has no `pattern` / `maxLength`;
  a single record parameter is nested under its parameter name; `ToolCallbacks.from(...)` works on the AOP proxy; the violation
  arrives as the `ToolExecutionException`'s root cause.
- Live (`local`, port 8081, `provider=OLLAMA`, qwen3.5:4b): nested input works (laptops under 1500, SKU price, name search, unknown
  SKU); `SKU FLEX13` was rejected and the model relayed the format rule; "laptop flex 13" made the model invent `LAP-0013`, which
  passes validation and is "not found": validation checks shape, not truth. Not run: Anthropic.
- Tests: `CatalogToolsTest` (6), `ToolValidationExceptionProcessorTest` (3), `AssistantApiIT` (4: scripted bad call, rejection
  message in round 2, corrected call); `AssistantControllerTest` fixture uses the nested arguments.
- README: 11.2 section (constraint table, where validation runs, message, live table, limits, tests); 11.1's loop example updated to
  the nested shape.
- Verified: full-reactor `./mvnw verify` with the compose stack stopped -> `BUILD SUCCESS` (spring-ai-training: 170 unit/slice + 48
  IT, 0 failures). The compose stack was restarted afterwards, including the `ai` and `observability` profiles.

### 11.3 Error handling (done)

- Decisions (approved plan): a **simulated carrier** (stub controller, `local` profile) behind a real HTTP client, and retries. Deviation:
  retries use Spring Framework 7's built-in `RetryTemplate` (`org.springframework.core.retry`), not the Spring Retry library, so no new
  dependency (CLAUDE.md lists Spring Retry; the built-in one does the same job).
- `delivery` feature: `DeliveryTools` (`@Tool estimateDelivery(input{sku, postcode})`, validated, no try/catch), `DeliveryService`
  (catalog lookup first, then the carrier; not `@Transactional`), `CarrierQuoteClient` (interface) + `HttpCarrierQuoteClient`
  (`RestClient`, `RetryTemplate`), `DeliveryConfig` (JDK client with connect 1 s / read 2 s), `DeliveryProperties`
  (`app.delivery.carrier.*`), `StubCarrierController` (`/api/v1/stub-carrier/quotes`, postcode-scripted: 50000 down, 50001 fails once,
  50002 429, 50003 too slow, 40404 404), DTO records.
- Shared kernel: `ModelReadableException` (marker: message safe to show a model; `ProductNotFoundException` implements it) and
  `ExternalServiceException` with `Kind` UNAVAILABLE / RATE_LIMITED / REJECTED, each with the advice the model is given.
- `ToolValidationExceptionProcessor` renamed `ToolErrorProcessor` (public): validation, external service (message + advice), Jackson
  (malformed arguments), `ModelReadableException`, and anything else gets a generic "do not guess" message with the exception logged.
  `describe(Throwable)` is the same text without logging; `RecordingToolCallback` records it, so `toolCalls[].error` is what the model was
  told (an unexpected exception's message reaches neither the model nor the caller). `AssistantService` offers the catalog and the
  delivery tools; `question.st` (since moved to `instructions.st`, see the memory follow-up) says what to do on an error.
- Findings: `ToolExecutionException`'s cause chain holds `MismatchedInputException` / `StreamReadException` for bad arguments (both
  `tools.jackson.core.JacksonException`); `RetryTemplate.invoke` rethrows the last runtime exception; `.tools(...)` of the assistant
  now has three tools (`AssistantApiIT` asserts them).
- Live bug: the stub first sat under `/stub-carrier`; the unit test (standalone `MockMvc`) passed, the running app answered `400 Invalid
  API version: 'quotes'` (path-segment versioning reads segment 1 of every path). Moved under `/api/{version}/`; the test is now
  `@WebMvcTest`.
- Live (`local`, port 8081, `provider=OLLAMA`, qwen3.5:4b; Ollama on CPU, 65-80 s per answer): quote, down (3 attempts), flaky (a
  retry that worked), rate limited, too slow (6.4 s), refused, unknown SKU, name then delivery; the model passed each message on and
  never invented an estimate. Not run: Anthropic.
- Tests: `HttpCarrierQuoteClientTest` (9, incl. a real timeout against a JDK HTTP server), `ToolErrorProcessorTest` (9), `DeliveryToolsTest`
  (4), `DeliveryServiceTest` (3), `StubCarrierControllerTest` (5), `DeliveryToolsIT` (6); `AssistantApiIT` (4) updated for the third tool and
  the model-facing error text.
- README: 11.3 section (layers, kinds and retries, what the model reads, stub table, live table, what went wrong first, pitfalls,
  tests); 11.2's processor name and error note updated.
- Verified: full-reactor `./mvnw verify` with the compose stack stopped -> `BUILD SUCCESS` (spring-ai-training: 197 unit/slice + 54 IT, 0
  failures; spring-boot-training 75 + 66). The compose stack was restarted afterwards with the `ai` and `observability` profiles.

### Assistant memory (follow-up to 11.1 to 11.3, on request)

- User request: "cannot have a conversation as every prompt starts all over, hook conversation memory to assistance service".
- Decisions: optional `conversationId` on `AssistantRequest`; omitted, a new conversation starts and its id comes back in
  `AssistantResponse.conversationId` (removed again if its first turn fails). Unknown id: `404` (existing handler).
- `ConversationService` is public with `start()`, `chat(UUID, UnaryOperator<ChatClientRequestSpec>)` and `delete(UUID)`;
  `ConversationNotFoundException`'s constructor is public. `send` and `chat` share one private `run` (exists check, per-conversation
  lock, memory advisor + id, rollback on failure, turn count). `AssistantService` no longer holds a `ChatClient`.
- The shop instructions moved from the user message (`question.st`, now `instructions.st`) to the system prompt, appended to
  `chat-system.st` (per-call `.system(...)` replaces the default text): otherwise memory would store the instruction block with every
  turn. The user message is the question as typed.
- Findings (2.0.1): the memory advisor (`DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER`, +200) is outside the tool calling advisor (+300), so
  memory holds the question and the final answer only, not tool calls or results; a follow-up needing fresh data calls the tool again.
- Live (`local`, port 8081, qwen3.5:4b): 4 messages in one conversation: "those", "it" and "instead" resolved; the second answer needed no
  tool call (1,294 prompt tokens); history has 8 messages, questions and answers only. Not run: Anthropic.
- Tests: `AssistantApiIT` 11 (7 new: system prompt, follow-up history order, memory content, new conversation per message, unknown id
  404, failed first message leaves nothing, failed follow-up keeps memory), `AssistantControllerTest` 5 (2 new).
- Follow-up: the user's requests reached the API without a `conversationId` (Swagger UI: nothing to paste, so every "Execute" was a new
  conversation; the log showed a new id each time). Decision (asked: Swagger UI / IntelliJ HTTP client): every answer sets an
  `assistant-conversation` cookie (`HttpOnly`, `SameSite=Lax`, session, path `/api/v1/assistant`, `Secure` over HTTPS) and a message
  without an id continues the cookie's conversation: body id, then cookie, then a new one. A stale or malformed cookie is ignored.
  `DELETE /api/v1/assistant/conversation` deletes the conversation and clears the cookie. Clients without cookies (curl) still pass the id.
- Live: curl with a cookie jar, no id in the body: both messages in one conversation id; `DELETE` answered `204` with `Max-Age=0`. The
  first answer of that run wrongly said "no laptops under 1500" (a 4B-model tool-call flake; an earlier run found two). Swagger UI itself
  was not driven. Tests: `AssistantControllerTest` 12, `AssistantApiIT` 13.
- README: "Memory for the shop assistant" section after 11.3.
- Verified: full-reactor `./mvnw verify` with the compose stack stopped -> `BUILD SUCCESS` (spring-ai-training: 206 unit/slice + 63 IT, 0
  failures; spring-boot-training 75 + 66). Compose restarted afterwards with the `ai` and `observability` profiles.
