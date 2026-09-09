package gr.codelearn.springbootai.training.foundation.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Built by {@link BeansConfig#lazyGreeter()} with {@code @Lazy}. The static counter is the proof: it only moves when the bean is actually
 * requested, never during context refresh (see the 2.2 lazy-bean test).
 */
class LazyGreeter {

	private static final Logger log = LoggerFactory.getLogger(LazyGreeter.class);
	private static final AtomicInteger CONSTRUCTIONS = new AtomicInteger();

	LazyGreeter() {
		CONSTRUCTIONS.incrementAndGet();
		log.info("LazyGreeter constructed (only happens on first use)");
	}

	static int constructionCount() {
		return CONSTRUCTIONS.get();
	}

	String greet(final String name) {
		return "Hello, " + name + "!";
	}
}