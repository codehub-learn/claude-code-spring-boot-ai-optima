package gr.codelearn.springbootai.ai.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 12.1's {@code @Tool} style behind the same server: every inherited test passes unchanged, so the catalog tools behave the same in
 * both annotation styles. Only the MCP hints differ, because {@code @Tool} cannot express them.
 */
@TestPropertySource(properties = "app.catalog.tool-style=tool")
class ToolStyleIT extends McpServerShowcaseIT {

	@Override
	@Test
	void mcpToolsCarryReadOnlyHints() {
		final var tool = client.listTools().tools().stream().filter(t -> t.name().equals("searchProducts")).findFirst().orElseThrow();

		assertThat(tool.annotations() == null || tool.annotations().readOnlyHint() == null || !tool.annotations().readOnlyHint()).isTrue();
	}
}