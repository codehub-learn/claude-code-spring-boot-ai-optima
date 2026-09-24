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
| `spring-ai-starter-model-<provider>`      | `-anthropic` (7.1 cloud), `-ollama` (7.1 local) | the provider's `ChatModel` / `EmbeddingModel` + its autoconfig |
| `spring-ai-starter-vector-store-<db>`     | `-oracle` (10.x RAG)                            | a `VectorStore` over Oracle 23ai AI Vector Search              |
| `spring-ai-starter-model-chat-memory-*`   | `-repository-jdbc` (9.2)                        | a persistent `ChatMemoryRepository`                            |
| `spring-ai-starter-mcp-{client,server}-*` | `-server-webmvc` (11.4, 12.1)                   | MCP client/server over HTTP                                    |

Every model starter pulls the same two provider-neutral jars. This module declares exactly those two, and no provider yet:

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

| What                         | Where                                                                                                          |
|------------------------------|----------------------------------------------------------------------------------------------------------------|
| central `ChatClient` bean(s) | `config/ai` (6.2); features inject `ChatClient`, never `new` a model client                                    |
| prompt templates             | `src/main/resources/prompts/*.st`, loaded as a `Resource` (6.4, 8.1)                                           |
| Spring AI config             | `spring.ai.*` in `application*.yml` (`spring.ai.anthropic.*`, `spring.ai.ollama.*`, `spring.ai.chat.client.*`) |
| API keys                     | environment only (`ANTHROPIC_API_KEY`), never a committed file                                                 |
| tools                        | `@Tool` methods on dedicated `*Tools` components (U11)                                                         |

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