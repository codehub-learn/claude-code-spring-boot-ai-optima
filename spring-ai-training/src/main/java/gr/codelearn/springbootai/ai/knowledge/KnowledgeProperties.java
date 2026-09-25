package gr.codelearn.springbootai.ai.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Unit 10.1: where the knowledge base reads its sources, bound from {@code app.ai.knowledge.*}. Unit 10.2: how documents are chunked.
 *
 * @param catalogSchema the Oracle schema that holds spring-boot-training's {@code product} and {@code category} tables ({@code training}
 *                      locally, readable by {@code ai_training} through {@code compose/oracle-startdb}). It is written into the SQL, so it
 *                      must be a plain identifier: the pattern keeps anything else out of the statement.
 * @param chunking      the chunking settings
 */
@Validated
@ConfigurationProperties("app.ai.knowledge")
record KnowledgeProperties(@DefaultValue("training") @NotNull @Pattern(regexp = "[A-Za-z][A-Za-z0-9_$#]{0,127}") String catalogSchema,
						   @DefaultValue @Valid Chunking chunking) {

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