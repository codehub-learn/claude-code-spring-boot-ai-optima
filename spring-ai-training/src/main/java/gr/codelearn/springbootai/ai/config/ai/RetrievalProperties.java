package gr.codelearn.springbootai.ai.config.ai;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Unit 10.1: the retrieval step of RAG, bound from {@code app.ai.retrieval.*} and turned into the {@code SearchRequest} of
 * {@link RagConfig}'s advisor.
 *
 * @param topK                how many documents are retrieved per question (at most); more is more context and more prompt tokens
 * @param similarityThreshold the minimum similarity (1 - cosine distance) a document needs; {@code 0} accepts the whole top-k. Any other
 *                            value only works with {@code spring.ai.vectorstore.oracle.forced-normalization: true}
 */
@Validated
@ConfigurationProperties("app.ai.retrieval")
record RetrievalProperties(@DefaultValue("4") @Min(1) @Max(20) int topK,
						   @DefaultValue("0.0") @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold) {
}