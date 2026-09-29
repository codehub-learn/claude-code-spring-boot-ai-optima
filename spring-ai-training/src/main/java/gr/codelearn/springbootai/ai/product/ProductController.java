package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.product.dto.ProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unit 11.1: the product lookup as a plain REST API. The caller (a program) decides which endpoint to call and with which arguments, gets
 * JSON with a status code, and handles a 404 itself. Contrast with {@link CatalogTools}, the same lookups offered to a model.
 */
@RestController
@RequestMapping(path = "/api/{version}/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "products")
class ProductController {

	private final ProductService productService;

	ProductController(final ProductService productService) {
		this.productService = productService;
	}

	@GetMapping(version = "v1")
	@Operation(summary = "Search products",
			   description = "Products whose name, description or category contains any word of the query (case-insensitive), those "
							 + "matching the most words first; no query lists products by name. max-price caps the price (EUR). "
							 + "Generated demo products are left out.")
	@ApiResponse(responseCode = "200", description = "Matching products, by name")
	@ApiResponse(responseCode = "400", description = "limit outside 1-50, a negative max-price, a query over 200 characters, or an "
													 + "unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	List<ProductResponse> search(@RequestParam(required = false) @Size(max = 200) final @Nullable String query,
								 @RequestParam(name = "max-price", required = false) @PositiveOrZero final @Nullable BigDecimal maxPrice,
								 @RequestParam(defaultValue = "10") @Min(1) @Max(50) final int limit) {
		return productService.search(query, maxPrice, limit);
	}

	@GetMapping(path = "/{sku}", version = "v1")
	@Operation(summary = "Get a product by SKU")
	@ApiResponse(responseCode = "200", description = "The product with its current price")
	@ApiResponse(responseCode = "404", description = "No product with this SKU",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	ProductResponse get(@PathVariable final String sku) {
		return productService.get(sku);
	}
}