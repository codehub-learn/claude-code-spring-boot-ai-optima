package gr.codelearn.springbootai.training.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Use-case orchestration for placing and reading orders, and the unit 3.4 exhibit: every method below shows one facet of Spring's
 * declarative transactions. {@code @Transactional} sits here on the service, never on the controller or the repository (CLAUDE.md), and
 * every transactional method is {@code public} because Spring's proxy only advises {@code public} methods.
 *
 * <table>
 *   <caption>What each method demonstrates</caption>
 *   <tr><th>Method</th><th>Point</th></tr>
 *   <tr><td>{@link #placeOrder}</td><td>the baseline read-write transaction; publishes {@link OrderPlacedEvent}</td></tr>
 *   <tr><td>{@link #findOrder}</td><td>{@code readOnly = true} — a hint that lets Hibernate skip dirty checking and the driver
 *   optimise</td></tr>
 *   <tr><td>{@link #peekStatusAfterLocalEdit}</td><td>a {@code readOnly} transaction does not flush: an entity mutated in it is never
 *   written back</td></tr>
 *   <tr><td>{@link #placeOrderAndDeliverReceipt}</td><td>{@code rollbackFor} — opt into rollback for a checked exception (default:
 *   commit)</td></tr>
 *   <tr><td>{@link #placeOrderToleratingLowStock}</td><td>{@code noRollbackFor} — opt out of rollback for a runtime exception (default:
 *   roll back)</td></tr>
 *   <tr><td>{@link #placeBatchIndependently}</td><td>{@code REQUIRES_NEW} per order (via {@link OrderPlacementService}) — one bad order
 *   does not undo the rest</td></tr>
 *   <tr><td>{@link #placeBatchAtomically}</td><td>the contrast: one transaction for the whole batch, all-or-nothing</td></tr>
 *   <tr><td>{@link #checkViaSelfInvocation} vs {@link #checkViaInjectedProxy}</td><td>self-invocation bypasses the proxy, so the
 *   annotation is ignored</td></tr>
 * </table>
 *
 * <p><b>No network or AI calls inside a transaction.</b> An HTTP or model call made from a {@code @Transactional} method keeps the database
 * connection checked out and the touched rows locked for the whole round trip, and a timeout or failure rolls the business data back with
 * it. Do the call before the transaction opens, or react after it commits — see {@link OrderNotifications}, which runs on
 * {@code AFTER_COMMIT}.
 */
@Service
class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final OrderFactory orderFactory;
	private final OrderPlacementService orderPlacementService;
	private final ApplicationEventPublisher events;
	private final OrderService self;

	/**
	 * {@code self} is this same bean, injected as the Spring proxy. {@code @Lazy} breaks the constructor cycle (the proxy is created now,
	 * the target wired on first use). It exists only for {@link #checkViaInjectedProxy()} — production code would put the transactional
	 * work in a collaborator instead, the way {@link #placeBatchIndependently} uses {@link OrderPlacementService}.
	 */
	OrderService(final OrderRepository orderRepository, final OrderFactory orderFactory,
				 final OrderPlacementService orderPlacementService, final ApplicationEventPublisher events,
				 @Lazy final OrderService self) {
		this.orderRepository = orderRepository;
		this.orderFactory = orderFactory;
		this.orderPlacementService = orderPlacementService;
		this.events = events;
		this.self = self;
	}

	/**
	 * Builds the aggregate, saves it, and publishes {@link OrderPlacedEvent}. The event is delivered to {@link OrderNotifications} only
	 * after this transaction commits.
	 */
	@Transactional
	public OrderConfirmation placeOrder(final PlaceOrderCommand command) {
		final Order saved = orderRepository.save(orderFactory.build(command));
		events.publishEvent(new OrderPlacedEvent(saved.getOrderNumber(), saved.getTotalAmount()));
		return OrderConfirmation.from(saved);
	}

	/**
	 * {@code readOnly = true}: Spring sets the Hibernate {@code FlushMode} to {@code MANUAL} and flags the JDBC connection read-only, so
	 * there is no dirty-checking cost and no accidental write. Use it for every query method.
	 */
	@Transactional(readOnly = true)
	public OrderConfirmation findOrder(final String orderNumber) {
		return orderRepository.findByOrderNumber(orderNumber)
							  .map(OrderConfirmation::from)
							  .orElseThrow(() -> new CatalogNotFoundException("No order " + orderNumber));
	}

	/**
	 * Loads the order, changes its status in memory, and returns that value — but because the transaction is {@code readOnly}, Hibernate
	 * never flushes the change. Reload the row afterwards and it still has its old status. A dirty write in a {@code readOnly} transaction
	 * is silently lost, not rejected.
	 */
	@Transactional(readOnly = true)
	public OrderStatus peekStatusAfterLocalEdit(final String orderNumber, final OrderStatus newStatus) {
		final Order order = orderRepository.findByOrderNumber(orderNumber)
										   .orElseThrow(() -> new CatalogNotFoundException("No order " + orderNumber));
		order.setStatus(newStatus);
		return order.getStatus();
	}

	/**
	 * Saves the order, publishes {@link OrderPlacedEvent}, then fails to deliver the receipt. {@link ReceiptDeliveryException} is checked,
	 * and Spring's default rule commits on checked exceptions — {@code rollbackFor} is what makes this roll the order back. Because the
	 * transaction rolls back, the {@code AFTER_COMMIT} listener never runs either.
	 */
	@Transactional(rollbackFor = ReceiptDeliveryException.class)
	public OrderConfirmation placeOrderAndDeliverReceipt(final PlaceOrderCommand command) throws ReceiptDeliveryException {
		final Order saved = orderRepository.save(orderFactory.build(command));
		events.publishEvent(new OrderPlacedEvent(saved.getOrderNumber(), saved.getTotalAmount()));
		throw new ReceiptDeliveryException("Receipt printer offline for order " + saved.getOrderNumber());
	}

	/**
	 * Saves the order, publishes {@link OrderPlacedEvent}, then raises a low-stock warning. {@link LowStockException} is a
	 * {@code RuntimeException}, which the default rule would roll back — {@code noRollbackFor} keeps the commit, so the order persists and
	 * the {@code AFTER_COMMIT} listener does run. The exception still propagates to the caller.
	 */
	@Transactional(noRollbackFor = LowStockException.class)
	public OrderConfirmation placeOrderToleratingLowStock(final PlaceOrderCommand command) {
		final Order saved = orderRepository.save(orderFactory.build(command));
		events.publishEvent(new OrderPlacedEvent(saved.getOrderNumber(), saved.getTotalAmount()));
		throw new LowStockException("Order " + saved.getOrderNumber() + " placed but some lines are back-ordered");
	}

	/**
	 * Places each order in its own {@code REQUIRES_NEW} transaction through {@link OrderPlacementService}. This method itself is
	 * <b>not</b>
	 * transactional; each delegate call commits before the next starts, so an order that fails leaves the already-placed ones intact.
	 * Returns the confirmations for the orders that went through.
	 */
	public List<OrderConfirmation> placeBatchIndependently(final List<PlaceOrderCommand> commands) {
		final List<OrderConfirmation> placed = new ArrayList<>();
		for (final PlaceOrderCommand command : commands) {
			try {
				placed.add(orderPlacementService.placeIndependently(command));
			} catch (final RuntimeException ex) {
				log.warn("Skipping order {}: {}", command.orderNumber(), ex.getMessage());
			}
		}
		return placed;
	}

	/**
	 * The contrast to {@link #placeBatchIndependently}: one transaction for the whole batch. The first bad order throws, and every order
	 * saved earlier in the loop is rolled back with it.
	 */
	@Transactional
	public List<OrderConfirmation> placeBatchAtomically(final List<PlaceOrderCommand> commands) {
		final List<OrderConfirmation> placed = new ArrayList<>();
		for (final PlaceOrderCommand command : commands) {
			placed.add(OrderConfirmation.from(orderRepository.save(orderFactory.build(command))));
		}
		return placed;
	}

	/**
	 * Reports whether a real transaction is active on the calling thread. Transactional, so through the proxy it returns {@code true}.
	 */
	@Transactional
	public boolean probeTransactionActive() {
		return TransactionSynchronizationManager.isActualTransactionActive();
	}

	/**
	 * Calls {@link #probeTransactionActive()} on {@code this} — the call never reaches the proxy, so {@code @Transactional} is not applied
	 * and no transaction starts. Returns {@code false}. This is the self-invocation pitfall.
	 */
	boolean checkViaSelfInvocation() {
		return probeTransactionActive();
	}

	/**
	 * Calls the same method through the injected proxy, so the transaction advice runs. Returns {@code true}. The fix for the pitfall
	 * above: go through a proxy — an injected collaborator, an injected {@code self}, or {@code TransactionTemplate}.
	 */
	boolean checkViaInjectedProxy() {
		return self.probeTransactionActive();
	}
}