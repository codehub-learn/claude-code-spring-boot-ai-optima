package gr.codelearn.springbootai.training.reporting;

import java.math.BigDecimal;

/**
 * One row of the sales-per-category report: the aggregated sales for a single {@code category} over a date window. A plain immutable value
 * the caller owns - it is not an entity and has no repository.
 *
 * <p>Populated by {@link CategorySalesQuery} straight from a {@code JdbcClient} query: the {@code select}
 * aliases ({@code category_name}, {@code units_sold}, {@code line_count}, {@code gross_revenue}) map onto these record components by
 * relaxed name matching, so every alias must correspond to a component or the row mapper fails.
 *
 * @param categoryName the {@code category.name} the sales roll up to
 * @param unitsSold    total {@code quantity} across the window
 * @param lineCount    number of {@code sales_ledger} rows contributing
 * @param grossRevenue total {@code net_amount} across the window
 */
record CategorySales(String categoryName, long unitsSold, long lineCount, BigDecimal grossRevenue) {
}