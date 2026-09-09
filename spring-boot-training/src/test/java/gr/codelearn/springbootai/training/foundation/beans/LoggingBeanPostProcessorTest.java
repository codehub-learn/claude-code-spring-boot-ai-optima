package gr.codelearn.springbootai.training.foundation.beans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.2: {@link LoggingBeanPostProcessor} sees every project bean right after its init callbacks. Eager singletons
 * ({@code standardWidget}, {@code thirdPartyResource}) show up; the {@code prototype} and {@code @Lazy} beans that nobody requested do
 * not.
 */
class LoggingBeanPostProcessorTest {
	@Test
	void observesEagerlyCreatedBeansOnly() {
		final AtomicReference<LoggingBeanPostProcessor> processor = new AtomicReference<>();
		new ApplicationContextRunner().withUserConfiguration(BeansConfig.class, LoggingBeanPostProcessor.class)
									  .run(context -> processor.set(context.getBean(LoggingBeanPostProcessor.class)));

		assertThat(processor.get().observedBeanNames()).contains("standardWidget", "thirdPartyResource")
													   .doesNotContain("customWidget", "lazyGreeter");
	}
}