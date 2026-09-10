package gr.codelearn.springbootai.training.catalog;

import java.math.BigDecimal;

/**
 * A <b>closed interface projection</b> for {@link Product}: Spring Data implements this interface with a proxy backed only by the columns
 * its getters name, so the query selects {@code sku}, {@code name}, {@code price} and nothing else — no entity is built, no association is
 * touched. "Closed" means every method maps straight to a property path; an <i>open</i> projection (a getter with {@code @Value("#{...}")})
 * would force Spring Data to load the whole entity first.
 *
 * <p>Returned from {@link ProductRepository#findByCategoryOrderByPriceAsc(Category)} and, dynamically, from
 * {@link ProductRepository#findBySkuStartingWith(String, Class)}.
 */
interface ProductSummary {

	String getSku();

	String getName();

	BigDecimal getPrice();
}
