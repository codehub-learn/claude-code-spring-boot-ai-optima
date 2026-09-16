/**
 * Cross-cutting web-layer configuration that is not tied to a single feature: CORS, the config-driven {@code Pageable} resolver, and the
 * top-level OpenAPI document metadata. Feature controllers live in their own feature packages ({@code catalog}), not here.
 */
@NullMarked
package gr.codelearn.springbootai.boot.config.web;

import org.jspecify.annotations.NullMarked;