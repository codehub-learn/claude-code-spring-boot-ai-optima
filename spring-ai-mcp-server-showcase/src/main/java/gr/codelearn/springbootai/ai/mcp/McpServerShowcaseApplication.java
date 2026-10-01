package gr.codelearn.springbootai.ai.mcp;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the unit 12 showcase: a Spring Boot web application that publishes a read-only view of the product catalog as MCP tools
 * (streamable HTTP, {@code /mcp}), so a client such as Claude Code can search it in plain language. No model lives here; the client brings
 * its own.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class McpServerShowcaseApplication {
	static void main(final String[] args) {
		final var application = new SpringApplication(McpServerShowcaseApplication.class);
		application.setBannerMode(Banner.Mode.CONSOLE);
		application.addListeners(new StartupListener());
		application.run(args);
	}
}