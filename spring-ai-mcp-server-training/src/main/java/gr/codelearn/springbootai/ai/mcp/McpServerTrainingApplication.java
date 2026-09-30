package gr.codelearn.springbootai.ai.mcp;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the minimal MCP server of unit 11.4: a Spring Boot web application that publishes a few tools over the Model Context
 * Protocol (streamable HTTP, {@code /mcp}). It has no model, no database and no state; an MCP client (here {@code spring-ai-training},
 * later Claude Code) discovers the tools and calls them.
 */
@SpringBootApplication
public class McpServerTrainingApplication {
	static void main(final String[] args) {
		final var application = new SpringApplication(McpServerTrainingApplication.class);
		application.setBannerMode(Banner.Mode.CONSOLE);
		application.addListeners(new StartupListener());
		application.run(args);
	}
}