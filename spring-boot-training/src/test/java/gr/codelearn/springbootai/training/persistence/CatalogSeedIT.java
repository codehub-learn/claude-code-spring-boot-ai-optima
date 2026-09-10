package gr.codelearn.springbootai.training.persistence;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.1 follow-up: {@code R__010_seed_catalog.sql} is a repeatable Flyway migration in {@code db/migration}, so the {@code test} profile
 * applies it right after the DDL (same as {@link DatasourceMigrationIT} relies on for {@code V1}). This proves the baseline fixture every
 * integration test starts from is present, and that the demo-only bulk data ({@code db/demo}, {@code -Pdemo}) never leaks into the test
 * database.
 */
class CatalogSeedIT extends AbstractOracleIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void baselineCategoriesAndProductsAreSeeded() {
		final Long categories = jdbcClient.sql("select count(*) from category").query(Long.class).single();
		final Long products = jdbcClient.sql("select count(*) from product").query(Long.class).single();

		assertThat(categories).isGreaterThanOrEqualTo(6L);
		assertThat(products).isGreaterThanOrEqualTo(24L);
	}

	@Test
	void everySeededProductBelongsToACategory() {
		final Long orphans = jdbcClient.sql("""
											select count(*) from product p
											where not exists (select 1 from category c where c.id = p.category_id)
											""")
									   .query(Long.class)
									   .single();

		assertThat(orphans).isZero();
	}

	@Test
	void demoBulkDataIsNotLoadedInTests() {
		final Long generated = jdbcClient.sql("select count(*) from product where sku like 'GEN-%'")
										 .query(Long.class)
										 .single();

		assertThat(generated).isZero();
	}
}