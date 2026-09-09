package gr.codelearn.springbootai.training.foundation.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@code @Scheduled} method. The trigger is externalised: {@code fixedDelayString} reads {@code app.foundation.heartbeat-interval},
 * defaulting to one hour so a running app or a full test suite gets exactly one beat at startup. {@code application-test.yml} shortens it
 * so {@code SchedulingAndAsyncTest} does not wait.
 *
 * <p>{@code @Scheduled} needs {@code @EnableScheduling} ({@link ComponentsConfig}). The method must take no
 * arguments; return value is ignored.
 */
@Component
class HeartbeatTask {

	private static final Logger log = LoggerFactory.getLogger(HeartbeatTask.class);

	private final AtomicLong beats = new AtomicLong();
	private final CountDownLatch firstBeat = new CountDownLatch(1);

	@Scheduled(fixedDelayString = "${app.foundation.heartbeat-interval:PT1H}")
	void beat() {
		final long count = beats.incrementAndGet();
		firstBeat.countDown();
		log.debug("Heartbeat #{} on '{}'", count, Thread.currentThread().getName());
	}

	long beatCount() {
		return beats.get();
	}

	CountDownLatch firstBeat() {
		return firstBeat;
	}
}