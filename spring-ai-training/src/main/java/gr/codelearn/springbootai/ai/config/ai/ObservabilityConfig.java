package gr.codelearn.springbootai.ai.config.ai;

import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Unit 10.4: what Spring AI's observations may carry into the telemetry backend.
 *
 * <p>Model calls, embeddings and vector searches are observed out of the box: each is a span and a timer, and model calls also count
 * tokens ({@code gen_ai.client.token.usage}). The {@code spring.ai.*.observations.*} properties in {@code application.yml} keep prompt and
 * completion text out of them. One piece of content has no property: the vector search span's {@code db.vector.query.content} attribute,
 * which is the user's question (for RAG, the text that was embedded). Found in Tempo on the first live run.
 *
 * <p>{@link #dropVectorQueryContent()} removes it before any handler sees the finished observation, in every profile (CLAUDE.md: no
 * prompt content in telemetry outside local; SimpleLoggerAdvisor already shows it locally). Spring Boot applies every
 * {@link ObservationFilter} bean to the {@code ObservationRegistry}.
 */
@Configuration
class ObservabilityConfig {

	@Bean
	ObservationFilter dropVectorQueryContent() {
		return context -> {
			if (context instanceof VectorStoreObservationContext) {
				context.removeHighCardinalityKeyValue(HighCardinalityKeyNames.DB_VECTOR_QUERY_CONTENT.asString());
			}
			return context;
		};
	}
}