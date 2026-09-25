package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeIngestionResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

/**
 * Unit 10.1: step 1 of RAG, ingestion (offline, before any question): read the sources, embed every document, store text + metadata +
 * vector in Oracle ({@code VectorStore.add} does the embedding through the {@code EmbeddingModel}, in batches).
 *
 * <p>A full rebuild each time: the documents of both sources are deleted first, so a product removed from the catalog leaves the store
 * too, then everything is added again. Stable ids make it idempotent. Not transactional: the embedding calls go to Ollama and must not hold
 * a database transaction open, so a failure half way leaves a partial store until the next run.
 */
@Service
class KnowledgeIngestionService {

	private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

	private final CatalogDocumentReader catalogReader;
	private final BundledDocumentReader bundledReader;
	private final VectorStore vectorStore;

	KnowledgeIngestionService(final CatalogDocumentReader catalogReader, final BundledDocumentReader bundledReader,
							  final VectorStore vectorStore) {
		this.catalogReader = catalogReader;
		this.bundledReader = bundledReader;
		this.vectorStore = vectorStore;
	}

	KnowledgeIngestionResponse ingest() {
		final List<Document> catalog = catalogReader.read();
		final List<Document> bundled = bundledReader.read();
		final long start = System.nanoTime();
		try {
			vectorStore.delete("source in ['" + CatalogDocumentReader.SOURCE + "', '" + BundledDocumentReader.SOURCE + "']");
			vectorStore.add(Stream.concat(catalog.stream(), bundled.stream()).toList());
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException("Ingestion failed: the embedding model or the vector store is unavailable", ex);
		}
		final long millis = (System.nanoTime() - start) / 1_000_000;
		log.info("Ingested {} catalog and {} bundled documents in {} ms", catalog.size(), bundled.size(), millis);
		return new KnowledgeIngestionResponse(catalog.size(), bundled.size(), catalog.size() + bundled.size(), millis);
	}
}