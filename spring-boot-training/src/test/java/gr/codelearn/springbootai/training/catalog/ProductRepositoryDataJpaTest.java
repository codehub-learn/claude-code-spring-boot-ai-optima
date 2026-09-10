package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.support.AbstractOraclePersistenceSliceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.6: the {@code @DataJpaTest} slice for repositories. Boots a cut-down context - Hibernate, the {@code catalog} repositories,
 * {@link TestEntityManager}, an {@code @Transactional} wrapper that rolls every method back - and
 * <b>nothing else</b>: no {@code @Service}, no {@code @Component}, no web layer.
 *
 * <p>A slice does not load user {@code @Configuration}, so the production {@code PersistenceConfig} (and its
 * {@code @EnableJpaAuditing}) is absent - without auditing the {@code NOT NULL} {@code created_at} / {@code updated_at} columns blow up on
 * the first flush. The nested {@link AuditingConfig} {@code @TestConfiguration} puts just that annotation back.
 *
 * <p>Flyway still runs (see {@link AbstractOraclePersistenceSliceTest}), so the {@code R__010} baseline (6
 * categories, 4 products each) is present. {@code @Sql} adds a focused fixture on top for one method.
 */
@DataJpaTest
class ProductRepositoryDataJpaTest extends AbstractOraclePersistenceSliceTest {

	@TestConfiguration(proxyBeanMethods = false)
	@EnableJpaAuditing
	static class AuditingConfig {
	}

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ApplicationContext context;

	private Category laptops() {
		return categoryRepository.findByName("Laptops").orElseThrow();
	}

	@Test
	void theSliceLoadsRepositoriesButNotServices() {
		assertThat(context.getBeanNamesForType(ProductRepository.class)).isNotEmpty();
		assertThat(context.getBeanNamesForType(OrderService.class)).isEmpty();
		assertThat(context.getBeanNamesForType(OrderRepository.class)).isNotEmpty();
	}

	@Test
	void derivedQueryResolvesAgainstTheFlywayBaseline() {
		assertThat(productRepository.findBySku("LAP-0001"))
				.get()
				.extracting(Product::getName)
				.isEqualTo("Latitude 14 Ultralight");
		assertThat(productRepository.countByCategory(laptops())).isEqualTo(4);
	}

	@Test
	void testEntityManagerPersistPopulatesAuditingAndVersion() {
		final Product persisted = entityManager.persistFlushFind(
				new Product("TST-DJ-1", "Bench Unit", new BigDecimal("999.00"), laptops()));

		assertThat(persisted.getId()).isNotNull();
		assertThat(persisted.getCreatedAt()).isNotNull();   // @Import(PersistenceConfig) -> auditing active
		assertThat(persisted.getUpdatedAt()).isNotNull();
		assertThat(persisted.getVersion()).isZero();
	}

	@Test
	@Sql("/sql/extra-laptop.sql")
	void sqlFixtureRowsAreVisibleToTheRepository() {
		assertThat(productRepository.findBySku("TST-9001")).isPresent();
		assertThat(productRepository.countByCategory(laptops())).isEqualTo(5); // 4 seeded + 1 from @Sql
	}
}