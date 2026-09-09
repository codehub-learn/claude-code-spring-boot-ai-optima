package gr.codelearn.springbootai.training.foundation.beans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.2: {@code @Lazy} on {@link BeansConfig#lazyGreeter()} defers construction to first use. {@link LazyGreeter#constructionCount()} is
 * a process-wide counter, so this checks a delta rather than an absolute value.
 */
class LazyBeanTest {
	@Test
	void lazyBeanIsNotConstructedUntilFirstRequested() {
		final int before = LazyGreeter.constructionCount();
		new ApplicationContextRunner().withUserConfiguration(BeansConfig.class).run(context -> {
			assertThat(LazyGreeter.constructionCount()).isEqualTo(before);
			final LazyGreeter greeter = context.getBean(LazyGreeter.class);
			assertThat(LazyGreeter.constructionCount()).isEqualTo(before + 1);
			assertThat(greeter.greet("Ada")).isEqualTo("Hello, Ada!");
		});
	}
}