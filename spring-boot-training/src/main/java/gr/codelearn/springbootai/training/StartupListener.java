package gr.codelearn.springbootai.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Bootstrap listener wired via {@code SpringApplication.addListeners(...)} in {@link TrainingApplication}.
 *
 * <p>It reacts to {@link ApplicationReadyEvent} (published once the context is refreshed and the app can serve
 * requests) and logs a short startup summary. Registering it on the {@code SpringApplication} rather than as a bean means it also sees the
 * very early bootstrap events, should we add handling for them later.
 */
class StartupListener implements ApplicationListener<ApplicationReadyEvent> {
	private static final Logger log = LoggerFactory.getLogger(StartupListener.class);

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		final Environment environment = event.getApplicationContext().getEnvironment();
		final String[] activeProfiles = environment.getActiveProfiles();
		log.info("'{}' is ready. Active profiles: {}. Run with --debug to print the auto-configuration report.",
				 environment.getProperty("spring.application.name"),
				 activeProfiles.length == 0 ? "(default)" : Arrays.toString(activeProfiles));
	}
}