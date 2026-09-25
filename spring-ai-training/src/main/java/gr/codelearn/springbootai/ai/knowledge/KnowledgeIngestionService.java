package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.knowledge.BundledDocumentReader.BundledDocument;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeIngestionResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit 10.1: step 1 of RAG, ingestion (offline, before any question): read the sources, embed every document, store text + metadata +
 * vector in Oracle ({@code VectorStore.add} does the embedding through the {@code EmbeddingModel}, in batches).
 *
 * <p>Unit 10.2: every bundled document is stored once per {@link ChunkingStrategy} (whole, token chunks, Markdown sections), tagged by
 * the {@code chunking} metadata, so the strategies can be compared on the same store and the active one switched without re-ingesting.
 * Catalog rows are stored once, marked as not chunked, and searched by every strategy.
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
	private final DocumentChunker chunker;
	private final VectorStore vectorStore;

	KnowledgeIngestionService(final CatalogDocumentReader catalogReader, final BundledDocumentReader bundledReader,
							  final DocumentChunker chunker, final VectorStore vectorStore) {
		this.catalogReader = catalogReader;
		this.bundledReader = bundledReader;
		this.chunker = chunker;
		this.vectorStore = vectorStore;
	}

	KnowledgeIngestionResponse ingest() {
		final List<Document> catalog = catalogReader.read();
		final List<BundledDocument> bundled = bundledReader.read();
		final List<Document> rows = new ArrayList<>(catalog);
		final Map<String, Integer> chunksByStrategy = new LinkedHashMap<>();
		for (final ChunkingStrategy strategy : ChunkingStrategy.values()) {
			final List<Document> chunks = bundled.stream().flatMap(document -> chunker.chunk(document, strategy).stream()).toList();
			chunksByStrategy.put(strategy.key(), chunks.size());
			rows.addAll(chunks);
		}
		final long start = System.nanoTime();
		try {
			vectorStore.delete("source in ['" + CatalogDocumentReader.SOURCE + "', '" + BundledDocumentReader.SOURCE + "']");
			vectorStore.add(rows);
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException("Ingestion failed: the embedding model or the vector store is unavailable", ex);
		}
		final long millis = (System.nanoTime() - start) / 1_000_000;
		log.info("Ingested {} catalog rows and {} bundled documents as {} chunks in {} ms", catalog.size(), bundled.size(),
				 chunksByStrategy, millis);
		return new KnowledgeIngestionResponse(catalog.size(), bundled.size(), chunksByStrategy, rows.size(), millis);
	}
}