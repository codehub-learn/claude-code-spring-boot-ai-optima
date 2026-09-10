package gr.codelearn.springbootai.training.catalog;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/orders}. {@code OrderWebMapper} converts it to the internal {@link PlaceOrderCommand} that
 * {@link OrderService#placeOrder(PlaceOrderCommand)} consumes — the web layer and the service layer keep separate types so U4.3 can reshape
 * the wire contract (nested line DTOs, {@code @JsonView}) without touching the command object.
 *
 * <p>Bean Validation ({@code @NotBlank orderNumber}, {@code @Email customerEmail}, {@code @NotEmpty @Valid lines}) is a U4.2 addition.
 */
record PlaceOrderRequest(
		String orderNumber,
		String customerEmail,
		AddressRequest shippingAddress,
		List<OrderLineRequest> lines) {
}
