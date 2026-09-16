package gr.codelearn.springbootai.boot.order.dto;

import gr.codelearn.springbootai.boot.catalog.Product;
import gr.codelearn.springbootai.boot.order.internal.OrderFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * One requested order line on {@link PlaceOrderRequest}: a product {@code sku} and a quantity. The unit price is not the client's to set —
 * {@link OrderFactory} captures it from the current {@link Product#getPrice()} at order time.
 */
public record OrderLineRequest(@NotBlank String sku, @Positive int quantity) {
}