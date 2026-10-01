# spring-ai-mcp-server-showcase

Unit 12: a standalone MCP server that publishes a read-only view of the product catalog (the `training` schema of
spring-boot-training) so Claude Code can search it in plain language. No model lives here: the client brings its own. Built fresh,
no code shared with `spring-ai-mcp-server-training`.

| What      | Value                                                                     |
|-----------|---------------------------------------------------------------------------|
| Transport | Streamable HTTP, `http://localhost:8091/mcp` (port 8091, not 8080 / 8090) |
| Server    | `spring-ai-starter-mcp-server-webmvc`, `type: SYNC`                       |
| Data      | Oracle 23ai, `training.product` / `training.category`, via `JdbcClient`   |
| DB user   | `ai_training` (READ on those two tables, from `compose/oracle-startdb`)   |

## Tools

| Tool             | Arguments (all optional unless noted)          | Returns                                             |
|------------------|------------------------------------------------|-----------------------------------------------------|
| `listCategories` | none                                           | category names with product counts                  |
| `searchProducts` | `query`, `category`, `maxPrice` (EUR), `limit` | `sku`, `name`, `category`, `price`, ordered by name |
| `getProduct`     | `sku` (required, case-insensitive)             | the above plus `description`                        |

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