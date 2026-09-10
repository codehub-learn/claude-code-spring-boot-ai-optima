/**
 * REST request and response records for the catalog API — the public wire contract for {@code /api/v1/products} and
 * {@code /api/v1/categories}. No entity leaks here (CLAUDE.md); {@code catalog.mapper} converts between these and the entities. Jackson-3
 * shaping and {@code @JsonView} arrive in U4.3.
 */
@NullMarked
package gr.codelearn.springbootai.training.catalog.dto;

import org.jspecify.annotations.NullMarked;