package gr.codelearn.springbootai.training;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the training module.
 *
 * <p>{@code @SpringBootApplication} is a meta-annotation that bundles
 * {@code @SpringBootConfiguration} + {@code @EnableAutoConfiguration} + {@code @ComponentScan}. See {@code README.md} (topic 1.1) for how
 * starters, auto-configuration and the Actuator endpoints fit together.
 *
 * <p>Rather than the one-line {@code SpringApplication.run(...)}, this {@code main} builds a
 * {@link SpringApplication} instance so the customization hooks (banner mode, bootstrap listeners) are explicit.
 *
 * <p>{@code @ConfigurationPropertiesScan} (topic 1.3) registers every {@code @ConfigurationProperties} record in
 * this package tree ({@link gr.codelearn.springbootai.training.config.AppProperties}) without an explicit
 * {@code @EnableConfigurationProperties} list.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
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