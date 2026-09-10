package gr.codelearn.springbootai.training.catalog.mapper;

import gr.codelearn.springbootai.training.catalog.Category;
import gr.codelearn.springbootai.training.catalog.Product;
import gr.codelearn.springbootai.training.catalog.dto.CategoryResponse;
import gr.codelearn.springbootai.training.catalog.dto.ProductResponse;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Hand-written entity → response mapping for the catalog web layer (U4.1). Deliberately trivial and in one place; U4.4 replaces this class
 * with a MapStruct {@code @Mapper(componentModel = "spring")} and the call sites do not change.
 *
 * <p>Called only from {@link CatalogService}, inside its {@code readOnly} transaction, so reading the LAZY {@code product.category} here
 * is safe. A filtered page of products still triggers one category select per row (the classic N+1); it is bounded by the page size and the
 * fix — a fetch graph on the query — is the U3.3 material, left out here to keep the slice small.
 */
@Component
public class CatalogWebMapper {

	public ProductResponse toResponse(final Product product) {
		return new ProductResponse(
				product.getSku(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getCategory().getName());
	}

	public CategoryResponse toResponse(final Category category) {
		return new CategoryResponse(category.getName(), category.getDescription());
	}

	public Page<ProductResponse> toResponsePage(final Page<Product> page) {
		return page.map(this::toResponse);
	}
}
