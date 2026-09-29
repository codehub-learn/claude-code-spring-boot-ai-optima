package gr.codelearn.springbootai.ai.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Unit 10.2: how the knowledge base chunks documents, bound from {@code app.ai.knowledge.*}. The catalog's schema (10.1's
 * {@code catalog-schema}) moved to {@code config.persistence.CatalogProperties} in 11.1, shared with the {@code product} feature.
 *
 * @param chunking the chunking settings
 */
@Validated
@ConfigurationProperties("app.ai.knowledge")
record KnowledgeProperties(@DefaultValue @Valid Chunking chunking) {

	/**
	 * Unit 10.2: chunking, bound from {@code app.ai.knowledge.chunking.*}.
	 *
	 * @param active         the strategy the answers endpoint retrieves from; ingestion always stores all of them
	 * @param tokenChunkSize the target chunk size of {@link ChunkingStrategy#TOKEN}, in tokens (Spring AI's default, 800, would leave
	 *                          every
	 *                       document here in one piece)
	 */
	record Chunking(@DefaultValue("markdown") @NotNull ChunkingStrategy active,
					@DefaultValue("200") @Min(50) @Max(2000) int tokenChunkSize) {
	}
}