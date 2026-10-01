package gr.codelearn.springbootai.ai.mcp.catalog;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the {@code @Tool} methods over MCP (only in the {@code tool} style; {@code @McpTool} methods need no provider bean): the server
 * autoconfiguration turns every {@link ToolCallbackProvider} bean into server tools, with name, description and JSON schema taken from the
 * annotations.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.catalog", name = "tool-style", havingValue = "tool")
class CatalogToolsConfig {

	@Bean
	ToolCallbackProvider catalogToolCallbacks(final CatalogTools catalogTools) {
		return MethodToolCallbackProvider.builder().toolObjects(catalogTools).build();
	}
}