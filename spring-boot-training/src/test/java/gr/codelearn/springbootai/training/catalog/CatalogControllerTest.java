package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.catalog.dto.ProductResponse;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

/**
 * U4.1 slice test for {@link CatalogController}: MVC infrastructure only, {@link CatalogService} mocked. Verifies status codes, the
 * {@code Location} header, JSON body shape, the {@code 404} failure path (mapped by {@code @ResponseStatus} on {@link NotFoundException}),
 * and {@code 406} content negotiation.
 *
 * <p>{@code config.web.WebConfig} is a {@link WebMvcConfigurer} and would otherwise be pulled into the slice; it needs
 * {@code AppProperties} which the slice does not bind, so it is filtered out. Its behaviour (CORS, page-size clamp) is covered by
 * {@code CatalogApiIT}.
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

		assertThat(mvc.get().uri("/api/v1/products/{sku}", "LAP-0001"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.categoryName").isEqualTo("Laptops");
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
}