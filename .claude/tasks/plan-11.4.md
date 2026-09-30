# Plan: 11.4 MCP client / server basics

## Scope

New runnable module `spring-ai-mcp-server-training` (`gr.codelearn.springbootai.ai.mcp`), plus an MCP client in `spring-ai-training`.
No model in the server; it only exposes tools. Both run at once, the client calls the server over HTTP.

## Server: `spring-ai-mcp-server-training`

- [ ] Register in the parent `<modules>`; pom via `maven-scaffold` (runnable code module, `spring-boot-maven-plugin`, layers, mainClass).
- [ ] Deps: `spring-ai-starter-mcp-server-webmvc` (BOM 2.0.1), actuator, test starters. No datasource, no model starter.
- [ ] `application.yml`: port 8090, `spring.ai.mcp.server.{name,version,protocol: STREAMABLE, type: SYNC}`, endpoint `/mcp`, health only.
- [ ] `TrainingMcpServerApplication`, `StartupListener`/banner like the sibling modules.
- [ ] `tools` feature: `ClockTools` (`@Tool currentTime(zoneId)`), `MathTools` (`@Tool add(a, b)`), a `ToolCallbackProvider` bean
      (`MethodToolCallbackProvider`) exposing them. Toy tools, validated input, unknown zone returns a readable error.
- [ ] Tests: `*Test` for the tools; `McpServerIT` connects with the MCP Java SDK client to a random port, lists the tools, calls both.

## Client: `spring-ai-training`

- [ ] Dep `spring-ai-starter-mcp-client` (HTTP client, no WebFlux).
- [ ] `application.yml`: `spring.ai.mcp.client.streamable-http.connections.training.{url: http://localhost:8090, endpoint: /mcp}`,
      request timeout, `app.mcp.*` if needed. Check whether the app must fail or start when the server is down (prefer: start, fail on use).
- [ ] `mcp` feature: `McpService` (list discovered tools, ask the central `ChatClient` with the MCP `ToolCallbackProvider` per call, recorded
      tool calls like the assistant), `McpController`: `GET /api/v1/mcp/tools`, `POST /api/v1/mcp/answers`. 502 if the server is unreachable.
- [ ] Tests: `McpControllerTest` (slice), `McpApiIT` (scripted model + a stub `ToolCallbackProvider`; MCP client disabled in the `test`
      profile so `verify` never needs the server).
- [ ] README: 11.4 section (MCP vs local tools, transports, server/client wiring, run both, curl round trip, pitfalls).
- [ ] Live demo: start the server on 8090, the app on 8081 (`provider=OLLAMA` check first), call both endpoints.

## Done when

`./mvnw verify` green from the root (compose stack stopped), files reformatted via the `idea` MCP, Review block appended to
`todo-spring-ai.md`, checkbox 11.4 ticked.
