package gr.codelearn.springbootai.training.order.mapper;

import gr.codelearn.springbootai.training.order.Address;
import gr.codelearn.springbootai.training.order.dto.AddressRequest;
import gr.codelearn.springbootai.training.order.dto.OrderResponse;
import gr.codelearn.springbootai.training.order.dto.PlaceOrderRequest;
import gr.codelearn.springbootai.training.order.internal.OrderConfirmation;
import gr.codelearn.springbootai.training.order.internal.PlaceOrderCommand;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bridges the order web contract and the internal order types (U4.1): {@link PlaceOrderRequest} → {@link PlaceOrderCommand} on the way in,
 * {@link OrderConfirmation} → {@link OrderResponse} on the way out. Every conversion here is record → record with no entity in sight, so
 * {@code OrderController} may call it directly (CLAUDE.md only bars controllers from entities and repositories).
 *
 * <p>U4.4 folds this into a MapStruct mapper alongside {@code CatalogWebMapper}.
 */
@Component
public class OrderWebMapper {

	public PlaceOrderCommand toCommand(final PlaceOrderRequest request) {
		final AddressRequest a = request.shippingAddress();
		final Address shippingAddress = new Address(a.line1(), a.line2(), a.city(), a.postalCode(), a.country());
		final List<PlaceOrderCommand.Line> lines = request.lines().stream()
														  .map(l -> new PlaceOrderCommand.Line(l.sku(), l.quantity()))
														  .toList();
		return new PlaceOrderCommand(request.orderNumber(), request.customerEmail(), shippingAddress, lines);
	}

	public OrderResponse toResponse(final OrderConfirmation confirmation) {
		return new OrderResponse(
				confirmation.orderNumber(),
				confirmation.status(),
				confirmation.total(),
				confirmation.lineCount());
	}
}
