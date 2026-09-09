package gr.codelearn.springbootai.training.foundation.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.3: the application-event bus. A plain {@code @EventListener} runs inline on the publishing thread; an
 * {@code @Async @EventListener} runs on the task executor, off the publisher's stack. The {@code @TransactionalEventListener} is not
 * exercised here — it needs a transaction manager (U3.4).
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationEventsTest {

	@Autowired
	private GreetingEventPublisher publisher;

	@Autowired
	private GreetingEventListener listener;

	@BeforeEach
	void resetListener() {
		listener.reset();
	}

	@Test
	void syncListenerRunsOnThePublishingThread() {
		final String callerThread = Thread.currentThread().getName();

		final GreetingEvent published = publisher.publish("hello");

		assertThat(listener.syncHandlingThread()).isEqualTo(callerThread);
		assertThat(listener.lastEvent()).isEqualTo(published);
	}

	@Test
	void asyncListenerRunsOnAnotherThread() throws InterruptedException {
		final String callerThread = Thread.currentThread().getName();

		publisher.publish("hello async");

		assertThat(listener.asyncLatch().await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(listener.asyncHandlingThread()).isNotNull().isNotEqualTo(callerThread);
	}
}