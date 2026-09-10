package gr.codelearn.springbootai.training.catalog;

import java.math.BigDecimal;

/**
 * A <b>DTO (record) projection</b> populated by a JPQL <i>constructor expression</i> —
 * {@code select new gr.codelearn.springbootai.training.catalog.ProductPriceView(p.sku, p.name, c.name, p.price) ...} in
 * {@link ProductRepository#priceView()}. The constructor's parameter list must line up with the {@code select} clause by type and order;
 * the fully-qualified class name is required in the query because JPQL has no imports.
 *
 * <p>Unlike an interface projection this is a plain immutable value the caller owns — handy when the shape is passed further (a service
 * return, a cache key) rather than just read once. {@code categoryName} flattens the {@code product → category} join into one field.
 */
record ProductPriceView(String sku, String name, String categoryName, BigDecimal price) {
}
