# spring-ai-training

Training module for the Spring AI curriculum (`docs/spring-ai-scope.md`, task plan `.claude/tasks/todo-spring-ai.md`). It
continues the numbering of `spring-boot-training` (units 0-4) at unit 5 and grows one topic at a time; each topic leaves
runnable, tested code plus notes here.

Run it:

```bash
./mvnw -pl spring-ai-training spring-boot:run
```

---

## U6 — Spring AI Core Concepts

### 6.1 Spring AI architecture

**BOM.** The reactor parent imports `org.springframework.ai:spring-ai-bom` (`spring-ai.version`, 2.0.x) in
`<dependencyManagement>`, next to the Spring Boot BOM. Module poms list Spring AI artifacts without a `<version>`; one
property bump moves every Spring AI jar together.

**Starters.** Like Boot's, a Spring AI starter is *client library + auto-configuration module*:

| Starter family                            | Example                                         | Brings                                                         |
|-------------------------------------------|-------------------------------------------------|----------------------------------------------------------------|
| `spring-ai-starter-model-<provider>`      | `-anthropic` (6.2 cloud), `-ollama` (7.x local) | the provider's `ChatModel` / `EmbeddingModel` + its autoconfig |
| `spring-ai-starter-vector-store-<db>`     | `-oracle` (10.x RAG)                            | a `VectorStore` over Oracle 23ai AI Vector Search              |
| `spring-ai-starter-model-chat-memory-*`   | `-repository-jdbc` (9.2)                        | a persistent `ChatMemoryRepository`                            |
| `spring-ai-starter-mcp-{client,server}-*` | `-server-webmvc` (11.4, 12.1)                   | MCP client/server over HTTP                                    |

Every model starter pulls the same two provider-neutral jars. In 6.1 this module declared exactly those two and no provider;
from 6.2 they arrive transitively through `spring-ai-starter-model-anthropic`:

| Artifact                                    | Contents                                                                        |
|---------------------------------------------|---------------------------------------------------------------------------------|
| `spring-ai-client-chat`                     | `ChatClient`, the `Advisor` API, `SimpleLoggerAdvisor`; pulls `spring-ai-model` |
| `spring-ai-autoconfigure-model-chat-client` | `ChatClientAutoConfiguration`: a prototype `ChatClient.Builder` bean            |

**Provider abstraction.** `spring-ai-model` defines the interfaces, and each provider module implements them:

- `ChatModel`: `ChatResponse call(Prompt)`. It also extends `StreamingChatModel` (`Flux<ChatResponse> stream(Prompt)`).
- `EmbeddingModel`: text → vector, used for RAG in U10.
- A `Prompt` is a list of `Message`s (`SystemMessage`, `UserMessage`, `AssistantMessage`, `ToolResponseMessage`; see 6.3)
  plus `ChatOptions` (model name, temperature, max tokens).
- With several providers on the classpath, `spring.ai.model.chat=<provider>` selects the one whose `ChatModel` bean is
  created (7.1 / 7.4). Feature code only ever sees the interface.

**Layering.** Application code talks to `ChatClient`, never to a provider class:

```
feature code (controller / service)
   │  chatClient.prompt().system(..).user(..).call().content()
   ▼
ChatClient            fluent API: builds a ChatClientRequest (Prompt + context map)
   │
   ▼
Advisor chain         CallAdvisor / StreamAdvisor, ordered by getOrder(); each wraps nextCall():
   │                  memory (U9), RAG QuestionAnswerAdvisor (U10), logging, tool calling (U11)
   ▼
ChatModel             provider SPI: AnthropicChatModel, OllamaChatModel, ...
   │
   ▼
provider HTTP API     Anthropic Messages API, local Ollama server
```

An advisor is around-advice: code before `chain.nextCall(request)` can rewrite the request (add memory, retrieved context),
and code after it sees the `ChatClientResponse`.

**Where things live** (per CLAUDE.md):

| What                         | Where                                                                                                                |
|------------------------------|----------------------------------------------------------------------------------------------------------------------|
| central `ChatClient` bean(s) | `config/ai` (6.2); features inject `ChatClient`, never `new` a model client                                          |
| prompt templates             | `src/main/resources/prompts/*.st`, loaded as a `Resource` (6.4, 8.1)                                                 |
| Spring AI config             | `spring.ai.*` in `application*.yml` (`spring.ai.anthropic.*`, later `spring.ai.ollama.*`, `spring.ai.chat.client.*`) |
| API keys                     | environment only (`ANTHROPIC_API_KEY`), never a committed file                                                       |
| tools                        | `@Tool` methods on dedicated `*Tools` components (U11)                                                               |

**Seeing it run without a provider.** `SpringAiArchitectureTest` loads `ChatClientAutoConfiguration` in an
`ApplicationContextRunner` over a stub `ChatModel` lambda and shows:

- a `ChatClient.Builder` is auto-configured on top of whatever `ChatModel` bean exists;
- a call goes `advisor:before → model → advisor:after`, and the model receives a `Prompt` holding one `SystemMessage` and
  one `UserMessage`;
- without a `ChatModel`, the context still starts (the builder is prototype-scoped and has no `@ConditionalOnBean`
  guard), but the first injection of `ChatClient.Builder` fails. That is why the app has no `ChatClient` until a provider
  lands in 6.2;
- `spring.ai.chat.client.enabled=false` removes the builder entirely.

```bash
./mvnw -pl spring-ai-training test -Dtest=SpringAiArchitectureTest
```

### 6.2 ChatClient

**Provider.** `spring-ai-starter-model-anthropic` registers an `AnthropicChatModel`, built on the official Anthropic Java SDK.
The local Ollama runtime and cloud-vs-local switching come later, in U7. Config lives in `application.yml`:

| Property                                          | Value                       | Why                                                                                                              |
|---------------------------------------------------|-----------------------------|------------------------------------------------------------------------------------------------------------------|
| `spring.ai.anthropic.api-key`                     | `${ANTHROPIC_API_KEY}`      | from the environment only; the app starts without it, and a missing key only shows up as a 502 on the first call |
| `spring.ai.anthropic.base-url`                    | `https://api.anthropic.com` | the public Anthropic API                                                                                         |
| `spring.ai.anthropic.timeout`                     | `60s`                       | per-request HTTP timeout                                                                                         |
| `spring.ai.anthropic.max-retries`                 | `2`                         | SDK-level retries on transient failures before the call fails (and this API answers 502)                         |
| `spring.ai.anthropic.chat.model`                  | `claude-sonnet-5`           | the Sonnet model agreed for the curriculum                                                                       |
| `spring.ai.anthropic.chat.max-tokens`             | `4096`                      | Anthropic requires an upper bound on every request                                                               |
| `spring.ai.anthropic.chat.cache-options.strategy` | `SYSTEM_AND_TOOLS`          | prompt caching of the system prompt and tool definitions (they arrive in 6.4 and U11)                            |
| `spring.ai.anthropic.chat.inference-geo`          | `global`                    | where Anthropic processes the request (`global`, `us`, `eu`)                                                     |

`temperature`, `top-p` and `top-k` are left commented out, so the model defaults apply.

`application-test.yml` sets a dummy key, since every test stubs the `ChatModel` and never calls the API.

```bash
export ANTHROPIC_API_KEY=sk-ant-...             # or set it once as a user environment variable
./mvnw -pl spring-ai-training spring-boot:run
```

**One central `ChatClient`.** `config/ai/ChatClientConfig` turns the auto-configured `ChatClient.Builder` into the single
`ChatClient` bean, with `SimpleLoggerAdvisor` as a default advisor. Features inject `ChatClient` and never see `AnthropicChatModel`,
so switching providers (7.4) touches config, not feature code. `SimpleLoggerAdvisor` logs at DEBUG, and only
`application-local.yml` enables DEBUG for `org.springframework.ai.chat.client.advisor`: prompts and completions can hold user
data, so they never reach logs at INFO outside `local`.

**The endpoint.** `POST /api/v1/chat-completions`, single turn with no memory (memory is U9):

```bash
curl -s -X POST localhost:8080/api/v1/chat-completions \
     -H 'Content-Type: application/json' \
     -d '{"message": "Explain Spring AI ChatClient in one sentence."}'
# {"content":"...","model":"claude-sonnet-5","usage":{"promptTokens":18,"completionTokens":42,"totalTokens":60}}
```

`ChatService` calls `chatClient.prompt().user(message).call().chatResponse()`. The full `ChatResponse`, rather than
`.content()`, also carries the provider metadata the response exposes: the model name and the token usage (10.4 turns token
usage into metrics).

| Case                                    | Status | Body                                                   |
|-----------------------------------------|--------|--------------------------------------------------------|
| reply                                   | 200    | `ChatCompletionResponse` (`content`, `model`, `usage`) |
| blank or > 4000-char `message`          | 400    | ProblemDetail + `errors[{field, message}]`             |
| unsupported version, e.g. `/api/v2/...` | 400    | ProblemDetail (Spring MVC's own)                       |
| Anthropic unreachable / API error       | 502    | ProblemDetail, `detail` without provider internals     |

**API versioning (Spring Framework 7 / Boot 4).** The `v1` in the URL is not a hard-coded path segment. MVC resolves it as an
API version:

```yaml
spring.mvc.apiversion:
	use.path-segment: 1   # /api/{version}/... -> segment index 1
	supported: 1
```

```java

@RequestMapping("/api/{version}/chat-completions")
class ChatController {
	@PostMapping(version = "v1") ...
}
```

A `v2` handler can sit on the same path beside `v1`, and a request for a version not in `supported` gets a 400 before any
handler runs. The same property family also offers header (`use.header: API-Version`), query-parameter and media-type-parameter
strategies. Path segment was chosen to keep CLAUDE.md's `/api/v1/<resource>` URLs. The mapping says `"v1"` rather than `"1"`
(the parser treats them the same) because springdoc writes the literal into the documented path, and `"1"` would publish
`/api/1/chat-completions`.

Swagger UI: `http://localhost:8080/swagger-ui.html` (spec at `/v3/api-docs`).

**Tests.** `ChatControllerTest` (`@WebMvcTest`, service mocked) covers the JSON contract, 400 on validation, 502 on model
failure, and 400 on an unsupported version. `ChatApiIT` (`@SpringBootTest`) runs the real context and the real `ChatClient`
over a `@Primary` stub `ChatModel`, so neither test needs an API key or network access.