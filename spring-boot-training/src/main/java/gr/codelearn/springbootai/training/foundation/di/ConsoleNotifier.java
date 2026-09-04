package gr.codelearn.springbootai.training.foundation.di;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A {@code @Profile("local")} bean: it only exists when the {@code local} profile is active (a {@code ./mvnw spring-boot:run}). Under the
 * {@code test} profile it is absent, which is what makes it a useful target for {@link org.springframework.beans.factory.ObjectProvider} in
 * {@link NotificationDispatcher}.
 */
@Component
@Profile("local")
@Order(30)
class ConsoleNotifier implements Notifier {

	@Override
	public String channel() {
		return "console";
	}

	@Override
	public String send(final String message) {
		return channel() + ":" + message;
	}
}