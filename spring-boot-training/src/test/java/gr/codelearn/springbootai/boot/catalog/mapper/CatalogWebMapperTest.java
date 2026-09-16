package gr.codelearn.springbootai.boot.catalog.mapper;

import gr.codelearn.springbootai.boot.catalog.Category;
import gr.codelearn.springbootai.boot.catalog.Product;
import gr.codelearn.springbootai.boot.catalog.dto.CategoryRequest;
import gr.codelearn.springbootai.boot.catalog.dto.ProductPatchRequest;
import gr.codelearn.springbootai.boot.catalog.dto.ProductRequest;
import gr.codelearn.springbootai.boot.catalog.dto.ProductResponse;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U4.4: plain unit tests for the MapStruct-generated {@code CatalogWebMapperImpl} — no Spring context needed,
 * {@code componentModel = "spring"} still gives the generated class a public no-arg constructor. Covers the two things this unit adds over
 * the U4.1 hand-written version: {@link #toResponse(Product)}'s nested {@code category.name -> categoryName} and the {@code @MappingTarget}
 * update methods' partial-write behaviour ({@code category} always ignored, {@code patchEntityFromRequest} skipping {@code null} fields).
 */
class CatalogWebMapperTest {

	private final CatalogWebMapper mapper = new CatalogWebMapperImpl();

	@Test
	void toResponseFlattensCategoryName() {
		final Category category = new Category("Laptops", "Portable computers");
		final Product product = new Product("LAP-0001", "Latitude 14", new BigDecimal("1299.00"), category);
		product.setDescription("Ultralight");

		final ProductResponse response = mapper.toResponse(product);

		assertThat(response.sku()).isEqualTo("LAP-0001");
		assertThat(response.categoryName()).isEqualTo("Laptops");
	}

	@Test
	void toResponsePageMapsEveryElement() {
		final Category category = new Category("Laptops", null);
		final Product first = new Product("LAP-0001", "Latitude 14", new BigDecimal("1299.00"), category);
		final Product second = new Product("LAP-0002", "Latitude 16", new BigDecimal("1499.00"), category);

		final var page = mapper.toResponsePage(new PageImpl<>(List.of(first, second), PageRequest.of(0, 10), 2));

		assertThat(page.getContent()).extracting(ProductResponse::sku).containsExactly("LAP-0001", "LAP-0002");
	}

	@Test
	void updateEntityFromRequestWritesEveryFieldButLeavesCategoryAndSkuAlone() {
		final Category original = new Category("Laptops", null);
		final Product product = new Product("LAP-0001", "Latitude 14", new BigDecimal("1299.00"), original);
		final ProductRequest request = new ProductRequest("ignored-on-update", "Latitude 16", "Now with more RAM",
														  new BigDecimal("1599.00"), "Desktops");

		mapper.updateEntityFromRequest(request, product);

		assertThat(product.getName()).isEqualTo("Latitude 16");
		assertThat(product.getDescription()).isEqualTo("Now with more RAM");
		assertThat(product.getPrice()).isEqualByComparingTo("1599.00");
		// Immutable business key: no setter, so the mapper never touches it regardless of the request body.
		assertThat(product.getSku()).isEqualTo("LAP-0001");
		// categoryName is a String on the wire but Category an entity on the target - CatalogService resolves and sets it, not the mapper.
		assertThat(product.getCategory()).isSameAs(original);
	}

	@Test
	void patchEntityFromRequestSkipsNullFields() {
		final Category category = new Category("Laptops", null);
		final Product product = new Product("LAP-0001", "Latitude 14", new BigDecimal("1299.00"), category);
		product.setDescription("Ultralight");
		final ProductPatchRequest request = new ProductPatchRequest(null, null, new BigDecimal("1399.00"), null);

		mapper.patchEntityFromRequest(request, product);

		assertThat(product.getName()).isEqualTo("Latitude 14");
		assertThat(product.getDescription()).isEqualTo("Ultralight");
		assertThat(product.getPrice()).isEqualByComparingTo("1399.00");
	}

	@Test
	void updateEntityFromCategoryRequestWritesOnlyDescription() {
		final Category category = new Category("Laptops", "Old blurb");

		mapper.updateEntityFromRequest(new CategoryRequest("ignored-on-update", "New blurb"), category);

		assertThat(category.getDescription()).isEqualTo("New blurb");
		// Business key, no setter: unaffected even though the request carries a (deliberately ignored) name.
		assertThat(category.getName()).isEqualTo("Laptops");
	}
}