package gr.codelearn.springbootai.training.order;

import gr.codelearn.springbootai.training.order.dto.OrderDetailResponse;
import gr.codelearn.springbootai.training.order.dto.PlaceOrderRequest;
import gr.codelearn.springbootai.training.order.dto.UpdateOrderStatusRequest;
import gr.codelearn.springbootai.training.order.dto.Views;
import gr.codelearn.springbootai.training.order.internal.OrderConfirmation;
import gr.codelearn.springbootai.training.order.mapper.OrderWebMapper;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
 * <p>Delegates to {@link OrderService} (the U3.4 transaction exhibit, extended with the use cases the web layer needs) and maps the
 * {@link OrderConfirmation} it returns to an {@link OrderDetailResponse} through {@link OrderWebMapper}. {@code getOrder} instead calls
 * {@link OrderService#getOrderDetail}, which builds the full nested response straight from the aggregate — see {@link Views} for how
 * {@code @JsonView} decides which of {@code OrderDetailResponse}'s fields each endpoint actually serializes (U4.3).
 *
 * <p><b>Validation (U4.2).</b> {@code @Email} on the optional {@code customerEmail} filter is a discrete {@code @RequestParam} constraint
 * — Spring MVC validates it natively (Framework 6.1+, {@code HandlerMethodValidationException} on failure), no class-level
 * {@code @Validated} involved (see {@link gr.codelearn.springbootai.training.catalog.CatalogController}'s Javadoc for why adding one would
 * be wrong here). {@code @Email} only checks a non-null value, so the parameter stays optional. The {@code @Valid} request bodies below
 * throw {@code MethodArgumentNotValidException} instead; both land in {@code config.web.ApiExceptionHandler}.
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
	@JsonView(Views.Summary.class)
	@Operation(summary = "Place an order")
	@ApiResponse(responseCode = "201", description = "Placed; URI in the Location header")
	@ApiResponse(responseCode = "404", description = "Unknown customer or product")
	ResponseEntity<OrderDetailResponse> placeOrder(@RequestBody @Valid final PlaceOrderRequest request) {
		final OrderDetailResponse placed = mapper.toResponse(orderService.placeOrder(mapper.toCommand(request)));
		final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
														.path("/{orderNumber}")
														.buildAndExpand(placed.orderNumber())
														.toUri();
		return ResponseEntity.created(location).body(placed);
	}

	@GetMapping("/{orderNumber}")
	@JsonView(Views.Detail.class)
	@Operation(summary = "Get one order by number, with its lines and shipping address")
	@ApiResponse(responseCode = "404", description = "No order with that number")
	OrderDetailResponse getOrder(@PathVariable final String orderNumber) {
		return orderService.getOrderDetail(orderNumber);
	}

	@GetMapping
	@JsonView(Views.Summary.class)
	@Operation(summary = "List orders, optionally by customer or status")
	List<OrderDetailResponse> listOrders(@RequestParam(required = false) final @Nullable @Email String customerEmail,
										 @RequestParam(required = false) final @Nullable OrderStatus status) {
		return orderService.listOrders(customerEmail, status).stream().map(mapper::toResponse).toList();
	}

	@PatchMapping("/{orderNumber}/status")
	@JsonView(Views.Summary.class)
	@Operation(summary = "Change an order's status")
	@ApiResponse(responseCode = "404", description = "No order with that number")
	OrderDetailResponse updateStatus(@PathVariable final String orderNumber,
									 @RequestBody @Valid final UpdateOrderStatusRequest request) {
		return mapper.toResponse(orderService.updateStatus(orderNumber, request.status()));
	}
}