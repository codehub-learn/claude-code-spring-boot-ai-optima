package gr.codelearn.springbootai.ai.mcp.catalog;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the {@code @Tool} methods over MCP: the server autoconfiguration turns every {@link ToolCallbackProvider} bean into server
 * tools, with name, description and JSON schema taken from the annotations.
 */
@Configuration(proxyBeanMethods = false)
class CatalogToolsConfig {

	@Bean
	ToolCallbackProvider catalogToolCallbacks(final CatalogTools catalogTools) {
		return MethodToolCallbackProvider.builder().toolObjects(catalogTools).build();
	}
}