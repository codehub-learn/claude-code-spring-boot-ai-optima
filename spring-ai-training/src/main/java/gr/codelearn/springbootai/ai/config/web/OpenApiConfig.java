package gr.codelearn.springbootai.ai.config.web;

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
 * Document-level OpenAPI metadata, the same shape as {@code spring-boot-training}'s {@code OpenApiConfig}. springdoc-openapi builds the
 * paths and schemas from the {@code @RestController}s; this bean supplies title, version, contact, and the tag list.
 *
 * <p>The version comes from {@code META-INF/build-info.properties} when present and falls back to {@code "dev"} under
 * {@code spring-boot:run}. Spec: {@code /v3/api-docs}. UI: {@code /swagger-ui.html}.
 */
@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI aiTrainingOpenApi(final ObjectProvider<BuildProperties> buildProperties) {
		final String version = buildProperties.stream()
											  .map(BuildProperties::getVersion)
											  .findFirst()
											  .orElse("dev");

		return new OpenAPI()
				.info(new Info()
							  .title("Code.Learn Spring AI Training API")
							  .description("Chat, memory, RAG, and tool-calling endpoints of the Spring AI training curriculum.")
							  .version(version)
							  .contact(new Contact().name("Code.Learn by Code.Hub").url("https://www.codehub.gr/codelearn/"))
							  .license(new License().name("Apache-2.0")))
				.tags(List.of(new Tag().name("chat")
									   .description(
											   "Chat completions through the central ChatClient: blocking or streamed, one message or a " +
											   "message list.")));
	}
}