package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.delivery.dto.DeliveryEstimate;
import gr.codelearn.springbootai.ai.delivery.dto.DeliveryEstimateInput;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Unit 11.3: the delivery estimate offered to a model as a tool, and the first tool whose work can fail for reasons that are nobody's
 * fault: it asks an external carrier service. What the model is told when it does:
 *
 * <ul>
 *   <li>the client retries what is worth retrying and gives up with a classified {@code ExternalServiceException} (see
 *   {@code HttpCarrierQuoteClient});</li>
 *   <li>{@code ToolErrorProcessor} turns it into a message that says what failed and what to do: not to retry, not to guess, what to tell
 *   the customer. Nothing technical (URL, status code, stack) reaches the model;</li>
 *   <li>any exception nobody planned for gets a generic message instead of its own, and is logged.</li>
 * </ul>
 *
 * <p>The tool itself has no try/catch: the failure handling is one policy in one place, not a habit of each tool. The description tells
 * the model up front that the answer can be an error, and not to invent an estimate then.
 */
@Component
@Validated
public class DeliveryTools {

	private final DeliveryService deliveryService;

	DeliveryTools(final DeliveryService deliveryService) {
		this.deliveryService = deliveryService;
	}

	@Tool(name = "estimateDelivery",
		  description = "Estimate how many working days delivering a product to a postcode takes, and what it costs in EUR. Needs the "
						+ "product's SKU (find it with the catalog tools) and a five-digit postcode. The estimate comes from an external "
						+ "carrier service that can be unavailable: when this tool reports an error, do not make up an estimate.")
	public DeliveryEstimate estimateDelivery(
			@ToolParam(description = "The product and the postcode") @NotNull @Valid final DeliveryEstimateInput input) {
		return deliveryService.estimate(input.sku(), input.postcode());
	}
}
