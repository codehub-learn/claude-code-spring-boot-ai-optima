package gr.codelearn.springbootai.ai.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;

/**
 * Unit 11.2: the arguments the model sends to the {@code searchProducts} tool. A record with Bean Validation constraints: Spring AI puts it
 * in the tool's JSON schema, and the constraints run when the call arrives (see {@code CatalogTools}), before any SQL.
 *
 * @param keywords one to three product words in English, or {@code null} to list products
 * @param maxPrice the customer's budget in EUR, or {@code null} for any price
 */
public record ProductSearchInput(
		@ToolParam(required = false,
				   description = "One to three product words in English (a product type, category, brand or feature). No prices, "
								 + "numbers or conditions here. Leave out to list products.")
		@Size(max = 60, message = "must be at most 60 characters: one to three product words, not a sentence") @Nullable String keywords,
		@ToolParam(required = false, description = "Highest price in EUR, when the customer gives a budget. Leave out otherwise.")
		@Positive(message = "must be greater than 0 (a price in EUR)")
		@DecimalMax(value = "100000", message = "must be at most 100000 (a price in EUR)") @Nullable BigDecimal maxPrice) {
}