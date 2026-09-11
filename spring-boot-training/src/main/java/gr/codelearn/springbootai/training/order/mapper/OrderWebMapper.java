package gr.codelearn.springbootai.training.order.mapper;

import gr.codelearn.springbootai.training.order.Address;
import gr.codelearn.springbootai.training.order.Order;
import gr.codelearn.springbootai.training.order.OrderItem;
import gr.codelearn.springbootai.training.order.dto.AddressRequest;
import gr.codelearn.springbootai.training.order.dto.AddressResponse;
import gr.codelearn.springbootai.training.order.dto.OrderDetailResponse;
import gr.codelearn.springbootai.training.order.dto.OrderLineResponse;
import gr.codelearn.springbootai.training.order.dto.PlaceOrderRequest;
import gr.codelearn.springbootai.training.order.internal.OrderConfirmation;
import gr.codelearn.springbootai.training.order.internal.PlaceOrderCommand;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bridges the order web contract and the internal order types (U4.1/U4.3): {@link PlaceOrderRequest} → {@link PlaceOrderCommand} on the way
 * in, {@link OrderConfirmation}/{@link Order} → {@link OrderDetailResponse} on the way out. Every conversion here is record/entity → record
 * with no entity ever handed back to a caller (CLAUDE.md), so {@code OrderController}/{@code OrderService} may call it directly.
 *
 * <p>Two ways in to the same response type: {@link #toResponse(OrderConfirmation)} only has the flat confirmation to work with, so it
 * fills just the {@code Views.Summary} fields and leaves the rest {@code null}/empty; {@link #toDetailResponse(Order)} has the whole
 * aggregate (loaded via {@code OrderRepository.findWithCustomerItemsAndProductsByOrderNumber}) and fills everything. Which fields a caller
 * actually sees is decided by the {@code @JsonView} on the controller method, not by which of these two ran.
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

	/**
	 * Summary-only: {@link OrderConfirmation} carries no customer email, placement time, address, or line detail, so those four fields
	 * come
	 * back {@code null}/empty. Safe, because every endpoint that calls this ({@code placeOrder}, {@code listOrders}, {@code updateStatus})
	 * is {@code @JsonView(Views.Summary.class)} and never serializes them.
	 */
	public OrderDetailResponse toResponse(final OrderConfirmation confirmation) {
		return new OrderDetailResponse(
				confirmation.orderNumber(),
				confirmation.status(),
				confirmation.total(),
				confirmation.lineCount(),
				null,
				null,
				null,
				List.of());
	}

	/**
	 * The full detail view, built from the aggregate itself. Must run inside a transaction while {@code order.getItems()} and each item's
	 * {@code getProduct()} are still attached — {@code OrderService.getOrderDetail} calls this before its {@code readOnly} transaction
	 * returns.
	 */
	public OrderDetailResponse toDetailResponse(final Order order) {
		final AddressResponse address = toResponse(order.getShippingAddress());
		final List<OrderLineResponse> lines = order.getItems().stream().map(this::toResponse).toList();
		return new OrderDetailResponse(
				order.getOrderNumber(),
				order.getStatus(),
				order.getTotalAmount(),
				order.getItems().size(),
				order.getCustomer().getEmail(),
				order.getPlacedAt(),
				address,
				lines);
	}

	private AddressResponse toResponse(final Address address) {
		return new AddressResponse(address.line1(), address.line2(), address.city(), address.postalCode(), address.country());
	}

	private OrderLineResponse toResponse(final OrderItem item) {
		// OrderItem.lineTotal() is package-private (order, not order.mapper) — the same multiplication, recomputed here.
		final BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
		return new OrderLineResponse(item.getProduct().getSku(), item.getProduct().getName(), item.getQuantity(), item.getUnitPrice(),
									 lineTotal);
	}
}