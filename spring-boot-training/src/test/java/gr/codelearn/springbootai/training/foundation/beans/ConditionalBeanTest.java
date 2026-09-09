package gr.codelearn.springbootai.training.foundation.beans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.2: {@code @ConditionalOnProperty} on {@link BeansConfig#premiumSupportChannel()} gates a bean on
 * {@code app.foundation.premium-support}.
 */
class ConditionalBeanTest {

	private final ApplicationContextRunner runner =
			new ApplicationContextRunner().withUserConfiguration(BeansConfig.class);

	@Test
	void beanIsAbsentByDefault() {
		runner.run(context -> assertThat(context).doesNotHaveBean(PremiumSupportChannel.class));
	}

	@Test
	void beanAppearsWhenThePropertyIsSetToTrue() {
		runner.withPropertyValues("app.foundation.premium-support=true")
			  .run(context -> assertThat(context).hasSingleBean(PremiumSupportChannel.class));
	}

	@Test
	void beanStaysAbsentWhenThePropertyIsSetToFalse() {
		runner.withPropertyValues("app.foundation.premium-support=false")
			  .run(context -> assertThat(context).doesNotHaveBean(PremiumSupportChannel.class));
	}
}