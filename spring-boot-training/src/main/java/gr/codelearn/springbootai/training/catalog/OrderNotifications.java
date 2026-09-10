package gr.codelearn.springbootai.training.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The live {@code @TransactionalEventListener} demo the {@code foundation.components} unit (2.3) left dormant until there was a real
 * transaction to hang it on.
 *
 * <p>{@link #onOrderPlaced} is bound to {@link TransactionPhase#AFTER_COMMIT}: Spring holds the event until the transaction that published
 * it commits, then invokes the handler on the same thread. If that transaction rolls back — or there was no transaction at all — the
 * handler never runs.
 *
 * <p>This is <b>where an external side effect belongs</b>: sending the confirmation email, calling a payment gateway, kicking off an AI
 * summary. Doing any of that <i>inside</i> {@link OrderService#placeOrder} would hold the database connection and the new row's locks open
 * for the whole network round trip, and a slow or failing call would roll the order back with it. Here the order is already durably
 * committed and the work is free to take its time or fail on its own.
 *
 * <p>The side effect is a logged stub. {@link #deliveredCount()} / {@link #reset()} let {@code OrderServiceIT} assert the handler fired
 * exactly when the transaction committed and not otherwise.
 */
@Component
class OrderNotifications {

	private static final Logger log = LoggerFactory.getLogger(OrderNotifications.class);

	private final AtomicInteger delivered = new AtomicInteger();

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onOrderPlaced(final OrderPlacedEvent event) {
		delivered.incrementAndGet();
		log.info("Order {} committed (total {}) — sending confirmation (stub for an email / payment / AI call)",
				 event.orderNumber(), event.total());
	}

	int deliveredCount() {
		return delivered.get();
	}

	void reset() {
		delivered.set(0);
	}
}
