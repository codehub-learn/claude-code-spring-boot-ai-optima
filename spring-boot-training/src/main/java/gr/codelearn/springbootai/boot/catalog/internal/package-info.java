/**
 * Feature-private building blocks for the catalog use cases. Currently just {@code CatalogDuplicateException} — the "business key already
 * taken" failure ({@code @ResponseStatus(CONFLICT)} stopgap until the central {@code ProblemDetail} advice in U4.2).
 *
 * <p>{@code public} so the {@code catalog} root and {@code catalog.mapper} can use it, but no package outside {@code catalog} imports it.
 */
@NullMarked
package gr.codelearn.springbootai.boot.catalog.internal;

import org.jspecify.annotations.NullMarked;