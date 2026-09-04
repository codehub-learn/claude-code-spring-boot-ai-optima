package gr.codelearn.springbootai.training.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two facets of {@link AppProperties} (topic 1.3): relaxed binding from properties, and {@code @Validated} failing the context when a
 * constraint is violated. {@link ApplicationContextRunner} spins a tiny context per case, no {@code @SpringBootTest}.
 */
class AppPropertiesTest {
	private final ApplicationContextRunner runner =
			new ApplicationContextRunner().withUserConfiguration(Enablement.class);

	@Test
	void bindsAndAppliesDefaultsForOmittedKeys() {
		runner.withPropertyValues(
					  "app.name=Acme Catalog",
					  "app.cors.allowed-origins=https://acme.test,https://admin.acme.test")
			  .run(context -> {
				  assertThat(context).hasSingleBean(AppProperties.class);
				  AppProperties props = context.getBean(AppProperties.class);

				  assertThat(props.name()).isEqualTo("Acme Catalog");
				  assertThat(props.cors().allowedOrigins()).containsExactly("https://acme.test", "https://admin.acme.test");
				  assertThat(props.cors().allowCredentials()).isFalse();
				  // app.catalog.* omitted entirely -> nested record still bound from @DefaultValue
				  assertThat(props.catalog().defaultPageSize()).isEqualTo(20);
				  assertThat(props.catalog().maxPageSize()).isEqualTo(100);
				  assertThat(props.catalog().cacheTtl().toSeconds()).isEqualTo(60);
			  });
	}

	@Test
	void failsFastWhenAConstraintIsViolated() {
		runner.withPropertyValues(
					  "app.name=", // @NotBlank
					  "app.cors.allowed-origins=https://acme.test")
			  .run(context -> assertThat(context).hasFailed()
												 .getFailure()
												 .hasStackTraceContaining("app.name"));
	}

	@Test
	void failsFastWhenANestedConstraintIsViolated() {
		runner.withPropertyValues(
					  "app.name=Acme",
					  "app.cors.allowed-origins=https://acme.test",
					  "app.catalog.default-page-size=0") // @Positive
			  .run(context -> assertThat(context).hasFailed());
	}

	@EnableConfigurationProperties(AppProperties.class)
	private static final class Enablement {
	}
}