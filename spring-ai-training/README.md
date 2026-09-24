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

### 6.3 Models and messages

**The message model.** A `Prompt` is an ordered list of `Message`s plus optional `ChatOptions`. Every message has a role
(`MessageType`), text, and a metadata map:

| Type                  | Role (`MessageType`) | Who writes it           | Purpose                                                                                     |
|-----------------------|----------------------|-------------------------|---------------------------------------------------------------------------------------------|
| `SystemMessage`       | `system`             | the application         | standing instructions: persona, rules, output format (6.4 loads it from a template)         |
| `UserMessage`         | `user`               | the end user            | the question or task; can also carry `Media` (images, files)                                |
| `AssistantMessage`    | `assistant`          | the model               | a previous reply; or, instead of text, `ToolCall`s asking the application to run a tool     |
| `ToolResponseMessage` | `tool`               | the application (U11)   | the result of each requested tool, paired to its `ToolCall` by `id`                         |

The model is stateless: it only knows what is in the prompt. A "conversation" is the client (or, from U9, a memory advisor)
resending earlier `user` / `assistant` turns every time. A tool round trip looks like this; U11 lets Spring AI drive it:

```
UserMessage("What time is it in Athens?")
AssistantMessage(toolCalls = [ToolCall(id = "call-1", name = "currentTime", arguments = {"zone":"Europe/Athens"})])
ToolResponseMessage(responses = [ToolResponse(id = "call-1", name = "currentTime", responseData = "\"10:15\"")])
      → the model now answers from the tool result
```

**Building a prompt.** The fluent API and a hand-built `Prompt` produce the same thing:

```java
chatClient.prompt().system("Answer in one word.").user("Capital of Greece?").call().content();

Prompt prompt = new Prompt(List.of(new SystemMessage("Answer in one word."), new UserMessage("Capital of Greece?"),
								   new AssistantMessage("Athens."), new UserMessage("And of Italy?")),
						   ChatOptions.builder().temperature(0.0).maxTokens(50).build());   // per-call options, optional
chatClient.prompt(prompt).call().content();   // still goes through the default advisors
```

`new Prompt("Hi")` is a single `UserMessage`. Options set on a `Prompt` override `spring.ai.anthropic.chat.*` for that call only.

**Endpoint: `POST /api/v1/prompts`.** The message model exposed over REST. The client sends the whole conversation, and
`ChatService.completePrompt` maps each entry to the matching `Message` type (a `switch` over the role) and wraps them in a
`Prompt`:

```bash
curl -s -X POST localhost:8080/api/v1/prompts -H 'Content-Type: application/json' -d '{"messages": [
  {"role": "system",    "content": "Answer in one word."},
  {"role": "user",      "content": "Capital of Greece?"},
  {"role": "assistant", "content": "Athens."},
  {"role": "user",      "content": "And of Italy?"}]}'
# {"content":"Rome.","model":"claude-sonnet-5","usage":{...}}
```

| Case                                                     | Status | Body                                                 |
|----------------------------------------------------------|--------|------------------------------------------------------|
| reply                                                    | 200    | `ChatCompletionResponse`, same shape as 6.2          |
| empty list, > 20 messages, blank or > 4000-char content  | 400    | ProblemDetail + `errors[{field, message}]`           |
| last message not `user`                                  | 400    | `errors[{field: "lastMessageFromUser", ...}]`        |
| unknown role (including `tool`)                          | 400    | ProblemDetail (unreadable body)                      |
| Anthropic unreachable / API error                        | 502    | ProblemDetail                                        |

`tool` is not accepted from clients: tool results are produced by the application when the model asks for a tool (U11).

**Blocking vs streaming.** `call()` waits until the model has finished and returns one `ChatResponse` (text + metadata such
as token usage). `stream()` returns a `Flux<ChatResponse>` that emits each fragment as the model generates it, so the first
words reach the user in a fraction of the total time. `stream().content()` is the `Flux<String>` of just the text
fragments; concatenated, they equal the blocking reply.

**Endpoint: `POST /api/v1/chat-completions` with `Accept: text/event-stream`.** Same URL and request body as 6.2; content
negotiation picks the streaming handler, which returns a `Flux` that Spring MVC writes as server-sent events. Without that
`Accept` header (or with `application/json`) the 6.2 blocking handler answers.

```bash
curl -N -X POST localhost:8080/api/v1/chat-completions \
     -H 'Content-Type: application/json' -H 'Accept: text/event-stream' \
     -d '{"message": "Write a haiku about Java."}'
# event:chunk
# data:{"content":"Steam"}
#
# event:chunk
# data:{"content":"ing cup"}
# ...
```

Two things the streaming design has to handle:

- **Each fragment is JSON (`{"content": ...}`), not raw text.** SSE clients drop one leading space after `data:` and split on
  newlines, so a raw fragment like `" world"` would lose its space.
- **Errors mid-stream are in-band.** Once the first event is sent, the response is already `200 text/event-stream`, so a model
  failure can't become a 502. The stream ends with an `event:error` whose data is the same `ProblemDetail` (`"status": 502`)
  the blocking endpoint returns. Validation (400) still happens before streaming starts, so it's a normal ProblemDetail
  response.

When a stream fails, Spring AI's `MessageAggregator` logs one `Aggregation Error` line at ERROR per aggregating layer in the
chain (5 with the default setup). That is logging noise, not retries: checked with a counting stub, the model is called and
subscribed once.

**Tests.** `MessagesAndPromptsTest` (no Spring context, stub `ChatModel` that implements both `call` and `stream`) walks
through the four message types, a hand-built tool round trip, `Prompt` construction with options, and `call` vs `stream`.
`PromptControllerTest` and `ChatControllerTest` (`@WebMvcTest`) cover the JSON/SSE contracts and validation; `ChatApiIT`
checks that the typed messages reach the model in order, the SSE body, and the 502 / in-band error paths through the real
`ChatClient`.