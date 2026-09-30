package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.mcp.tools.ClockTools;
import gr.codelearn.springbootai.ai.mcp.tools.MathTools;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Publishes the {@code @Tool} methods over MCP. The MCP server autoconfiguration turns every {@link ToolCallbackProvider} bean into server
 * tools (name, description and JSON schema come from the annotations, exactly as for a local tool), so adding a tool means adding its
 * component to this list.
 */
@Configuration(proxyBeanMethods = false)
class ToolsConfig {

	@Bean
	ToolCallbackProvider mcpTools(final ClockTools clockTools, final MathTools mathTools) {
		return MethodToolCallbackProvider.builder().toolObjects(clockTools, mathTools).build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}