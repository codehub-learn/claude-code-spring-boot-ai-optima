package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.knowledge.BundledDocumentReader.BundledDocument;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 10.1: the documents ingestion builds, without a database or a model. What matters is the text: it is both what is embedded and all
 * the model sees of a document, so the citation tag and the facts must be in it. Unit 10.2: the bundled files are read here and chunked by
 * {@link DocumentChunker} (see {@code ChunkingTest}).
 */
class KnowledgeDocumentsTest {

	@Test
	void aProductBecomesOneTaggedDocument() {
		final Document document = CatalogDocumentReader.toDocument("LAP-0002", "Latitude 16 Workstation",
																   "16\" mobile workstation, 32 GB RAM, 1 TB SSD, discrete GPU",
																   new BigDecimal("2199.00"), "Laptops");

		assertThat(document.getText()).isEqualTo("[catalog:LAP-0002] Latitude 16 Workstation. Category: Laptops. Price: 2199.00 EUR. "
												 + "16\" mobile workstation, 32 GB RAM, 1 TB SSD, discrete GPU");
		assertThat(document.getMetadata()).containsEntry("source", "catalog").containsEntry("ref", "LAP-0002").containsEntry("chunking",
		                                                                                                                     "none");
	}

	@Test
	void idsAreStableSoReIngestionReplacesRows() {
		assertThat(CatalogDocumentReader.idFor("catalog", "LAP-0002")).isEqualTo(CatalogDocumentReader.idFor("catalog", "LAP-0002"))
																	  .isNotEqualTo(CatalogDocumentReader.idFor("doc", "LAP-0002"))
																	  .hasSize(36);
	}

	@Test
	void everyBundledFileIsReadWithItsTitle() {
		final List<BundledDocument> documents = new BundledDocumentReader(new DefaultResourceLoader()).read();

		assertThat(documents).extracting(BundledDocument::name)
							 .containsExactly("business-accounts", "laptop-buying-guide", "product-care-guide", "returns-policy",
		                                      "shipping",
											  "terms-of-sale", "trade-in-and-recycling", "warranty");
		assertThat(documents.getLast().title()).isEqualTo("Warranty");
		assertThat(documents.getLast().content()).startsWith("# Warranty");
	}
}