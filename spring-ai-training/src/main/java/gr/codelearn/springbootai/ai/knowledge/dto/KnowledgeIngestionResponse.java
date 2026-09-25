package gr.codelearn.springbootai.ai.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The result of rebuilding the knowledge base.
 *
 * @param catalogDocuments one per catalog product (the generated demo rows are skipped)
 * @param bundledDocuments one per bundled policy document
 * @param totalDocuments   both together: the rows now in the vector store
 * @param durationMillis   how long embedding and storing took
 */
public record KnowledgeIngestionResponse(
		@Schema(example = "24") int catalogDocuments,
		@Schema(example = "4") int bundledDocuments,
		@Schema(example = "28") int totalDocuments,
		@Schema(example = "850") long durationMillis) {
}