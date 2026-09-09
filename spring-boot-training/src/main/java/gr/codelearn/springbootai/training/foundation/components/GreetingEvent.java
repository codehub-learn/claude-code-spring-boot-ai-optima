package gr.codelearn.springbootai.training.foundation.components;

import java.time.Instant;

/**
 * A plain immutable payload. Since Spring Framework 4.2 an application event does not need to extend {@code ApplicationEvent} — any object
 * works, so a {@code record} is the natural fit (CLAUDE.md: records for event payloads).
 */
record GreetingEvent(String message, Instant occurredAt) {

	static GreetingEvent of(final String message) {
		return new GreetingEvent(message, Instant.now());
	}
}