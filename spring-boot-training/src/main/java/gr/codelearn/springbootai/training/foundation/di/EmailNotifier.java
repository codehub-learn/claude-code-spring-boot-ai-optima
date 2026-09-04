package gr.codelearn.springbootai.training.foundation.di;

import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The default {@link Notifier}: {@code @Primary} means a plain {@code Notifier} injection point (no {@code @Qualifier}) resolves to this
 * one even though three beans match.
 *
 * <p>{@code @Order} controls the position in an injected {@code List<Notifier>} (lowest value first).
 */
@Component
@Primary
@Order(10)
class EmailNotifier implements Notifier {

	@Override
	public String channel() {
		return "email";
	}

	@Override
	public String send(final String message) {
		return channel() + ":" + message;
	}
}