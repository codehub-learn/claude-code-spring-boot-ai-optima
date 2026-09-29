package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.knowledge.BundledDocumentReader.BundledDocument;
import gr.codelearn.springbootai.ai.knowledge.KnowledgeProperties.Chunking;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 10.2: how Spring AI 2.0.1 cuts text, and what {@link DocumentChunker} adds on top. No database, no model.
 */
class ChunkingTest {

	private static final String MARKDOWN = """
										   # Terms
										   
										   Intro paragraph.
										   
										   ## Payment
										   
										   Cards are charged at dispatch.
										   
										   Bank transfers are held for 3 days.
										   
										   ## Returns
										   
										   - 14 days
										   - store credit
										   """;

	private final DocumentChunker chunker = new DocumentChunker(
			new KnowledgeProperties(new Chunking(ChunkingStrategy.MARKDOWN, 200)));

	@Test
	void theTokenSplitterCutsByTokenCountAndKeepsNoContext() {
		final String text = "The warranty lasts two years. ".repeat(100);

		final List<Document> chunks = TokenTextSplitter.builder().withChunkSize(100).build().split(new Document("[doc:w] " + text));

		assertThat(chunks).hasSizeGreaterThan(5);
		// Only the first chunk keeps anything written at the top of the document, such as a citation tag.
		assertThat(chunks.getFirst().getText()).startsWith("[doc:w]");
		assertThat(chunks.get(1).getText()).doesNotContain("[doc:w]");
		assertThat(chunks.get(1).getMetadata()).containsKeys("parent_document_id", "chunk_index", "total_chunks");
	}

	@Test
	void theSplitterDefaultOf800TokensLeavesAShortDocumentWhole() {
		final String text = "The warranty lasts two years. ".repeat(100); // about 600 tokens

		assertThat(TokenTextSplitter.builder().build().split(new Document(text))).hasSize(1);
	}

	@Test
	void theMarkdownReaderMakesOneDocumentPerHeadingWithTheHeadingOnlyInMetadata() {
		final List<Document> sections = new MarkdownDocumentReader(new ByteArrayResource(MARKDOWN.getBytes(StandardCharsets.UTF_8)),
																   MarkdownDocumentReaderConfig.defaultConfig()).get();

		assertThat(sections).extracting(section -> section.getMetadata().get("title")).containsExactly("Terms", "Payment", "Returns");
		assertThat(sections.get(1).getText()).doesNotContain("Payment");
	}

	@Test
	void pitfallTheMarkdownReaderGluesParagraphsTogether() {
		final List<Document> sections = new MarkdownDocumentReader(new ByteArrayResource(MARKDOWN.getBytes(StandardCharsets.UTF_8)),
																   MarkdownDocumentReaderConfig.defaultConfig()).get();

		// Two paragraphs, no separator: "dispatch." runs straight into "Bank".
		assertThat(sections.get(1).getText()).isEqualTo("Cards are charged at dispatch.Bank transfers are held for 3 days.");
	}

	@Test
	void everyChunkGetsTheTagTheTitleAndItsSection() {
		final List<Document> chunks = chunker.chunk(new BundledDocument("terms", "Terms", MARKDOWN), ChunkingStrategy.MARKDOWN);

		assertThat(chunks).extracting(Document::getText)
						  .containsExactly("[doc:terms] Terms: Intro paragraph.",
										   "[doc:terms] Terms > Payment: Cards are charged at dispatch.Bank transfers are held for 3 " +
										   "days.",
										   "[doc:terms] Terms > Returns: 14 days store credit");
		assertThat(chunks.get(1).getMetadata()).containsEntry("section", "Payment").containsEntry("chunking", "markdown")
											   .containsEntry("chunk_index", 1).containsEntry("chunk_count", 3);
	}

	@Test
	void tokenChunksAreNumberedPartsOfTheDocument() {
		final BundledDocument document = new BundledDocument("w", "Warranty", "The warranty lasts two years. ".repeat(100));

		final List<Document> chunks = chunker.chunk(document, ChunkingStrategy.TOKEN);

		assertThat(chunks).hasSizeGreaterThan(1);
		assertThat(chunks.get(1).getText()).startsWith("[doc:w] Warranty (part 2 of " + chunks.size() + "): ");
	}

	@Test
	void idsDifferPerStrategyAndStayStable() {
		final BundledDocument document = new BundledDocument("terms", "Terms", MARKDOWN);

		assertThat(chunker.chunk(document, ChunkingStrategy.WHOLE).getFirst().getId()).isNotEqualTo(
				chunker.chunk(document, ChunkingStrategy.MARKDOWN).getFirst().getId());
		assertThat(chunker.chunk(document, ChunkingStrategy.MARKDOWN)).extracting(Document::getId)
																	  .isEqualTo(chunker.chunk(document, ChunkingStrategy.MARKDOWN)
																						.stream()
																						.map(Document::getId)
																						.toList());
	}

	@Test
	void theBundledLongDocumentsSplitIntoSeveralChunks() {
		final List<BundledDocument> documents = new BundledDocumentReader(new DefaultResourceLoader()).read();
		final BundledDocument terms = documents.stream()
											   .filter(document -> document.name().equals("terms-of-sale"))
											   .findFirst()
											   .orElseThrow();

		assertThat(chunker.chunk(terms, ChunkingStrategy.WHOLE)).hasSize(1);
		assertThat(chunker.chunk(terms, ChunkingStrategy.TOKEN)).hasSizeGreaterThan(4);
		assertThat(chunker.chunk(terms, ChunkingStrategy.MARKDOWN)).hasSizeGreaterThan(8);
	}
}