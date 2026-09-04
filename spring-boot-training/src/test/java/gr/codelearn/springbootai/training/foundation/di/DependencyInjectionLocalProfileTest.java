package gr.codelearn.springbootai.training.foundation.di;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.1, {@code local} profile added: the {@code @Profile("local")} {@link ConsoleNotifier} now joins the context, so it appears in the
 * {@code List} / {@code Map} and the {@link org.springframework.beans.factory.ObjectProvider} resolves.
 */
@SpringBootTest
@ActiveProfiles({"test", "local"})
class DependencyInjectionLocalProfileTest {

	@Autowired
	private NotificationDispatcher dispatcher;

	@Test
	void profileScopedBeanJoinsListAndMapInOrder() {
		assertThat(dispatcher.channels()).containsExactly("email", "sms", "console");
		assertThat(dispatcher.beanNames())
				.containsExactlyInAnyOrder("emailNotifier", "smsNotifier", "consoleNotifier");
	}

	@Test
	void objectProviderResolvesWhenTheBeanExists() {
		assertThat(dispatcher.notifyConsoleIfPresent("hi")).contains("console:hi");
	}
}