package gr.codelearn.springbootai.training.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/orders}. {@code OrderWebMapper} converts it to the internal {@link PlaceOrderCommand} that
 * {@link OrderService#placeOrder(PlaceOrderCommand)} consumes — the web layer and the service layer keep separate types so U4.3 can reshape
 * the wire contract (nested line DTOs, {@code @JsonView}) without touching the command object.
 *
 * <p>{@code @Valid} on the {@code lines} elements and on {@code shippingAddress} (U4.2) makes validation cascade into
 * {@link OrderLineRequest} and {@link AddressRequest} — a nested-body failure reports the same {@code field}/{@code message} shape as a
 * top-level one.
 */
public record PlaceOrderRequest(
		@NotBlank String orderNumber,
		@NotBlank @Email String customerEmail,
		@NotNull @Valid AddressRequest shippingAddress,
		@NotEmpty List<@Valid OrderLineRequest> lines) {
}
