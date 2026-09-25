package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.knowledge.BundledDocumentReader.BundledDocument;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unit 10.2: cuts a bundled document into the chunks that are embedded, one way per {@link ChunkingStrategy}.
 *
 * <p>Spring AI does the cutting ({@link TokenTextSplitter}, {@link MarkdownDocumentReader}); this class adds what both leave out. The
 * model
 * only ever sees a chunk's text (10.1), and a splitter keeps the {@code [doc:name]} tag on the first chunk only, while the Markdown reader
 * drops the heading from the text entirely (it goes to metadata). So every chunk is rebuilt with a <em>contextual header</em>: the citation
 * tag, the document title, and the section or part, e.g. {@code [doc:terms-of-sale] Terms of sale > Payment: ...}. The header is embedded
 * too, which helps a chunk that never names its topic ("It is held for 3 working days...") match a question about it.
 *
 * <p>Ids are stable per document, strategy and position, so a re-ingestion replaces rows.
 */
@Component
class DocumentChunker {

	private final KnowledgeProperties properties;

	DocumentChunker(final KnowledgeProperties properties) {
		this.properties = properties;
	}

	List<Document> chunk(final BundledDocument document, final ChunkingStrategy strategy) {
		final List<Chunk> chunks = switch (strategy) {
			case WHOLE -> List.of(new Chunk(null, document.content()));
			case TOKEN -> tokenChunks(document);
			case MARKDOWN -> markdownChunks(document);
		};
		final List<Document> documents = new ArrayList<>(chunks.size());
		for (int index = 0; index < chunks.size(); index++) {
			documents.add(toDocument(document, strategy, chunks.get(index), index, chunks.size()));
		}
		return documents;
	}

	private List<Chunk> tokenChunks(final BundledDocument document) {
		final TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(properties.chunking().tokenChunkSize()).build();
		return splitter.split(new Document(document.content()))
					   .stream()
					   .map(chunk -> new Chunk(null, Objects.requireNonNullElse(chunk.getText(), "")))
					   .toList();
	}

	private static List<Chunk> markdownChunks(final BundledDocument document) {
		final var resource = new ByteArrayResource(document.content().getBytes(StandardCharsets.UTF_8));
		return new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig())
				.get()
				.stream()
				.map(section -> new Chunk((String) section.getMetadata().get("title"), Objects.requireNonNullElse(section.getText(), "")))
				.toList();
	}

	private static Document toDocument(final BundledDocument document, final ChunkingStrategy strategy, final Chunk chunk, final int index,
									   final int count) {
		final String tag = "[%s:%s]".formatted(BundledDocumentReader.SOURCE, document.name());
		final String text = switch (strategy) {
			case WHOLE -> tag + " " + chunk.text();
			case TOKEN -> "%s %s (part %d of %d): %s".formatted(tag, document.title(), index + 1, count, chunk.text());
			case MARKDOWN -> chunk.section() == null || chunk.section().equals(document.title())
							 ? "%s %s: %s".formatted(tag, document.title(), chunk.text())
							 : "%s %s > %s: %s".formatted(tag, document.title(), chunk.section(), chunk.text());
		};
		final Map<String, Object> metadata = new HashMap<>();
		metadata.put("source", BundledDocumentReader.SOURCE);
		metadata.put("ref", document.name());
		metadata.put("title", document.title());
		metadata.put("chunking", strategy.key());
		metadata.put("chunk_index", index);
		metadata.put("chunk_count", count);
		if (chunk.section() != null) {
			metadata.put("section", chunk.section());
		}
		return Document.builder()
					   .id(CatalogDocumentReader.idFor(BundledDocumentReader.SOURCE, document.name() + "#" + strategy.key() + "#" + index))
					   .text(text)
					   .metadata(metadata)
					   .build();
	}

	/**
	 * A piece of a document before it gets its header: the section heading it came from (Markdown only) and its text.
	 */
	private record Chunk(@Nullable String section, String text) {
	}
}