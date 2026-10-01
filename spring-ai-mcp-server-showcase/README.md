# spring-ai-mcp-server-showcase

## 12.1 MCP Server

A standalone MCP server that publishes a read-only view of the product catalog (the `training` schema of
spring-boot-training) so Claude Code can search it in plain language. No model lives here: the client brings its own. Built fresh,
no code shared with `spring-ai-mcp-server-training`.

| What      | Value                                                                     |
|-----------|---------------------------------------------------------------------------|
| Transport | Streamable HTTP, `http://localhost:8091/mcp` (port 8091, not 8080 / 8090) |
| Server    | `spring-ai-starter-mcp-server-webmvc`, `type: SYNC`                       |
| Data      | Oracle 23ai, `training.product` / `training.category`, via `JdbcClient`   |
| DB user   | `ai_training` (READ on those two tables, from `compose/oracle-startdb`)   |

## Tools

| Tool             | Arguments (all optional unless noted)          | Returns                                                    |
|------------------|------------------------------------------------|------------------------------------------------------------|
| `listCategories` | none                                           | category names with product counts                         |
| `searchProducts` | `query`, `category`, `maxPrice` (EUR), `limit` | `sku`, `name`, `category`, `price`, ordered by name        |
| `getProduct`     | `sku` (required, case-insensitive)             | the above plus `description`                               |
| `enrichProduct`  | `sku` (required)                               | catalog text and a new text from the client's model (12.2) |

Rules worth knowing:

- Read-only: the service only runs `select`. Every value is a bind variable; the one thing written into the SQL is the schema name,
  which `app.catalog.schema` restricts to a plain identifier.
- The generated `GEN-*` demo rows are never returned (500 identical placeholders would crowd the real products).
- `limit` defaults to `app.catalog.default-limit` (10) and is clamped to `app.catalog.max-limit` (20), so one call cannot flood the
  model's context.
- `%`, `_` and `\` in `query` match themselves; they are not SQL wildcards.
- A failure (unknown SKU, blank SKU, negative `maxPrice`) is returned as an MCP error result (`isError: true`) with a message the model
  can act on, not as a protocol error.
- `price` is JSON, so `1749.00` arrives as `1749`.

## Run it

```
docker compose up -d oracle                               # spring-boot-training's data must be migrated already
./mvnw -pl spring-ai-mcp-server-showcase spring-boot:run  # starts on 8091 with the local profile
curl http://localhost:8091/actuator/health
```

## Connect Claude Code

Project-scoped, so it travels with that project and never touches your global config. In the project where you want the tools, add a
`.mcp.json` at the root:

```json
{
	"mcpServers": {
		"catalog-showcase": {
			"type": "http",
			"url": "http://localhost:8091/mcp"
		}
	}
}
```

or let Claude Code write it:

```
claude mcp add --transport http --scope project catalog-showcase http://localhost:8091/mcp
```

Then start `claude` in that project, approve the project server when asked, and check `/mcp`: `catalog-showcase` should show as connected
with 3 tools. Claude Code names them `mcp__catalog__searchProducts`, `mcp__catalog__getProduct`, `mcp__catalog__listCategories`.

Demo prompts:

- "What product categories does the catalog have?"
- "Find laptops under 2000 EUR and tell me which is the cheapest."
- "Give me the full description of LAP-0004."
- "Is there a product with SKU NOPE-1?" (shows the error result being read and explained)

## 12.2 MCP annotations: @McpTool, @McpToolParam, @McpResource

12.1 published the tools with Spring AI's `@Tool`. 12.2 adds the MCP-native annotations (`org.springframework.ai.mcp.annotation`, found by
the annotation scanner, `spring.ai.mcp.server.annotation-scanner.enabled: true`).

### @Tool or @McpTool

`app.catalog.tool-style` picks which one publishes the three catalog tools (`mcp` is the default, `tool` is the 12.1 style). Only one runs:
two tools with the same name in one server would clash. A client sees the same names, descriptions and arguments either way, and
`ToolStyleIT` runs the whole tool test class against the `tool` style to prove it.

|                                                                                           | `@Tool` / `@ToolParam` (12.1)                              | `@McpTool` / `@McpToolParam` (12.2)                                   |
|-------------------------------------------------------------------------------------------|------------------------------------------------------------|-----------------------------------------------------------------------|
| Package                                                                                   | `org.springframework.ai.tool.annotation`                   | `org.springframework.ai.mcp.annotation`                               |
| Works outside MCP                                                                         | yes: the same class can be offered to a local `ChatClient` | no, MCP only                                                          |
| Published by                                                                              | a `ToolCallbackProvider` bean (`CatalogToolsConfig`)       | the annotation scanner, no bean needed                                |
| MCP hints (`readOnlyHint`, `destructiveHint`, `idempotentHint`, `openWorldHint`, `title`) | not expressible                                            | `annotations = @McpTool.McpAnnotations(...)`                          |
| Optional argument                                                                         | `@ToolParam(required = false)`, default `true`             | `@McpToolParam(required = false)`, default `true`                     |
| Server context (progress, logging, sampling)                                              | not available                                              | a `McpSyncRequestContext` parameter, not part of the schema           |
| Failure                                                                                   | exception message goes back as an error result             | the same, but the text is `message` + line break + root cause message |

Why the hints matter: they are metadata a client may use, for example to treat a `readOnlyHint: true` tool as safe to run without a
prompt. Which clients act on them, and how, is up to the client; Claude Code's behaviour was not checked here. All three catalog tools
declare themselves read-only, idempotent and closed-world.

A note on the last row: with no cause, the error text repeats the message twice (`...\r\n...`, seen live). Harmless to a model, ugly in a
log.

### Resources

A tool is something the model decides to call, with arguments. A resource is content addressed by a URI that the application or the user
attaches to the conversation. `CatalogResources` serves the same data both ways:

| URI                       | Listed under               | Content                                                     |
|---------------------------|----------------------------|-------------------------------------------------------------|
| `catalog://categories`    | `resources/list`           | the categories with counts, JSON                            |
| `catalog://product/{sku}` | `resources/templates/list` | one product, JSON; the `{sku}` becomes the method parameter |

```
resources/read  {"uri": "catalog://product/LAP-0004"}  ->  {"contents":[{"uri":"...","mimeType":"application/json","text":"{...}"}]}
```

An unknown SKU makes `resources/read` fail with an error, not an empty result. Observed in 2.0.1: the `title` attribute of
`@McpResource` does not reach `resources/list` (it came back `null`), so `name` and `description` carry the meaning. In Claude Code a
resource is attached with an `@`-mention; support for resources differs between MCP clients, which is why the tools stay the primary
interface.

### enrichProduct: what only the MCP annotations can do

`enrichProduct(sku)` takes a `McpSyncRequestContext` and talks back to the client while it runs:

- **Sampling**: the server has no model. It asks the client's own model to write a short customer-facing description from the catalog facts
  (`context.sample(...)`), so the user's model, key and consent flow are used and no provider code lives here. The catalog text is wrapped
  as data in the prompt and the system prompt says to ignore instructions inside it.
- **Progress**: `progress(1/2)` before and `progress(2/2)` after the model call, tied to the client's `progressToken`. Without a token the
  SDK
  only logs a warning.
- **Logging**: `info("Enriching LAP-0004")`, shown in the client's own log view (the client sets the level with `logging/setLevel`).

A client that does not offer sampling gets a readable error (`This client does not support sampling ... Use getProduct`), checked live
with plain `curl`. A real sampling round trip needs a client with a model, so it is covered by `EnrichProductIT` (the SDK client plays the
model) and was not run against Claude Code: whether Claude Code answers sampling requests is not verified here.

### Tests (12.2)

- `McpServerShowcaseIT` (10) and `ToolStyleIT` (10, the same tests on the `tool` style): four tools, only `sku` required, hints present
  in one style and absent in the other, search / get / categories / error paths.
- `McpResourcesIT` (6): list, template list, read both, case-insensitive SKU, unknown and demo SKU fail.
- `EnrichProductIT` (3): sampling request content, progress and log notifications, no-sampling error, unknown SKU asks no model.

## Check the server without a client

```
curl -s -D hdr.txt -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' http://localhost:8091/mcp \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'
# take Mcp-Session-Id from hdr.txt, send notifications/initialized, then tools/list and tools/call with the same header
```

## Tests

- `CatalogServiceTest`: LIKE escaping and limit clamping, no database.
- `McpServerShowcaseIT`: real port, Testcontainers Oracle, the catalog tables created from `src/test/resources/sql`, driven by the MCP
  Java SDK client (initialize, list, call each tool, the error paths). Needs Docker, not the compose stack.

## Pitfalls

- Port 8091 busy: another copy is running. Check `netstat -ano | findstr :8091` before blaming the tools.
- The server starts without the catalog being readable and fails on the first call (`ORA-00942`): the `ai_training` user lacks READ on
  `training.product`; recreate the compose Oracle container so `compose/oracle-startdb` re-runs, and migrate spring-boot-training first.
- Over HTTP the server is unauthenticated: bind it to localhost only, as here. Do not expose it as is.