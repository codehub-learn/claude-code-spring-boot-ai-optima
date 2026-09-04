package gr.codelearn.springbootai.training;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the training module.
 *
 * <p>{@code @SpringBootApplication} is a meta-annotation that bundles
 * {@code @SpringBootConfiguration} + {@code @EnableAutoConfiguration} + {@code @ComponentScan}. See {@code README.md} (topic 1.1) for how
 * starters, auto-configuration and the Actuator endpoints fit together.
 *
 * <p>Rather than the one-line {@code SpringApplication.run(...)}, this {@code main} builds a
 * {@link SpringApplication} instance so the customization hooks (banner mode, bootstrap listeners) are explicit.
 */
@SpringBootApplication
public class TrainingApplication {
	static void main(final String[] args) {
		final var application = new SpringApplication(TrainingApplication.class);
		// CONSOLE prints the banner to stdout; LOG routes it through the logging system; OFF disables it.
		application.setBannerMode(Banner.Mode.CONSOLE);
		// A SpringApplication listener runs during bootstrap, before the context is refreshed. This is a
		// different hook from a bean @EventListener (covered in U2).
		application.addListeners(new StartupListener());
		application.run(args);
	}
}