package gr.codelearn.springbootai.ai.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Unit 11.1: one catalog product, as the REST endpoints return it and as the tools hand it to the model (serialized to JSON).
 *
 * @param sku         the stock-keeping unit, unique
 * @param name        the product name
 * @param category    the category name
 * @param price       the current price, two decimals
 * @param currency    always {@value #CURRENCY}
 * @param description the catalog description, if any
 */
public record ProductResponse(
		@Schema(example = "LAP-0002") String sku,
		@Schema(example = "Latitude 16 Workstation") String name,
		@Schema(example = "Laptops") String category,
		@Schema(example = "2199.00") BigDecimal price,
		@Schema(example = CURRENCY) String currency,
		@Schema(example = "16\" mobile workstation, 32 GB RAM, 1 TB SSD, discrete GPU") @Nullable String description) {

	public static final String CURRENCY = "EUR";
}