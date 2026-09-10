/**
 * The <b>catalog</b> feature — products and their categories: browse, filter, and CRUD over {@code /api/v1/products} and
 * {@code /api/v1/categories}. Carries units 3 (persistence) and 4 (REST) for this half of the domain; the ordering half lives in the
 * sibling {@code order} feature.
 *
 * <p>Package-by-feature, flat (CLAUDE.md): the controller, service, repositories, entities, {@code ProductSpecifications} and the query
 * projections all sit at this package root. Only three sub-packages:
 * <ul>
 *   <li>{@code dto} — the REST request/response records (public wire contract);</li>
 *   <li>{@code mapper} — hand-written entity&#8596;DTO mapping (MapStruct in U4.4);</li>
 *   <li>{@code internal} — feature-private detail (currently just {@code CatalogDuplicateException}).</li>
 * </ul>
 *
 * <p>{@code CatalogController} is package-private (only Spring and its slice test reference it). The projections
 * ({@code ProductSummary}, {@code ProductPriceView}) are package-private too. Everything a sub-package or another feature must reach —
 * {@code CatalogService}, the repositories, {@code Product}, {@code Category} — is {@code public}: Java package access is not hierarchical,
 * so a sub-package cannot see a package-private sibling of its parent. Cross-feature access goes through {@code CatalogService}
 * (CLAUDE.md); the one exception is {@code order}, which references {@code Product} / {@code ProductRepository} directly to price an order
 * line. Depends on {@code common} only; never on {@code order}.
 */
@NullMarked
package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.NullMarked;
