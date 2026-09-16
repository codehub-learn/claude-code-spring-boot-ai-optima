package gr.codelearn.springbootai.boot.order.dto;

import com.fasterxml.jackson.annotation.JsonView;

import gr.codelearn.springbootai.boot.order.Address;
import org.jspecify.annotations.Nullable;

/**
 * REST response view of an {@link Address} — the wire shape of {@link OrderDetailResponse#shippingAddress()}. Mirrors
 * {@link AddressRequest} field-for-field; kept as its own type so the response side does not reuse the request record (CLAUDE.md:
 * {@code *Request}/{@code *Response} per resource, no shared mutable shape between the two directions).
 *
 * <p><b>Every field needs its own {@code @JsonView}, or none of them render.</b> {@code MapperFeature.DEFAULT_VIEW_INCLUSION} defaults
 * to {@code false}, and that setting isn't scoped to the root type being serialized — once <i>any</i> view is active for a response (here,
 * {@link Views.Detail} on {@code OrderDetailResponse}), every nested type reached during that same serialization is filtered by it too.
 * Leave a nested record's fields unannotated and Jackson silently serializes it as {@code {}}, not "everything included by default" —
 * caught by {@code OrderControllerTest} asserting an actual nested field, not just that {@code shippingAddress} is present.
 */
public record AddressResponse(
		@JsonView(Views.Detail.class) String line1,
		@JsonView(Views.Detail.class) @Nullable String line2,
		@JsonView(Views.Detail.class) String city,
		@JsonView(Views.Detail.class) String postalCode,
		@JsonView(Views.Detail.class) String country) {
}