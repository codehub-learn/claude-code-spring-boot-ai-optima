package gr.codelearn.springbootai.training.foundation.di;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.1, {@code test} profile: {@code @Primary} / {@code @Qualifier} resolution, {@code List} and {@code Map} injection, and an
 * {@link org.springframework.beans.factory.ObjectProvider} that is empty because the {@code local}-only {@link ConsoleNotifier} is not in
 * this context.
 */
class DependencyInjectionTest extends AbstractOracleIntegrationTest {

	@Autowired
	private NotificationDispatcher dispatcher;

	@Autowired
	private Notifier injectedNotifier;

	@Autowired
	private ApplicationContext context;

	@Test
	void primaryResolvesABareNotifierInjectionPoint() {
		assertThat(injectedNotifier.channel()).isEqualTo("email");
		assertThat(dispatcher.notifyDefault("hi")).isEqualTo("email:hi");
	}

	@Test
	void qualifierOverridesPrimary() {
		assertThat(dispatcher.notifyUrgent("hi")).isEqualTo("sms:hi");
	}

	@Test
	void listInjectionHoldsEveryBeanSortedByOrder() {
		assertThat(dispatcher.channels()).containsExactly("email", "sms");
		assertThat(dispatcher.broadcast("hi")).containsExactly("email:hi", "sms:hi");
	}

	@Test
	void mapInjectionIsKeyedByBeanName() {
		assertThat(dispatcher.beanNames()).containsExactlyInAnyOrder("emailNotifier", "smsNotifier");
	}

	@Test
	void profileScopedBeanIsAbsentAndObjectProviderStaysEmpty() {
		assertThat(context.getBeanNamesForType(ConsoleNotifier.class)).isEmpty();
		assertThat(dispatcher.notifyConsoleIfPresent("hi")).isEmpty();
	}
}