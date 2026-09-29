package gr.codelearn.springbootai.ai.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Unit 11.2: the arguments the model sends to the {@code getProduct} tool. The pattern is the catalog's SKU shape (letters, a dash,
 * digits): a model that sends a product name or a made-up code is told so, instead of getting a "not found" that looks like a fact about
 * the catalog.
 *
 * @param sku the exact SKU, such as {@code LAP-0002}
 */
public record ProductLookupInput(
		@ToolParam(description = "The SKU: letters, a dash and digits, like LAP-0002")
		@NotBlank(message = "is required")
		@Pattern(regexp = "[A-Za-z]{2,5}-\\d{1,6}", message = "must be letters, a dash and digits, like LAP-0002") String sku) {
}