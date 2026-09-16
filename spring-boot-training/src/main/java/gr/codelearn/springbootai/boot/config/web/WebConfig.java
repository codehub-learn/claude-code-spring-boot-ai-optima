package gr.codelearn.springbootai.boot.config.web;

import gr.codelearn.springbootai.boot.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC-wide web configuration for the REST API (U4.1).
 *
 * <p><b>CORS.</b> The browser allow-list is config, not code: {@link AppProperties.Cors} carries the origins and the credentials flag,
 * bound and validated at startup (U1.3). Only {@code /api/**} is opened; the Actuator and springdoc endpoints keep the default same-origin
 * policy.
 *
 * <p><b>Paging guard-rails.</b> A controller method takes a bare {@link org.springframework.data.domain.Pageable} argument and Spring Data
 * resolves it from {@code ?page=&size=&sort=}. Left alone a caller could ask for {@code size=100000}; the
 * {@link PageableHandlerMethodArgumentResolverCustomizer} below clamps every request to {@link AppProperties.Catalog#maxPageSize()} and
 * falls back to {@link AppProperties.Catalog#defaultPageSize()} when {@code size} is absent — the concrete use of the {@code app.catalog.*}
 * limits the {@code AppProperties} Javadoc promised "from U3/U4".
 */
@Configuration
class WebConfig implements WebMvcConfigurer {

	private final AppProperties appProperties;

	WebConfig(final AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@Override
	public void addCorsMappings(final CorsRegistry registry) {
		final AppProperties.Cors cors = appProperties.cors();
		registry.addMapping("/api/**")
				.allowedOrigins(cors.allowedOrigins().toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
				.allowedHeaders("*")
				.allowCredentials(cors.allowCredentials());
	}

	@Bean
	PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
		final AppProperties.Catalog catalog = appProperties.catalog();
		return resolver -> {
			resolver.setMaxPageSize(catalog.maxPageSize());
			resolver.setFallbackPageable(PageRequest.of(0, catalog.defaultPageSize()));
		};
	}
}