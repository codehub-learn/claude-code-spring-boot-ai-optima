package gr.codelearn.springbootai.training.foundation.components;

import gr.codelearn.springbootai.training.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * The same key, {@code app.name}, read three ways:
 *
 * <ul>
 *   <li>{@code @Value("${app.name}")} — resolved once at construction; fails fast if the key is missing (no
 *       default given). Terse, but scatters config keys across the codebase as string literals;</li>
 *   <li>{@link Environment#getProperty(String)} — programmatic lookup against the merged property sources;
 *       returns {@code null} when absent. Good when the key name is dynamic;</li>
 *   <li>{@link AppProperties#name()} — the typed, validated {@code @ConfigurationProperties} record. The
 *       house default (CLAUDE.md): related keys bound and validated in one place, no literals at the use
 *       site.</li>
 * </ul>
 *
 * <p>All three return the same string. {@code ConfigAccessDemoTest} asserts that against the
 * {@code application-test.yml} value.
 */
@Component
class ConfigAccessDemo {

	private static final Logger log = LoggerFactory.getLogger(ConfigAccessDemo.class);

	private final String viaValueAnnotation;
	private final Environment environment;
	private final AppProperties appProperties;

	ConfigAccessDemo(@Value("${app.name}") final String viaValueAnnotation,
					 final Environment environment,
					 final AppProperties appProperties) {
		this.viaValueAnnotation = viaValueAnnotation;
		this.environment = environment;
		this.appProperties = appProperties;
	}

	@PostConstruct
	void logResolvedValues() {
		log.info("app.name via @Value='{}', via Environment='{}', via @ConfigurationProperties='{}'",
				 viaValueAnnotation, viaEnvironment(), viaConfigurationProperties());
	}

	String viaValueAnnotation() {
		return viaValueAnnotation;
	}

	@Nullable String viaEnvironment() {
		return environment.getProperty("app.name");
	}

	String viaConfigurationProperties() {
		return appProperties.name();
	}
}