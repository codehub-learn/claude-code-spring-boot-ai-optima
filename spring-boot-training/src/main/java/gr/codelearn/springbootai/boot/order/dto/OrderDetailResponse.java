package gr.codelearn.springbootai.boot.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import gr.codelearn.springbootai.boot.order.OrderStatus;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * REST response view of an {@link Order} — the sole response type for every order endpoint (U4.3, replacing the flat {@code OrderResponse}
 * from U4.1). Which fields actually serialize is controlled per-endpoint by {@link JsonView}, not by picking a different DTO:
 *
 * <ul>
 *   <li>{@link Views.Summary} (place / list / patch-status): {@code orderNumber}, {@code status}, {@code total}, {@code lineCount}.</li>
 *   <li>{@link Views.Detail} (get-by-number): everything above, plus {@code customerEmail}, {@code placedAt}, {@code shippingAddress},
 *       and the nested {@code lines}.</li>
 * </ul>
 *
 * <p>{@code OrderController} selects the view with {@code @JsonView} on the handler method. The summary-only construction path
 * ({@code OrderWebMapper.toResponse(OrderConfirmation)}) leaves the detail fields {@code null}/empty — harmless, since a
 * {@link Views.Summary}-filtered response never serializes them regardless of value.
 *
 * <p>{@code total} carries {@code @JsonProperty("totalAmount")}: a concrete example of the Java field name and the wire name
 * deliberately differing (the short internal name stays; the public contract spells it out). Contrast {@code ProductResponse}, which
 * renames every field at once via a class-level naming strategy instead of one field at a time.
 */
public record OrderDetailResponse(
		@JsonView(Views.Summary.class) String orderNumber,
		@JsonView(Views.Summary.class) OrderStatus status,
		@JsonView(Views.Summary.class) @JsonProperty("totalAmount") BigDecimal total,
		@JsonView(Views.Summary.class) int lineCount,
		@JsonView(Views.Detail.class) @Nullable String customerEmail,
		@JsonView(Views.Detail.class) @Nullable OffsetDateTime placedAt,
		@JsonView(Views.Detail.class) @Nullable AddressResponse shippingAddress,
		@JsonView(Views.Detail.class) List<OrderLineResponse> lines) {
}