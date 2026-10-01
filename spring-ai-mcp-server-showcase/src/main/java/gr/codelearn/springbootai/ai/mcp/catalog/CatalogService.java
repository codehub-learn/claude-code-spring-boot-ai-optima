package gr.codelearn.springbootai.ai.mcp.catalog;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only queries over the catalog tables of spring-boot-training. Every value reaches the database as a bind variable; the one thing
 * written into the SQL is the schema name, which {@link CatalogProperties} restricts to a plain identifier. The generated {@code GEN-*}
 * demo rows are left out of every answer: hundreds of identical placeholders would crowd the real products.
 */
@Service
class CatalogService {

	private static final String DEMO_ROWS = "p.sku not like 'GEN-%'";

	private final JdbcClient jdbcClient;
	private final CatalogProperties properties;

	CatalogService(final JdbcClient jdbcClient, final CatalogProperties properties) {
		this.jdbcClient = jdbcClient;
		this.properties = properties;
	}

	List<ProductSummary> search(final @Nullable String query, final @Nullable String category, final @Nullable BigDecimal maxPrice,
								final @Nullable Integer limit) {
		if (maxPrice != null && maxPrice.signum() < 0) {
			throw new IllegalArgumentException("maxPrice must not be negative, got " + maxPrice.toPlainString() + ".");
		}
		final var sql = new StringBuilder("select p.sku, p.name, c.name as category, p.price from %s.product p join %s.category c "
												  .formatted(properties.schema(), properties.schema()))
				.append("on c.id = p.category_id where ")
				.append(DEMO_ROWS);
		final Map<String, Object> params = new LinkedHashMap<>();
		if (query != null && !query.isBlank()) {
			sql.append(" and (lower(p.name) like :query escape '\\' or lower(p.description) like :query escape '\\')");
			params.put("query", likePattern(query));
		}
		if (category != null && !category.isBlank()) {
			sql.append(" and lower(c.name) = :category");
			params.put("category", category.strip().toLowerCase(Locale.ROOT));
		}
		if (maxPrice != null) {
			sql.append(" and p.price <= :maxPrice");
			params.put("maxPrice", maxPrice);
		}
		sql.append(" order by p.name, p.sku fetch first :limit rows only");
		params.put("limit", clamp(limit));
		return jdbcClient.sql(sql.toString())
						 .params(params)
						 .query((rs, rowNum) -> new ProductSummary(rs.getString("sku"), rs.getString("name"), rs.getString("category"),
																   rs.getBigDecimal("price")))
						 .list();
	}

	/**
	 * One product by SKU for a tool or resource: a blank or unknown SKU is an {@link IllegalArgumentException} whose message tells the
	 * caller (a model) what to do next.
	 */
	ProductDetail get(final @Nullable String sku) {
		if (sku == null || sku.isBlank()) {
			throw new IllegalArgumentException("sku must not be blank. Find SKUs with searchProducts.");
		}
		return find(sku).orElseThrow(() -> new IllegalArgumentException("No product with SKU '" + sku.strip()
																		+ "'. Find SKUs with searchProducts."));
	}

	Optional<ProductDetail> find(final String sku) {
		return jdbcClient.sql("select p.sku, p.name, c.name as category, p.price, p.description from %s.product p join %s.category c "
									  .formatted(properties.schema(), properties.schema())
							  + "on c.id = p.category_id where upper(p.sku) = :sku and " + DEMO_ROWS)
						 .param("sku", sku.strip().toUpperCase(Locale.ROOT))
						 .query((rs, rowNum) -> new ProductDetail(rs.getString("sku"), rs.getString("name"), rs.getString("category"),
																  rs.getBigDecimal("price"), rs.getString("description")))
						 .optional();
	}

	List<CategorySummary> categories() {
		return jdbcClient.sql("select c.name, count(p.id) as product_count from %s.category c left join %s.product p "
									  .formatted(properties.schema(), properties.schema())
							  + "on p.category_id = c.id and " + DEMO_ROWS + " group by c.name order by c.name")
						 .query((rs, rowNum) -> new CategorySummary(rs.getString("name"), rs.getInt("product_count")))
						 .list();
	}

	/**
	 * A caller's limit, kept between 1 and the configured maximum; none means the configured default.
	 */
	int clamp(final @Nullable Integer limit) {
		if (limit == null) {
			return properties.defaultLimit();
		}
		return Math.max(1, Math.min(limit, properties.maxLimit()));
	}

	/**
	 * A case-insensitive "contains" pattern for {@code like ... escape '\'}: the caller's own {@code %}, {@code _} and {@code \} match
	 * themselves instead of acting as wildcards.
	 */
	static String likePattern(final String text) {
		final String escaped = text.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}
}