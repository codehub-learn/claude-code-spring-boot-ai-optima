package gr.codelearn.springbootai.ai.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * The result of rebuilding the knowledge base.
 *
 * @param catalogDocuments one per catalog product (the generated demo rows are skipped), shared by every chunking strategy
 * @param bundledDocuments how many bundled Markdown files were read
 * @param chunks           unit 10.2: how many chunks the bundled files became, per chunking strategy ({@code whole}, {@code token},
 *                         {@code markdown})
 * @param totalRows        the rows now in the vector store: the catalog rows plus every strategy's chunks
 * @param durationMillis   how long embedding and storing took
 */
public record KnowledgeIngestionResponse(
		@Schema(example = "24") int catalogDocuments,
		@Schema(example = "8") int bundledDocuments,
		@Schema(example = "{\"whole\": 8, \"token\": 34, \"markdown\": 61}") Map<String, Integer> chunks,
		@Schema(example = "127") int totalRows,
		@Schema(example = "9500") long durationMillis) {
}