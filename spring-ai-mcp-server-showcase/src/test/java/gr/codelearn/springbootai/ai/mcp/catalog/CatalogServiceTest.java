package gr.codelearn.springbootai.ai.mcp.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CatalogServiceTest {

	private final CatalogService service = new CatalogService(mock(JdbcClient.class),
	                                                          new CatalogProperties("training", 10, 20, CatalogProperties.ToolStyle.MCP));

	@Test
	void likePatternMakesWildcardsLiteralAndLowercases() {
		assertThat(CatalogService.likePattern("  100%_Ready\\ ")).isEqualTo("%100\\%\\_ready\\\\%");
	}

	@Test
	void limitDefaultsWhenOmitted() {
		assertThat(service.clamp(null)).isEqualTo(10);
	}

	@Test
	void limitIsKeptBetweenOneAndTheMaximum() {
		assertThat(service.clamp(0)).isEqualTo(1);
		assertThat(service.clamp(-5)).isEqualTo(1);
		assertThat(service.clamp(7)).isEqualTo(7);
		assertThat(service.clamp(500)).isEqualTo(20);
	}
}