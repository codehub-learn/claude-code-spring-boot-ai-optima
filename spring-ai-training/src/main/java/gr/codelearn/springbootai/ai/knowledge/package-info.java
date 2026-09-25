/**
 * Unit 10.1: retrieval-augmented generation (RAG) over the store's knowledge: the catalog (spring-boot-training's product rows) and the
 * bundled policy documents ({@code src/main/resources/knowledge}). Two steps, two endpoints: ingestion (text to embeddings in Oracle AI
 * Vector Search) and answering (question, retrieval, context, generation). Contrast with {@code answer} (8.2), where the caller supplies
 * the documents.
 */
@NullMarked
package gr.codelearn.springbootai.ai.knowledge;

import org.jspecify.annotations.NullMarked;