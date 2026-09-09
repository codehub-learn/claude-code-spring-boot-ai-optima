package gr.codelearn.springbootai.training.foundation.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * An {@code @Async} method. Spring's proxy hands the call to the task executor and returns immediately; the caller gets a
 * {@link CompletableFuture} to compose or join on. Returning {@code CompletableFuture} (not {@code void}) is what lets the caller observe
 * the result and any exception.
 *
 * <p>Rules {@code @Async} shares with {@code @Transactional}: it only works through the Spring proxy, so a
 * self-invocation runs synchronously, and the method must be on a Spring-managed bean. Needs {@code @EnableAsync}
 * ({@link ComponentsConfig}); the executor here is Boot's virtual-thread {@code SimpleAsyncTaskExecutor}.
 */
@Component
class AsyncGreeter {

	private static final Logger log = LoggerFactory.getLogger(AsyncGreeter.class);

	@Async
	public CompletableFuture<String> greetAsync(final String name) {
		final String thread = Thread.currentThread().getName();
		log.debug("greetAsync('{}') running on '{}'", name, thread);
		return CompletableFuture.completedFuture("Hello, " + name + "! (from " + thread + ")");
	}
}