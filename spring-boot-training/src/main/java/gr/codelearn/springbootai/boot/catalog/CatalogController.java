package gr.codelearn.springbootai.boot.catalog;

import gr.codelearn.springbootai.boot.catalog.dto.CategoryRequest;
import gr.codelearn.springbootai.boot.catalog.dto.CategoryResponse;
import gr.codelearn.springbootai.boot.catalog.dto.ProductFilter;
import gr.codelearn.springbootai.boot.catalog.dto.ProductPatchRequest;
import gr.codelearn.springbootai.boot.catalog.dto.ProductRequest;
import gr.codelearn.springbootai.boot.catalog.dto.ProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for the product and category resources (U4.1). {@code /api/v1} prefix, plural kebab-case nouns, HTTP verbs for actions
 * (CLAUDE.md *Naming*). The controller only binds input, calls {@link CatalogService}, and shapes the {@link ResponseEntity} — no
 * repository, no entity, no {@code @Transactional}.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><th>Verb + path</th><th>Success</th></tr>
 *   <tr><td>{@code GET /products}</td><td>{@code 200} paged {@link ProductResponse}, filtered by {@link ProductFilter}</td></tr>
 *   <tr><td>{@code GET /products/{sku}}</td><td>{@code 200} / {@code 404}</td></tr>
 *   <tr><td>{@code POST /products}</td><td>{@code 201} + {@code Location}</td></tr>
 *   <tr><td>{@code PUT /products/{sku}}</td><td>{@code 200}</td></tr>
 *   <tr><td>{@code PATCH /products/{sku}}</td><td>{@code 200}, partial update (U4.3 {@link ProductPatchRequest})</td></tr>
 *   <tr><td>{@code DELETE /products/{sku}}</td><td>{@code 204}</td></tr>
 *   <tr><td>{@code GET /categories}</td><td>{@code 200} {@link CategoryResponse} list</td></tr>
 *   <tr><td>{@code POST /categories}</td><td>{@code 201} + {@code Location}</td></tr>
 *   <tr><td>{@code PUT /categories/{name}}</td><td>{@code 200}</td></tr>
 * </table>
 *
 * <p>The class-level {@code produces = application/json} means an {@code Accept} header the app cannot satisfy (e.g. {@code
 * application/xml}, with no XML message converter on the classpath) is answered {@code 406 Not Acceptable} — MVC content negotiation, not
 * controller code.
 *
 * <p><b>Validation (U4.2).</b> A constraint directly on a discrete {@code @PathVariable} (e.g. {@code @NotBlank}) is validated by Spring
 * MVC itself since Framework 6.1 — no class-level {@code @Validated} needed; a violation throws {@code HandlerMethodValidationException}.
 * <b>Do not</b> add class-level {@code @Validated} here: that opts the bean into the older AOP method-validation interceptor instead,
 * which throws a plain {@code jakarta.validation.ConstraintViolationException} and preempts the newer mechanism entirely (confirmed by
 * running it — the interceptor wraps the call before MVC's own resolution matters). A {@code @RequestBody}/{@code @Valid} failure throws
 * {@code MethodArgumentNotValidException}, unaffected either way. Both land in {@code config.web.ApiExceptionHandler}.
 */
@Tag(name = "catalog")
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
class CatalogController {

	private final CatalogService catalogService;

	CatalogController(final CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/products")
	@Operation(summary = "List products (paged, filtered)")
	PagedModel<ProductResponse> listProducts(@ParameterObject @Valid final ProductFilter filter,
											 @ParameterObject @PageableDefault(sort = "sku") final Pageable pageable) {
		return new PagedModel<>(catalogService.findProducts(filter, pageable));
	}

	@GetMapping("/products/{sku}")
	@Operation(summary = "Get one product by SKU")
	@ApiResponse(responseCode = "404", description = "No product with that SKU")
	ProductResponse getProduct(@PathVariable @NotBlank final String sku) {
		return catalogService.getProduct(sku);
	}

	@PostMapping("/products")
	@Operation(summary = "Create a product")
	@ApiResponse(responseCode = "201", description = "Created; URI in the Location header")
	@ApiResponse(responseCode = "409", description = "SKU already exists")
	ResponseEntity<ProductResponse> createProduct(
			@RequestBody @Validated(ProductRequest.OnCreate.class) final ProductRequest request) {
		final ProductResponse created = catalogService.createProduct(request);
		final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
														.path("/{sku}")
														.buildAndExpand(created.sku())
														.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/products/{sku}")
	@Operation(summary = "Replace a product")
	@ApiResponse(responseCode = "404", description = "No product with that SKU")
	ProductResponse replaceProduct(@PathVariable @NotBlank final String sku,
								   @RequestBody @Validated(ProductRequest.OnUpdate.class) final ProductRequest request) {
		return catalogService.replaceProduct(sku, request);
	}

	@PatchMapping("/products/{sku}")
	@Operation(summary = "Partially update a product")
	@ApiResponse(responseCode = "404", description = "No product with that SKU")
	ProductResponse patchProduct(@PathVariable @NotBlank final String sku, @RequestBody @Valid final ProductPatchRequest request) {
		return catalogService.patchProduct(sku, request);
	}

	@DeleteMapping("/products/{sku}")
	@Operation(summary = "Delete a product")
	@ApiResponse(responseCode = "204", description = "Deleted")
	@ApiResponse(responseCode = "404", description = "No product with that SKU")
	ResponseEntity<Void> deleteProduct(@PathVariable @NotBlank final String sku) {
		catalogService.deleteProduct(sku);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/categories")
	@Operation(summary = "List all categories")
	List<CategoryResponse> listCategories() {
		return catalogService.listCategories();
	}

	@GetMapping("/categories/{name}")
	@Operation(summary = "Get one category by name")
	@ApiResponse(responseCode = "404", description = "No category with that name")
	CategoryResponse getCategory(@PathVariable @NotBlank final String name) {
		return catalogService.getCategory(name);
	}

	@PostMapping("/categories")
	@Operation(summary = "Create a category")
	@ApiResponse(responseCode = "201", description = "Created; URI in the Location header")
	@ApiResponse(responseCode = "409", description = "Category name already exists")
	ResponseEntity<CategoryResponse> createCategory(
			@RequestBody @Validated(CategoryRequest.OnCreate.class) final CategoryRequest request) {
		final CategoryResponse created = catalogService.createCategory(request);
		final URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
														.path("/api/v1/categories/{name}")
														.buildAndExpand(created.name())
														.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/categories/{name}")
	@Operation(summary = "Replace a category")
	@ApiResponse(responseCode = "404", description = "No category with that name")
	CategoryResponse replaceCategory(@PathVariable @NotBlank final String name,
									 @RequestBody @Validated(CategoryRequest.OnUpdate.class) final CategoryRequest request) {
		return catalogService.replaceCategory(name, request);
	}
}