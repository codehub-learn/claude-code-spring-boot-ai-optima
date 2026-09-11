package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.catalog.dto.CategoryResponse;
import gr.codelearn.springbootai.training.catalog.dto.ProductResponse;
import gr.codelearn.springbootai.training.catalog.internal.CatalogDuplicateException;
import gr.codelearn.springbootai.training.common.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

/**
 * U4.1/U4.2 slice test for {@link CatalogController}: MVC infrastructure only, {@link CatalogService} mocked. Verifies status codes, the
 * {@code Location} header, JSON body shape, the {@code 404} failure path (mapped by {@code config.web.ApiExceptionHandler} from
 * {@link NotFoundException}), {@code 406} content negotiation, and (U4.2) the two Bean Validation failure paths — a {@code @Valid} body and
 * a {@code @Validated} path variable.
 *
 * <p>{@code config.web.WebConfig} is a {@link WebMvcConfigurer} and would otherwise be pulled into the slice; it needs
 * {@code AppProperties} which the slice does not bind, so it is filtered out. Its behaviour (CORS, page-size clamp) is covered by
 * {@code CatalogApiIT}. {@code config.web.ApiExceptionHandler} carries no such dependency and is picked up normally.
 */
@WebMvcTest(controllers = CatalogController.class,
			excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfigurer.class))
class CatalogControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private CatalogService catalogService;

	private static final ProductResponse LAPTOP =
			new ProductResponse("LAP-0001", "Latitude 14 Ultralight", "14\" ultrabook", new BigDecimal("1299.00"), "Laptops");

	@Test
	void listProductsReturnsAPagedBody() {
		given(catalogService.findProducts(any(), any())).willReturn(new PageImpl<>(List.of(LAPTOP)));

		assertThat(mvc.get().uri("/api/v1/products").param("size", "5"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.content[0].sku").isEqualTo("LAP-0001");
	}

	@Test
	void getProductBySkuReturns200() {
		given(catalogService.getProduct("LAP-0001")).willReturn(LAPTOP);

		// ProductResponse is @JsonNaming(SnakeCaseStrategy.class) (U4.3): categoryName -> category_name on the wire.
		assertThat(mvc.get().uri("/api/v1/products/{sku}", "LAP-0001"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.category_name").isEqualTo("Laptops");
	}

	@Test
	void getUnknownProductReturns404() {
		given(catalogService.getProduct("NOPE")).willThrow(new NotFoundException("No product with sku NOPE"));

		assertThat(mvc.get().uri("/api/v1/products/{sku}", "NOPE")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void createProductReturns201WithLocation() {
		given(catalogService.createProduct(any())).willReturn(LAPTOP);

		assertThat(mvc.post().uri("/api/v1/products")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"sku":"LAP-0001","name":"Latitude 14 Ultralight","description":null,"price":1299.00,"categoryName":"Laptops"}
							   """))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/v1/products/LAP-0001");
	}

	@Test
	void createDuplicateSkuReturns409ViaTheCentralAdvice() {
		given(catalogService.createProduct(any())).willThrow(new CatalogDuplicateException("A product with sku LAP-0001 already exists"));

		assertThat(mvc.post().uri("/api/v1/products")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"sku":"LAP-0001","name":"Latitude 14 Ultralight","description":null,"price":1299.00,"categoryName":"Laptops"}
							   """))
				.hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void patchProductAppliesOnlyTheSuppliedFields() {
		given(catalogService.patchProduct(eq("LAP-0001"), any())).willReturn(
				new ProductResponse("LAP-0001", "Latitude 14 Ultralight (refurb)", "14\" ultrabook", new BigDecimal("1199.00"), "Laptops"
				));

		// Only "name" is on the wire; patchProduct is mocked, so this proves the controller forwards a single-field PATCH body and
		// returns 200 with whatever the service hands back — the "which fields actually changed" logic is CatalogServiceTest territory.
		assertThat(mvc.patch().uri("/api/v1/products/{sku}", "LAP-0001")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"name":"Latitude 14 Ultralight (refurb)"}"""))
				.hasStatusOk()
				.bodyJson().extractingPath("$.name").isEqualTo("Latitude 14 Ultralight (refurb)");
	}

	@Test
	void patchProductWithANonPositivePriceReturns400() {
		assertThat(mvc.patch().uri("/api/v1/products/{sku}", "LAP-0001")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"price":0}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.errors[*].field").asArray().contains("price");
	}

	@Test
	void patchUnknownProductReturns404() {
		given(catalogService.patchProduct(eq("NOPE"), any())).willThrow(new NotFoundException("No product with sku NOPE"));

		assertThat(mvc.patch().uri("/api/v1/products/{sku}", "NOPE")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"name":"Renamed"}"""))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteProductReturns204() {
		willDoNothing().given(catalogService).deleteProduct("LAP-0001");

		assertThat(mvc.delete().uri("/api/v1/products/{sku}", "LAP-0001")).hasStatus(HttpStatus.NO_CONTENT);
		verify(catalogService).deleteProduct("LAP-0001");
	}

	@Test
	void unsupportedAcceptHeaderReturns406() {
		assertThat(mvc.get().uri("/api/v1/products/{sku}", "LAP-0001").accept(MediaType.APPLICATION_XML))
				.hasStatus(HttpStatus.NOT_ACCEPTABLE);
	}

	@Test
	void invalidCreateBodyReturns400WithFieldErrors() {
		// blank name + non-positive price: two @Valid failures on ProductRequest (OnCreate group also requires sku, satisfied here).
		assertThat(mvc.post().uri("/api/v1/products")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"sku":"LAP-0002","name":"","description":null,"price":0,"categoryName":"Laptops"}
							   """))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.contains("name", "price");
	}

	@Test
	void blankPathVariableReturns400ViaHandlerMethodValidation() {
		assertThat(mvc.get().uri("/api/v1/products/{sku}", " "))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void invalidReplaceBodyReturns400WithFieldErrorsNotTheParameterName() {
		// replaceProduct combines a @NotBlank @PathVariable with a @Valid @RequestBody, so this failure is reported through
		// HandlerMethodValidationException, not MethodArgumentNotValidException (U4.3 gotcha) — proves the advice still resolves
		// "price", not the bare method-parameter name "request".
		assertThat(mvc.put().uri("/api/v1/products/{sku}", "LAP-0001")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"sku":"LAP-0001","name":"Latitude 14 Ultralight","description":null,"price":0,"categoryName":"Laptops"}
							   """))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.contains("price");
	}

	@Test
	void invalidCreateCategoryBodyReturns400WithFieldErrors() {
		// blank name: the one @NotBlank(groups = OnCreate.class) constraint on CategoryRequest.
		assertThat(mvc.post().uri("/api/v1/categories")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"name":"","description":null}
							   """))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.contains("name");
	}

	@Test
	void replaceCategoryReturns200() {
		given(catalogService.replaceCategory(eq("Laptops"), any())).willReturn(new CategoryResponse("Laptops", "updated"));

		assertThat(mvc.put().uri("/api/v1/categories/{name}", "Laptops")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"name":"Laptops","description":"updated"}
							   """))
				.hasStatusOk()
				.bodyJson().extractingPath("$.description").isEqualTo("updated");
	}

	@Test
	void replaceUnknownCategoryReturns404() {
		given(catalogService.replaceCategory(any(), any())).willThrow(new NotFoundException("No category named NOPE"));

		assertThat(mvc.put().uri("/api/v1/categories/{name}", "NOPE")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"name":"NOPE","description":null}
							   """))
				.hasStatus(HttpStatus.NOT_FOUND);
	}
}