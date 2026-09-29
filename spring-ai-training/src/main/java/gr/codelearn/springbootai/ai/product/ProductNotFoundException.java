package gr.codelearn.springbootai.ai.product;

/**
 * Unit 11.1: no product with this SKU. The REST API maps it to {@code 404 Not Found} ({@code config.web.ApiExceptionHandler}); when a tool
 * throws it, Spring AI sends its message back to the model as the tool's result instead (see {@link CatalogTools}).
 */
public class ProductNotFoundException extends RuntimeException {

	ProductNotFoundException(final String sku) {
		super("No product with SKU " + sku);
	}
}