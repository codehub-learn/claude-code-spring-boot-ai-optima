package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.config.persistence.CatalogProperties;

import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unit 10.1: turns spring-boot-training's catalog rows into {@link Document}s, one per product. Read straight from that module's schema
 * (read-only grant), because the agreed corpus is the catalog itself. The generated {@code GEN-*} demo rows are left out: 500 products with
 * the same placeholder text would crowd every search.
 *
 * <p>The text is what gets embedded <em>and</em> what the model sees ({@code QuestionAnswerAdvisor} passes the text only), so it starts
 * with the citation tag {@code [catalog:SKU]} and carries every fact worth asking about: name, category, price, description. The metadata
 * is for the API's source list and for filters. Prices always show two decimals: Oracle hands {@code 2199.00} back as {@code 2199}. The id
 * is derived from the SKU, so ingesting again overwrites the same row.
 */
@Component
class CatalogDocumentReader {

	static final String SOURCE = "catalog";

	/**
	 * Unit 10.2: the {@code chunking} metadata of a catalog row. A product is one short sentence and is never split, so every chunking
	 * strategy searches the same catalog rows.
	 */
	static final String NOT_CHUNKED = "none";

	private final JdbcClient jdbcClient;
	private final CatalogProperties catalog;

	CatalogDocumentReader(final JdbcClient jdbcClient, final CatalogProperties catalog) {
		this.jdbcClient = jdbcClient;
		this.catalog = catalog;
	}

	List<Document> read() {
		final String schema = catalog.schema();
		return jdbcClient.sql("""
							  select p.sku, p.name, p.description, p.price, c.name as category
							  from %s.product p
							  join %s.category c on c.id = p.category_id
							  where p.sku not like 'GEN-%%'
							  order by p.sku""".formatted(schema, schema))
						 .query((rs, rowNum) -> toDocument(rs.getString("sku"), rs.getString("name"), rs.getString("description"),
														   rs.getBigDecimal("price"), rs.getString("category")))
						 .list();
	}

	static Document toDocument(final String sku, final String name, final String description, final BigDecimal price,
							   final String category) {
		final String text = "[%s:%s] %s. Category: %s. Price: %s EUR. %s".formatted(SOURCE, sku, name, category,
																					price.setScale(2, RoundingMode.HALF_UP).toPlainString(),
																					description);
		return Document.builder()
					   .id(idFor(SOURCE, sku))
					   .text(text)
					   .metadata(Map.of("source", SOURCE, "ref", sku, "title", name, "category", category, "chunking", NOT_CHUNKED))
					   .build();
	}

	/**
	 * A stable id per source item (a name-based UUID), so a re-ingestion replaces the row instead of adding a copy.
	 */
	static String idFor(final String source, final String ref) {
		return UUID.nameUUIDFromBytes((source + ":" + ref).getBytes(StandardCharsets.UTF_8)).toString();
	}
}