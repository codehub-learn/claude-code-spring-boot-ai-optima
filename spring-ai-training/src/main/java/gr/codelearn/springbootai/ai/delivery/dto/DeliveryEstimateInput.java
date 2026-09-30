package gr.codelearn.springbootai.ai.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Unit 11.3: the arguments the model sends to the {@code estimateDelivery} tool. Both are checked before the carrier is asked anything
 * (11.2): the carrier only answers for a five-digit postcode, and a request it will refuse is not worth a network call.
 *
 * @param sku      the exact SKU of the product to deliver, such as {@code LAP-0002}
 * @param postcode the five-digit postcode to deliver to
 */
public record DeliveryEstimateInput(
		@ToolParam(description = "The SKU of the product: letters, a dash and digits, like LAP-0002")
		@NotBlank(message = "is required")
		@Pattern(regexp = "[A-Za-z]{2,5}-\\d{1,6}", message = "must be letters, a dash and digits, like LAP-0002") String sku,
		@ToolParam(description = "The delivery postcode: exactly five digits")
		@NotBlank(message = "is required")
		@Pattern(regexp = "\\d{5}", message = "must be exactly five digits") String postcode) {
}
