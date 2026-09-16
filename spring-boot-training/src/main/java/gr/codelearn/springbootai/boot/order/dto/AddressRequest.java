package gr.codelearn.springbootai.boot.order.dto;

import gr.codelearn.springbootai.boot.order.Address;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

/**
 * The shipping address on {@link PlaceOrderRequest} — the wire shape of the {@code @Embeddable} {@link Address}. Kept as its own record so
 * the web contract does not import the persistence value object directly; {@code OrderWebMapper} bridges the two.
 */
public record AddressRequest(
		@NotBlank String line1,
		@Nullable String line2,
		@NotBlank String city,
		@NotBlank String postalCode,
		@NotBlank String country) {
}