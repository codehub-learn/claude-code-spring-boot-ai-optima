package gr.codelearn.springbootai.training.foundation.di;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A non-primary {@link Notifier}. {@link NotificationDispatcher} pulls it in with {@code @Qualifier("smsNotifier")} (the default bean
 * name), which wins over {@code @Primary}.
 */
@Component
@Order(20)
class SmsNotifier implements Notifier {

	@Override
	public String channel() {
		return "sms";
	}

	@Override
	public String send(final String message) {
		return channel() + ":" + message;
	}
}