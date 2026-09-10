package gr.codelearn.springbootai.training.reporting;

import gr.codelearn.springbootai.training.support.AbstractOraclePersistenceSliceTest;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.6: the {@code @JdbcTest} slice for the read-model. {@link CategorySalesQuery} is a hand-written {@link JdbcClient} query - no entity,
 * no Hibernate - so the JDBC slice fits it exactly: {@code DataSource}, {@code JdbcClient}, a {@code DataSourceTransactionManager} and an
 * {@code @Transactional} rollback wrapper, and nothing JPA.
 *
 * <p>The query object is not a repository, so it is not slice-scanned: {@code @Import(CategorySalesQuery.class)}
 * brings it in. Flyway still runs (see {@link AbstractOraclePersistenceSliceTest}), so the {@code R__030} ledger seed is present;
 * {@code @Sql} layers a March-dated fixture on one method.
 */
@JdbcTest
@Import(CategorySalesQuery.class)
class CategorySalesQueryJdbcTest extends AbstractOraclePersistenceSliceTest {

	private static final LocalDate ALL = LocalDate.of(2026, 1, 1);
	private static final LocalDate MARCH = LocalDate.of(2026, 3, 1);

	@Autowired
	private CategorySalesQuery categorySalesQuery;

	@Autowired
	private ApplicationContext context;

	@Test
	void theSliceExposesJdbcClientButNoJpa() {
		assertThat(context.getBeanNamesForType(JdbcClient.class)).isNotEmpty();
		assertThat(context.getBeanNamesForType(EntityManagerFactory.class)).isEmpty();
	}

	@Test
	void aggregatesTheFlywayLedgerSeed() {
		assertThat(categorySalesQuery.salesByCategory(ALL)).anySatisfy(row -> {
			assertThat(row.categoryName()).isEqualTo("Laptops");
			assertThat(row.unitsSold()).isEqualTo(3);
			assertThat(row.grossRevenue()).isEqualByComparingTo("5697.00");
		});
	}

	@Test
	@Sql("/sql/extra-storage-ledger.sql")
	void sqlFixtureRowsFeedIntoTheAggregate() {
		final CategorySales storage = categorySalesQuery.salesForCategory("Storage", MARCH).orElseThrow();

		assertThat(storage.lineCount()).isEqualTo(2);
		assertThat(storage.unitsSold()).isEqualTo(10);
		assertThat(storage.grossRevenue()).isEqualByComparingTo("2500.00");
	}
}