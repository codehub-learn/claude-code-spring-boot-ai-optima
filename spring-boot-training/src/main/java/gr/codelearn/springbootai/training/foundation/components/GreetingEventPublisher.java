package gr.codelearn.springbootai.training.foundation.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over {@link ApplicationEventPublisher} — the in-process pub/sub bus every {@code ApplicationContext} exposes. Publishing is
 * how one bean tells others "this happened" without depending on them, the first fix on the list for a circular dependency (see
 * {@code foundation.di}).
 */
@Component
class GreetingEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(GreetingEventPublisher.class);

	private final ApplicationEventPublisher publisher;

	GreetingEventPublisher(final ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	GreetingEvent publish(final String message) {
		final var event = GreetingEvent.of(message);
		log.debug("Publishing GreetingEvent on thread '{}'", Thread.currentThread().getName());
		publisher.publishEvent(event);
		return event;
	}
}