package gr.codelearn.springbootai.ai.knowledge;

import java.util.Locale;

/**
 * Unit 10.2: how a bundled document is cut into the pieces that are embedded and retrieved. Every strategy is ingested side by side
 * (metadata {@code chunking}), so they can be compared on the same store; {@code app.ai.knowledge.chunking.active} picks the one answers
 * use.
 */
enum ChunkingStrategy {

	/**
	 * The 10.1 baseline: one chunk per document. Retrieval returns whole documents: the answer is always inside, next to everything else,
	 * and one embedding has to stand for every topic in the document.
	 */
	WHOLE,

	/**
	 * Fixed-size chunks from Spring AI's {@code TokenTextSplitter}, cut by token count (backing up to the last sentence end); blind to the
	 * document's structure.
	 */
	TOKEN,

	/**
	 * One chunk per Markdown heading section, from Spring AI's {@code MarkdownDocumentReader}: follows the author's structure, so chunk
	 * sizes vary with the sections.
	 */
	MARKDOWN;

	/**
	 * The value stored in the {@code chunking} metadata and used in filters and the API.
	 */
	String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * The vector store filter that searches this strategy's chunks plus the (never chunked) catalog rows.
	 */
	String filterExpression() {
		return "chunking in ['" + CatalogDocumentReader.NOT_CHUNKED + "', '" + key() + "']";
	}
}