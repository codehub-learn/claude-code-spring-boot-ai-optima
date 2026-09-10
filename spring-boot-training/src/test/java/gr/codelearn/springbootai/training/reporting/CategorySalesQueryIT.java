package gr.codelearn.springbootai.training.reporting;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.5: {@link CategorySalesQuery} against the {@code R__030} ledger seed (12 rows: two per category, one dated 2026-01-15 and one
 * 2026-02-20). The per-category totals asserted here are the sums the seed is built to produce - see {@code R__030_seed_reporting.sql},
 * which also feeds the {@code baseline} snapshot lines.
 *
 * <p>{@code @Transactional} so nothing this test does is committed; it only reads.
 */
@Transactional
class CategorySalesQueryIT extends AbstractOracleIntegrationTest {

	private static final LocalDate ALL = LocalDate.of(2026, 1, 1);
	private static final LocalDate FROM_FEBRUARY = LocalDate.of(2026, 2, 1);

	@Autowired
	private CategorySalesQuery categorySalesQuery;

	@Test
	void rollsUpEveryCategorySinceTheWindowStart() {
		final List<CategorySales> sales = categorySalesQuery.salesByCategory(ALL);

		assertThat(sales).anySatisfy(row -> {
			assertThat(row.categoryName()).isEqualTo("Laptops");
			assertThat(row.unitsSold()).isEqualTo(3);
			assertThat(row.lineCount()).isEqualTo(2);
			assertThat(row.grossRevenue()).isEqualByComparingTo("5697.00");
		});
		assertThat(sales).anySatisfy(row -> {
			assertThat(row.categoryName()).isEqualTo("Storage");
			assertThat(row.unitsSold()).isEqualTo(7);
			assertThat(row.grossRevenue()).isEqualByComparingTo("833.00");
		});
	}

	@Test
	void ordersRichestCategoryFirst() {
		final List<CategorySales> sales = categorySalesQuery.salesByCategory(ALL);

		assertThat(sales).extracting(CategorySales::categoryName)
						 .containsSubsequence("Laptops", "Smartphones", "Audio", "Monitors", "Storage", "Accessories");
	}

	@Test
	void theSinceFilterDropsRowsBeforeTheWindow() {
		final List<CategorySales> sales = categorySalesQuery.salesByCategory(FROM_FEBRUARY);

		// Only the 2026-02-20 ledger row per category survives.
		assertThat(sales).anySatisfy(row -> {
			assertThat(row.categoryName()).isEqualTo("Laptops");
			assertThat(row.unitsSold()).isEqualTo(2);
			assertThat(row.lineCount()).isEqualTo(1);
			assertThat(row.grossRevenue()).isEqualByComparingTo("4398.00");
		});
	}

	@Test
	void totalNetRevenueIsAScalarRead() {
		assertThat(categorySalesQuery.totalNetRevenue(ALL)).isGreaterThanOrEqualTo(new BigDecimal("13083.00"));
		assertThat(categorySalesQuery.totalNetRevenue(LocalDate.of(2999, 1, 1))).isEqualByComparingTo("0");
	}

	@Test
	void salesForCategoryIsPresentOrEmpty() {
		assertThat(categorySalesQuery.salesForCategory("Storage", ALL))
				.hasValueSatisfying(row -> assertThat(row.unitsSold()).isEqualTo(7));

		assertThat(categorySalesQuery.salesForCategory("No Such Category", ALL)).isEmpty();
		assertThat(categorySalesQuery.salesForCategory("Laptops", LocalDate.of(2027, 1, 1))).isEmpty();
	}
}