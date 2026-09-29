package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.product.dto.ProductResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit 11.1 slice test for {@link ProductController}: the REST side of the product lookup, with its status codes. The service is mocked.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

	private static final ProductResponse LAPTOP =
			new ProductResponse("LAP-0003", "Flex 13 Convertible", "Laptops", new BigDecimal("1099.00"), "EUR", null);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void searchPassesTheQueryTheCeilingAndTheLimit() {
		given(productService.search("laptop", new BigDecimal("1500"), 3)).willReturn(List.of(LAPTOP));

		assertThat(mvc.get().uri("/api/v1/products?query=laptop&max-price=1500&limit=3")).hasStatusOk()
																						 .bodyJson()
																						 .isLenientlyEqualTo("""
																											 [{"sku": "LAP-0003", "price": 1099.00, "currency": "EUR"}]""");
	}

	@Test
	void searchDefaultsToTenResultsWithoutFilters() {
		given(productService.search(null, null, 10)).willReturn(List.of());

		assertThat(mvc.get().uri("/api/v1/products")).hasStatusOk().bodyJson().isEqualTo("[]");
		then(productService).should().search(null, null, 10);
	}

	@Test
	void anOutOfRangeLimitOrANegativeCeilingIsA400() {
		assertThat(mvc.get().uri("/api/v1/products?limit=51")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/v1/products?max-price=-1")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(productService);
	}

	@Test
	void getReturnsTheProduct() {
		given(productService.get("LAP-0003")).willReturn(LAPTOP);

		assertThat(mvc.get().uri("/api/v1/products/LAP-0003")).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo(
				"Flex 13 Convertible");
	}

	@Test
	void anUnknownSkuIsA404ProblemDetail() {
		given(productService.get("AUD-9999")).willThrow(new ProductNotFoundException("AUD-9999"));

		assertThat(mvc.get().uri("/api/v1/products/AUD-9999")).hasStatus(HttpStatus.NOT_FOUND)
															  .bodyJson()
															  .extractingPath("$.detail")
															  .isEqualTo("No product with SKU AUD-9999");
	}
}