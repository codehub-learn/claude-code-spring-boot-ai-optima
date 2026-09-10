package gr.codelearn.springbootai.training.order;

import gr.codelearn.springbootai.training.order.dto.OrderResponse;
import gr.codelearn.springbootai.training.order.dto.PlaceOrderRequest;
import gr.codelearn.springbootai.training.order.dto.UpdateOrderStatusRequest;
import gr.codelearn.springbootai.training.order.internal.OrderConfirmation;
import gr.codelearn.springbootai.training.order.mapper.OrderWebMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for the order resource (U4.1). Placement, read-back, listing, and a status transition — enough to show {@code POST} →
 * {@code 201 + Location}, {@code @PathVariable}, {@code @RequestParam} filters, and {@code PATCH} on a sub-resource.
 *
 * <p>Delegates to {@link OrderService} (the U3.4 transaction exhibit, extended with the two use cases the web layer needs) and maps the
 * {@link OrderConfirmation} it returns to an {@link OrderResponse} through {@link OrderWebMapper}.
 */
@Tag(name = "orders")
@RestController
@RequestMapping(path = "/api/v1/orders", produces = MediaType.APPLICATION_JSON_VALUE)
class OrderController {

	private final OrderService orderService;
	private final OrderWebMapper mapper;

	OrderController(final OrderService orderService, final OrderWebMapper mapper) {
		this.orderService = orderService;
		this.mapper = mapper;
	}

	@PostMapping
	@Operation(summary = "Place an order")
	@ApiResponse(responseCode = "201", description = "Placed; URI in the Location header")
	@ApiResponse(responseCode = "404", description = "Unknown customer or product")
	ResponseEntity<OrderResponse> placeOrder(@RequestBody final PlaceOrderRequest request) {
		final OrderResponse placed = mapper.toResponse(orderService.placeOrder(mapper.toCommand(request)));
		final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
														.path("/{orderNumber}")
														.buildAndExpand(placed.orderNumber())
														.toUri();
		return ResponseEntity.created(location).body(placed);
	}

	@GetMapping("/{orderNumber}")
	@Operation(summary = "Get one order by number")
	@ApiResponse(responseCode = "404", description = "No order with that number")
	OrderResponse getOrder(@PathVariable final String orderNumber) {
		return mapper.toResponse(orderService.findOrder(orderNumber));
	}

	@GetMapping
	@Operation(summary = "List orders, optionally by customer or status")
	List<OrderResponse> listOrders(@RequestParam(required = false) final @Nullable String customerEmail,
								   @RequestParam(required = false) final @Nullable OrderStatus status) {
		return orderService.listOrders(customerEmail, status).stream().map(mapper::toResponse).toList();
	}

	@PatchMapping("/{orderNumber}/status")
	@Operation(summary = "Change an order's status")
	@ApiResponse(responseCode = "404", description = "No order with that number")
	OrderResponse updateStatus(@PathVariable final String orderNumber, @RequestBody final UpdateOrderStatusRequest request) {
		return mapper.toResponse(orderService.updateStatus(orderNumber, request.status()));
	}
}
