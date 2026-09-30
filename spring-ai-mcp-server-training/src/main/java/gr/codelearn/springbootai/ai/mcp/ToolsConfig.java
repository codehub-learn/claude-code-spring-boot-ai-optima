package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.mcp.tools.ClockTools;
import gr.codelearn.springbootai.ai.mcp.tools.MathTools;
import gr.codelearn.springbootai.ai.mcp.tools.WikipediaProperties;
import gr.codelearn.springbootai.ai.mcp.tools.WikipediaTools;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

/**
 * Publishes the {@code @Tool} methods over MCP. The MCP server autoconfiguration turns every {@link ToolCallbackProvider} bean into server
 * tools (name, description and JSON schema come from the annotations, exactly as for a local tool), so adding a tool means adding its
 * component to this list.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WikipediaProperties.class)
class ToolsConfig {

	@Bean
	ToolCallbackProvider mcpTools(final ClockTools clockTools, final MathTools mathTools, final WikipediaTools wikipediaTools) {
		return MethodToolCallbackProvider.builder().toolObjects(clockTools, mathTools, wikipediaTools).build();
	}

	/**
	 * The HTTP client of {@link WikipediaTools}, with timeouts: a slow Wikipedia must not hold an MCP call open. Wikipedia asks API
	 * clients
	 * to identify themselves in the User-Agent.
	 */
	@Bean
	RestClient wikipediaClient(final WikipediaProperties properties) {
		final var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
		factory.setReadTimeout(properties.readTimeout());
		return RestClient.builder()
						 .requestFactory(factory)
						 .baseUrl(properties.baseUrl())
						 .defaultHeader("User-Agent",
		                                "spring-ai-mcp-server-training/0.0.1 (Code.Learn training; https://www.codehub.gr/codelearn/)")
						 .build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}