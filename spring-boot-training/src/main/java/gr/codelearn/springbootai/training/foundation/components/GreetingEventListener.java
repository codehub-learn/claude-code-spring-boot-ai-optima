package gr.codelearn.springbootai.training.foundation.components;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Three ways to receive a {@link GreetingEvent}:
 *
 * <ul>
 *   <li>{@link #onGreeting} — a plain {@code @EventListener}. Runs synchronously, on the thread that called
 *       {@code publishEvent}, inside the publisher's call stack;</li>
 *   <li>{@link #onGreetingAsync} — {@code @Async @EventListener}. Handed to the task executor, so it runs on a
 *       different thread and the publisher does not wait. Needs {@code @EnableAsync}
 *       ({@link ComponentsConfig});</li>
 *   <li>{@link #onGreetingAfterCommit} — {@code @TransactionalEventListener}. Fires only after the surrounding
 *       transaction commits, and is silently skipped when no transaction is active. There is no transaction
 *       manager in this module yet, so it stays dormant until U3.4 — kept here to show the annotation next to
 *       its siblings.</li>
 * </ul>
 *
 * <p>Handling threads and payloads are recorded so {@code ApplicationEventsTest} can assert them. {@link #reset()}
 * clears the state between tests.
 */
@Component
class GreetingEventListener {

	private static final Logger log = LoggerFactory.getLogger(GreetingEventListener.class);

	private final AtomicReference<String> syncThread = new AtomicReference<>();
	private final AtomicReference<String> asyncThread = new AtomicReference<>();
	private final AtomicReference<GreetingEvent> lastEvent = new AtomicReference<>();
	private volatile CountDownLatch asyncLatch = new CountDownLatch(1);

	@EventListener
	void onGreeting(final GreetingEvent event) {
		syncThread.set(Thread.currentThread().getName());
		lastEvent.set(event);
		log.debug("Sync @EventListener handled '{}' on '{}'", event.message(), syncThread.get());
	}

	@Async
	@EventListener
	public void onGreetingAsync(final GreetingEvent event) {
		asyncThread.set(Thread.currentThread().getName());
		log.debug("Async @EventListener handled '{}' on '{}'", event.message(), asyncThread.get());
		asyncLatch.countDown();
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onGreetingAfterCommit(final GreetingEvent event) {
		log.info("AFTER_COMMIT @TransactionalEventListener handled '{}' (needs an active transaction — see U3.4)",
				 event.message());
	}

	void reset() {
		syncThread.set(null);
		asyncThread.set(null);
		lastEvent.set(null);
		asyncLatch = new CountDownLatch(1);
	}

	CountDownLatch asyncLatch() {
		return asyncLatch;
	}

	@Nullable String syncHandlingThread() {
		return syncThread.get();
	}

	@Nullable String asyncHandlingThread() {
		return asyncThread.get();
	}

	@Nullable GreetingEvent lastEvent() {
		return lastEvent.get();
	}
}