package gr.codelearn.springbootai.ai.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Logs a short startup summary once the application can serve requests, including where the MCP endpoint is.
 */
class StartupListener implements ApplicationListener<ApplicationReadyEvent> {
	private static final Logger log = LoggerFactory.getLogger(StartupListener.class);

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		final Environment environment = event.getApplicationContext().getEnvironment();
		final String[] activeProfiles = environment.getActiveProfiles();
		log.info("'{}' is ready on port {}, MCP endpoint {}. Active profiles: {}.", environment.getProperty("spring.application.name"),
				 environment.getProperty("local.server.port"),
				 environment.getProperty("spring.ai.mcp.server.streamable-http.mcp-endpoint", "/mcp"),
				 activeProfiles.length == 0 ? "(default)" : Arrays.toString(activeProfiles));
	}
}