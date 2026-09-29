package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.product.dto.ProductLookupInput;
import gr.codelearn.springbootai.ai.product.dto.ProductResponse;
import gr.codelearn.springbootai.ai.product.dto.ProductSearchInput;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Unit 11.1: the product lookup offered to a model as tools. Spring AI turns each {@link Tool} method into a tool definition (name,
 * description, a JSON schema of the parameters from their types and {@link ToolParam}s) and sends the definitions with the prompt. The
 * model never runs anything: it answers with a tool call (a name and JSON arguments), Spring AI invokes the method, serializes the result
 * to JSON and sends it back as a tool response message, and the model continues, possibly calling again.
 *
 * <p>So, unlike {@link ProductController}:
 * <ul>
 *   <li>the <b>descriptions are the interface</b>: they are all the model knows about when and how to call, so they say what the tool is
 *   for, what it returns, and what the arguments look like;</li>
 *   <li>the <b>model chooses</b> whether to call, which tool, with which arguments, and how often; nothing guarantees a call;</li>
 *   <li>the <b>result is text for a model</b>, not a response for a program: it is kept small (at most {@value #MAX_RESULTS} products),
 *   because it is sent back in the prompt and costs tokens;</li>
 *   <li><b>failures become text too</b>: an exception (here {@link ProductNotFoundException}) is not a 404; by default Spring AI sends its
 *   message back to the model as the tool's result. Unit 11.3 takes this further.</li>
 * </ul>
 *
 * <p>The parameters and their semantics follow what the model writes, not the SQL. Two live-run fixes (qwen3.5:4b, Greek questions):
 * <ul>
 *   <li>with only {@code keywords}, "laptops under 1500 euro" became {@code "laptop, under 1500 euro, price"}; every word had to match,
 *   nothing did, and the model told the customer there were none. Now a {@code maxPrice} parameter, and the description keeps prices out
 *   of the keywords;</li>
 *   <li>"what headphones do you have" became {@code "acoustics headphones speakers"}: a model lists synonyms. Every word had to match
 *   again, so nothing did. Now any word matches and the best matches come first.</li>
 * </ul>
 * Both times the tool "worked" (no error), and the model trusted the empty result: a wrong answer, stated with confidence.
 *
 * <p>Unit 11.2: each tool takes one input record with Bean Validation constraints ({@link ProductSearchInput},
 * {@link ProductLookupInput}), and the class is {@link Validated}, so Spring's method validation proxy checks the arguments before the
 * method runs. Nothing else validates a model's arguments: Spring AI binds the JSON to the parameter types and calls the method, and a
 * model can send anything, valid JSON or not. A violation throws {@code ConstraintViolationException}, which {@code
 * ToolValidationExceptionProcessor} turns into a message the model can act on.
 *
 * <p>Both methods only read. A tool that changes something must be designed for being called when nobody asked (CLAUDE.md:
 * side-effect-aware tools).
 */
@Component
@Validated
public class CatalogTools {

	/**
	 * How many products one search hands to the model.
	 */
	static final int MAX_RESULTS = 5;

	private final ProductService productService;

	CatalogTools(final ProductService productService) {
		this.productService = productService;
	}

	@Tool(name = "searchProducts",
		  description = "Search the store's product catalog. Returns up to 5 products with SKU, name, category, current price in EUR and "
						+ "description. Use it for any question about which products the store sells, what they cost, or to find a "
						+ "SKU. Products matching more keywords come first.")
	public List<ProductResponse> searchProducts(@ToolParam(required = false, description = "The search: keywords and a price ceiling, "
																						   +
	                                                                                       "both optional") @Valid final @Nullable ProductSearchInput input) {
		return input == null ? productService.search(null, null, MAX_RESULTS)
							 : productService.search(input.keywords(), input.maxPrice(), MAX_RESULTS);
	}

	@Tool(name = "getProduct",
		  description = "Get one product by its exact SKU, with its current price in EUR. Use it when the SKU is known.")
	public ProductResponse getProduct(@ToolParam(description = "The product to get") @NotNull @Valid final ProductLookupInput input) {
		return productService.get(input.sku());
	}
}