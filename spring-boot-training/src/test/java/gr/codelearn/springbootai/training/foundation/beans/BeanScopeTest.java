package gr.codelearn.springbootai.training.foundation.beans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.2: default (singleton) vs {@code prototype} scope on {@link BeansConfig}'s two {@link Widget} beans.
 */
class BeanScopeTest {
	private final ApplicationContextRunner runner =
			new ApplicationContextRunner().withUserConfiguration(BeansConfig.class);

	@Test
	void singletonScopeReturnsTheSameInstanceEveryTime() {
		runner.run(context -> {
			Widget first = context.getBean("standardWidget", Widget.class);
			Widget second = context.getBean("standardWidget", Widget.class);
			assertThat(first).isSameAs(second);
		});
	}

	@Test
	void prototypeScopeReturnsANewInstanceEveryTime() {
		runner.run(context -> {
			Widget first = context.getBean("customWidget", Widget.class);
			Widget second = context.getBean("customWidget", Widget.class);
			assertThat(first).isNotSameAs(second);
			assertThat(first.id()).isNotEqualTo(second.id());
		});
	}
}