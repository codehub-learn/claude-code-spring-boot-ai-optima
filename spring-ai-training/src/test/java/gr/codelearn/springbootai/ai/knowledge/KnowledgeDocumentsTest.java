package gr.codelearn.springbootai.ai.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 10.1: the documents ingestion builds, without a database or a model. What matters is the text: it is both what is embedded and all
 * the model sees of a document, so the citation tag and the facts must be in it.
 */
class KnowledgeDocumentsTest {

	@Test
	void aProductBecomesOneTaggedDocument() {
		final Document document = CatalogDocumentReader.toDocument("LAP-0002", "Latitude 16 Workstation",
																   "16\" mobile workstation, 32 GB RAM, 1 TB SSD, discrete GPU",
																   new BigDecimal("2199.00"), "Laptops");

		assertThat(document.getText()).isEqualTo("[catalog:LAP-0002] Latitude 16 Workstation. Category: Laptops. Price: 2199.00 EUR. "
												 + "16\" mobile workstation, 32 GB RAM, 1 TB SSD, discrete GPU");
		assertThat(document.getMetadata()).containsEntry("source", "catalog").containsEntry("ref", "LAP-0002");
	}

	@Test
	void idsAreStableSoReIngestionReplacesRows() {
		assertThat(CatalogDocumentReader.idFor("catalog", "LAP-0002")).isEqualTo(CatalogDocumentReader.idFor("catalog", "LAP-0002"))
																	  .isNotEqualTo(CatalogDocumentReader.idFor("doc", "LAP-0002"))
																	  .hasSize(36);
	}

	@Test
	void eachBundledFileBecomesOneTaggedDocument() {
		final List<Document> documents = new BundledDocumentReader(new DefaultResourceLoader()).read();

		assertThat(documents).extracting(document -> document.getMetadata().get("ref"))
							 .containsExactly("laptop-buying-guide", "returns-policy", "shipping", "warranty");
		assertThat(documents.getLast().getText()).startsWith("[doc:warranty] # Warranty");
		assertThat(documents.getLast().getMetadata()).containsEntry("title", "Warranty").containsEntry("source", "doc");
	}
}