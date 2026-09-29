package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.config.persistence.CatalogProperties;
import gr.codelearn.springbootai.ai.product.dto.ProductResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Unit 11.1: the capability itself, a live product lookup over spring-boot-training's catalog (read-only, schema from
 * {@link CatalogProperties}). It has two adapters and knows neither: {@link ProductController} for programs (REST) and {@link CatalogTools}
 * for models (tool calling).
 *
 * <p>Search is keyword based: a product matches when any word of the query appears in its name, description or category name
 * (case-insensitive; the SKU counts too), and products matching more words come first. So "laptop" finds the Laptops category, and a
 * model's list of synonyms ("headphones earbuds audio") still finds the headphones. A first version required every word and failed exactly
 * there (see {@link CatalogTools}). A price ceiling is a parameter of its own. The generated {@code GEN-*} demo rows are left out, as in
 * the knowledge base (10.1): hundreds of placeholder products would crowd every result.
 */
@Service
public class ProductService {

	/**
	 * At most this many words of a query are matched; the rest are ignored.
	 */
	static final int MAX_KEYWORDS = 5;

	private final JdbcClient jdbcClient;
	private final CatalogProperties catalog;

	ProductService(final JdbcClient jdbcClient, final CatalogProperties catalog) {
		this.jdbcClient = jdbcClient;
		this.catalog = catalog;
	}

	/**
	 * Products matching at least one keyword of {@code query} (all products when it is blank), costing at most {@code maxPrice} (any price
	 * when {@code null}); those matching the most keywords first, then by name; at most {@code limit}.
	 */
	public List<ProductResponse> search(final @Nullable String query, final @Nullable BigDecimal maxPrice, final int limit) {
		final List<String> keywords = keywords(query);
		final List<String> matches = new ArrayList<>();
		for (int index = 0; index < keywords.size(); index++) {
			matches.add(
					("(lower(p.sku) like :k%1$d or lower(p.name) like :k%1$d or lower(p.description) like :k%1$d or lower(c.name) like " +
					 ":k%1$d)")
							.formatted(index));
		}
		final StringBuilder sql = new StringBuilder(select()).append(" where p.sku not like 'GEN-%'");
		if (maxPrice != null) {
			sql.append(" and p.price <= :maxPrice");
		}
		if (!matches.isEmpty()) {
			sql.append(" and (").append(String.join(" or ", matches)).append(')');
			sql.append(matches.stream().map(match -> "case when " + match + " then 1 else 0 end")
							  .collect(Collectors.joining(" + ", " order by ", " desc, p.name")));
		} else {
			sql.append(" order by p.name");
		}
		sql.append(" fetch first :limit rows only");
		JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString()).param("limit", limit);
		if (maxPrice != null) {
			statement = statement.param("maxPrice", maxPrice);
		}
		for (int index = 0; index < keywords.size(); index++) {
			statement = statement.param("k" + index, "%" + keywords.get(index) + "%");
		}
		return statement.query(ProductService::toResponse).list();
	}

	/**
	 * The product with this SKU (case-insensitive).
	 *
	 * @throws ProductNotFoundException when there is none
	 */
	public ProductResponse get(final String sku) {
		return jdbcClient.sql(select() + " where p.sku = :sku")
						 .param("sku", sku.trim().toUpperCase(Locale.ROOT))
						 .query(ProductService::toResponse)
						 .optional()
						 .orElseThrow(() -> new ProductNotFoundException(sku));
	}

	private String select() {
		return """
			   select p.sku, p.name, p.description, p.price, c.name as category
			   from %1$s.product p
			   join %1$s.category c on c.id = p.category_id""".formatted(catalog.schema());
	}

	static List<String> keywords(final @Nullable String query) {
		if (query == null || query.isBlank()) {
			return List.of();
		}
		// Words are runs of letters, digits and dashes (any script); punctuation, commas and LIKE wildcards separate them.
		return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}-]+"))
					 .filter(word -> !word.isEmpty())
					 .limit(MAX_KEYWORDS)
					 .toList();
	}

	private static ProductResponse toResponse(final ResultSet rs, final int rowNum) throws SQLException {
		final BigDecimal price = rs.getBigDecimal("price").setScale(2, RoundingMode.HALF_UP);
		return new ProductResponse(rs.getString("sku"), rs.getString("name"), rs.getString("category"), price, ProductResponse.CURRENCY,
								   rs.getString("description"));
	}
}