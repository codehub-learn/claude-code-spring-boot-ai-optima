package gr.codelearn.springbootai.training.foundation.components;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.3: {@code @Scheduled} fires {@link HeartbeatTask#beat()} on its own (the {@code test} profile shortens the interval to 200 ms),
 * and {@code @Async} runs {@link AsyncGreeter#greetAsync} on the task executor, not the caller thread.
 */
class SchedulingAndAsyncTest extends AbstractOracleIntegrationTest {

	@Autowired
	private HeartbeatTask heartbeatTask;

	@Autowired
	private AsyncGreeter asyncGreeter;

	@Test
	void scheduledTaskFiresOnItsOwn() throws InterruptedException {
		assertThat(heartbeatTask.firstBeat().await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(heartbeatTask.beatCount()).isPositive();
	}

	@Test
	void asyncMethodCompletesOffTheCallerThread() throws InterruptedException, ExecutionException, TimeoutException {
		final String callerThread = Thread.currentThread().getName();

		final CompletableFuture<String> future = asyncGreeter.greetAsync("world");
		final String result = future.get(5, TimeUnit.SECONDS);

		assertThat(result).startsWith("Hello, world!").doesNotContain("(from " + callerThread + ")");
	}
}