package gr.codelearn.springbootai.training.foundation.di;

/**
 * Strategy interface with several implementations, so the {@code foundation.di} package has something to disambiguate:
 * {@link EmailNotifier} ({@code @Primary}), {@link SmsNotifier} ({@code @Qualifier} target) and {@link ConsoleNotifier} ({@code local}
 * profile only).
 */
interface Notifier {

	/**
	 * Stable channel key, also used as the {@code Map<String, Notifier>} assertion handle in tests.
	 */
	String channel();

	/**
	 * "Sends" the message. Returns {@code "<channel>:<message>"} so a caller (and the tests) can see which implementation handled it
	 * without any real I/O.
	 */
	String send(String message);
}