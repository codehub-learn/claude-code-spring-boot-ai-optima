package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.common.ModelReadableException;

/**
 * Unit 11.1: no product with this SKU. The REST API maps it to {@code 404 Not Found} ({@code config.web.ApiExceptionHandler}); when a tool
 * throws it, Spring AI sends its message back to the model as the tool's result instead (see {@link CatalogTools}). Since 11.3 it is a
 * {@link ModelReadableException}: its message is a fact the model may read as it is, unlike the message of an unexpected exception.
 */
public class ProductNotFoundException extends RuntimeException implements ModelReadableException {

	ProductNotFoundException(final String sku) {
		super("No product with SKU " + sku);
	}
}