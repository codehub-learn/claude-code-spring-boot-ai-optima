package gr.codelearn.springbootai.ai;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Spring AI core training module.
 *
 * <p>Mirrors {@code spring-boot-training}'s {@code TrainingApplication}: an explicit
 * {@link SpringApplication} instance (rather than the one-line {@code SpringApplication.run(...)}) so the customization hooks (banner mode,
 * bootstrap listeners) are explicit.
 *
 * <p>{@code @ConfigurationPropertiesScan} registers every {@code @ConfigurationProperties} record in this
 * package tree without an explicit {@code @EnableConfigurationProperties} list.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AiTrainingApplication {
	static void main(final String[] args) {
		final var application = new SpringApplication(AiTrainingApplication.class);
		// CONSOLE prints the banner to stdout; LOG routes it through the logging system; OFF disables it.
		application.setBannerMode(Banner.Mode.CONSOLE);
		application.addListeners(new StartupListener());
		application.run(args);
	}
}