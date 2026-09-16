package gr.codelearn.springbootai.boot.order.dto;

import com.fasterxml.jackson.annotation.JsonView;
import gr.codelearn.springbootai.boot.order.OrderItem;

import java.math.BigDecimal;

/**
 * REST response view of one {@link OrderItem} — a nested DTO on {@link OrderDetailResponse#lines()} (U4.3). {@code productName} flattens
 * the {@code item → product} association so the client needs no second call, the same reasoning as {@code ProductResponse.categoryName}.
 * {@code lineTotal} is {@code unitPrice * quantity}, computed once here rather than left for every client to recompute.
 *
 * <p>Every field carries {@code @JsonView(Views.Detail.class)} — see {@link AddressResponse}'s Javadoc for why a nested type reached
 * from a view-filtered response needs its own view tags, not just the containing field's.
 */
public record OrderLineResponse(
		@JsonView(Views.Detail.class) String sku,
		@JsonView(Views.Detail.class) String productName,
		@JsonView(Views.Detail.class) int quantity,
		@JsonView(Views.Detail.class) BigDecimal unitPrice,
		@JsonView(Views.Detail.class) BigDecimal lineTotal) {
}