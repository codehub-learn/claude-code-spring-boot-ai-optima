/**
 * Unit 11.1: one capability, a live product lookup over the catalog, exposed twice. {@code ProductController} is the plain REST API: the
 * caller decides what to call and with which arguments. {@code CatalogTools} offers the same lookups to a model as tools: the model reads
 * their descriptions, decides whether and how to call them, and gets the results back as text. Both go through {@code ProductService}.
 */
@NullMarked
package gr.codelearn.springbootai.ai.product;

import org.jspecify.annotations.NullMarked;