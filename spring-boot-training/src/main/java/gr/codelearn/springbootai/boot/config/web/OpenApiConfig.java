package gr.codelearn.springbootai.boot.config.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Document-level OpenAPI metadata (U4.1). springdoc-openapi discovers every {@code @RestController} and builds the paths/schemas on its
 * own; this bean only supplies the parts it cannot infer — title, version, contact, and the tag order shown in Swagger UI.
 *
 * <p>The version is read from {@code META-INF/build-info.properties} (written by the {@code spring-boot:build-info} goal, U1.2) when it is
 * present, and falls back to {@code "dev"} under {@code spring-boot:run}, where no jar manifest or build-info file exists — the same gap
 * the banner {@code @project.version@} token works around.
 *
 * <p>Spec: {@code /v3/api-docs}. UI: {@code /swagger-ui.html}. Both paths are set (to their defaults, for visibility) in
 * {@code application.yml}.
 */
@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI trainingOpenApi(final ObjectProvider<BuildProperties> buildProperties) {
		final String version = buildProperties.stream()
											  .map(BuildProperties::getVersion)
											  .findFirst()
											  .orElse("dev");

		return new OpenAPI()
				.info(new Info()
							  .title("Code.Learn Spring Boot Training API")
							  .description("Catalog & Orders REST API — the U4 teaching slice of the Spring Boot training curriculum.")
							  .version(version)
							  .contact(new Contact().name("Code.Learn by Code.Hub").url("https://www.codehub.gr/codelearn/"))
							  .license(new License().name("Apache-2.0")))
				.tags(List.of(
						new Tag().name("catalog").description("Products and categories: browse, create, update, delete."),
						new Tag().name("orders").description("Place orders, read them back, and move them through their lifecycle.")));
	}
}