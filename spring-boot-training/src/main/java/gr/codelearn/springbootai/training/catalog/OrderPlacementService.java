package gr.codelearn.springbootai.training.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places one order in its <b>own</b> transaction. {@code @Transactional(propagation = REQUIRES_NEW)} suspends any transaction the caller is
 * already running, opens a fresh one, commits (or rolls back) just this order, then resumes the caller's.
 *
 * <p>It is a separate bean, not a method on {@link OrderService}, for a concrete reason: {@code REQUIRES_NEW} only takes effect when the
 * call goes through the Spring proxy. If {@link OrderService#placeBatchIndependently} called a {@code REQUIRES_NEW} method <i>on
 * itself</i>, the call would never leave the target instance, the annotation would be ignored, and every order would share the one outer
 * transaction — the same self-invocation trap {@link OrderService#checkViaSelfInvocation()} demonstrates. Injecting this as a collaborator
 * sidesteps it.
 *
 * <p>The transactional method is {@code public}: Spring's proxy only advises {@code public} methods, so a {@code @Transactional} on a
 * package-private method would compile and run but silently open no transaction. The class itself stays package-private.
 */
@Service
class OrderPlacementService {

	private final OrderRepository orderRepository;
	private final OrderFactory orderFactory;

	OrderPlacementService(final OrderRepository orderRepository, final OrderFactory orderFactory) {
		this.orderRepository = orderRepository;
		this.orderFactory = orderFactory;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public OrderConfirmation placeIndependently(final PlaceOrderCommand command) {
		return OrderConfirmation.from(orderRepository.save(orderFactory.build(command)));
	}
}