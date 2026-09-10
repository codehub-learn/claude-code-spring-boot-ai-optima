package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.Nullable;

/**
 * The shipping address on {@link PlaceOrderRequest} — the wire shape of the {@code @Embeddable} {@link Address}. Kept as its own record so
 * the web contract does not import the persistence value object directly; {@code OrderWebMapper} bridges the two.
 */
record AddressRequest(
		String line1,
		@Nullable String line2,
		String city,
		String postalCode,
		String country) {
}
