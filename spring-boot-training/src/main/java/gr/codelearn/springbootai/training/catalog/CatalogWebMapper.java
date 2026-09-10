package gr.codelearn.springbootai.training.catalog;

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
class CatalogWebMapper {

	ProductResponse toResponse(final Product product) {
		return new ProductResponse(
				product.getSku(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getCategory().getName());
	}

	CategoryResponse toResponse(final Category category) {
		return new CategoryResponse(category.getName(), category.getDescription());
	}

	Page<ProductResponse> toResponsePage(final Page<Product> page) {
		return page.map(this::toResponse);
	}
}