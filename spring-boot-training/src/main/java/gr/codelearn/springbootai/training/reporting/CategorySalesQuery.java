package gr.codelearn.springbootai.training.reporting;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The sales-per-category report, read with {@link JdbcClient}: hand-written aggregate SQL over the {@code sales_ledger} fact table joined
 * to {@code product} and {@code category}, mapped straight to {@link CategorySales} records. No entity, no persistence context, no
 * Hibernate - the query is the model.
 *
 * <p>{@code JdbcClient} is the fluent wrapper over {@code JdbcTemplate} that Boot auto-configures. The chain is
 * {@code .sql(...)} then a {@code .param(name, value)} per named parameter then a terminal
 * {@code .query(type).list() / .single() / .optional()}. {@code .query(CategorySales.class)} uses a property row mapper: {@code select}
 * aliases are matched to record components by relaxed name ({@code category_name -> categoryName}), so an alias with no matching component
 * is an error.
 *
 * <p>Contrast with the {@code catalog} repositories: there the query language is JPQL / derived method names and
 * the result is an entity graph; here it is SQL and a flat record. Pick this when the read is a report - a projection, an aggregate, a join
 * that no entity models - rather than a domain object you will mutate.
 */
@Component
class CategorySalesQuery {

	private final JdbcClient jdbcClient;

	CategorySalesQuery(final JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	/**
	 * Sales rolled up per category for every ledger row on or after {@code since}, richest category first.
	 */
	List<CategorySales> salesByCategory(final LocalDate since) {
		return jdbcClient.sql("""
							  select c.name             as category_name,
							   sum(sl.quantity)   as units_sold,
							   count(*)           as line_count,
							   sum(sl.net_amount) as gross_revenue
							  from sales_ledger sl
							     join product p on p.id = sl.product_id
							     join category c on c.id = p.category_id
							  where sl.sold_on >= :since
							  group by c.name
							  order by gross_revenue desc, c.name
							  """).param("since", since).query(CategorySales.class).list();
	}

	/**
	 * Total net revenue across all categories since {@code since} - a scalar read: {@code .query(type).single()} with a single-column row
	 * mapper. {@code nvl(..., 0)} keeps it non-null when the window is empty.
	 */
	BigDecimal totalNetRevenue(final LocalDate since) {
		return jdbcClient.sql("select nvl(sum(net_amount), 0) from sales_ledger where sold_on >= :since").param("since", since).query(
				BigDecimal.class).single();
	}

	/**
	 * Sales for one named category since {@code since}, or {@link Optional#empty()} when that category has no ledger rows in the window -
	 * the {@code .optional()} terminal, which tolerates zero rows but still rejects more than one (the {@code having} clause keeps it to a
	 * single group).
	 */
	Optional<CategorySales> salesForCategory(final String categoryName, final LocalDate since) {
		return jdbcClient.sql("""
							  select c.name             as category_name,
							   sum(sl.quantity)   as units_sold,
							   count(*)           as line_count,
							   sum(sl.net_amount) as gross_revenue
							  from sales_ledger sl
							     join product p on p.id = sl.product_id
							     join category c on c.id = p.category_id
							  where sl.sold_on >= :since
							  group by c.name
							  having c.name = :categoryName
							  """).param("since", since).param("categoryName", categoryName).query(CategorySales.class).optional();
	}
}