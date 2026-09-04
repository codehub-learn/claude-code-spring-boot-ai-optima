package gr.codelearn.springbootai.training.foundation.di;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The one collaborator that consumes every injection style unit 2.1 covers. A single constructor, so Spring needs no {@code @Autowired}.
 *
 * <ul>
 *   <li>{@code defaultNotifier} — a bare {@link Notifier}, resolved to the {@code @Primary} {@link EmailNotifier};</li>
 *   <li>{@code urgentNotifier} — {@code @Qualifier("smsNotifier")}, which overrides {@code @Primary};</li>
 *   <li>{@code allNotifiers} — every {@link Notifier} bean, sorted by {@code @Order};</li>
 *   <li>{@code notifiersByName} — the same beans keyed by bean name;</li>
 *   <li>{@code consoleNotifierProvider} — an {@link ObjectProvider} for the {@code local}-only
 *       {@link ConsoleNotifier}: zero or one instance, resolved lazily, never throws when absent.</li>
 * </ul>
 */
@Component
class NotificationDispatcher {

	private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

	private final Notifier defaultNotifier;
	private final Notifier urgentNotifier;
	private final List<Notifier> allNotifiers;
	private final Map<String, Notifier> notifiersByName;
	private final ObjectProvider<ConsoleNotifier> consoleNotifierProvider;

	NotificationDispatcher(final Notifier defaultNotifier,
						   @Qualifier("smsNotifier") final Notifier urgentNotifier,
						   final List<Notifier> allNotifiers,
						   final Map<String, Notifier> notifiersByName,
						   final ObjectProvider<ConsoleNotifier> consoleNotifierProvider) {
		this.defaultNotifier = defaultNotifier;
		this.urgentNotifier = urgentNotifier;
		this.allNotifiers = List.copyOf(allNotifiers);
		this.notifiersByName = Map.copyOf(notifiersByName);
		this.consoleNotifierProvider = consoleNotifierProvider;
	}

	@PostConstruct
	void logWiring() {
		log.info("DI demo wired: default={}, urgent={}, channels={}, console-present={}",
				 defaultNotifier.channel(), urgentNotifier.channel(), channels(),
				 consoleNotifierProvider.getIfAvailable() != null);
	}

	String notifyDefault(final String message) {
		return defaultNotifier.send(message);
	}

	String notifyUrgent(final String message) {
		return urgentNotifier.send(message);
	}

	/**
	 * Fan out to every registered {@link Notifier}, in {@code @Order}.
	 */
	List<String> broadcast(final String message) {
		return allNotifiers.stream().map(notifier -> notifier.send(message)).toList();
	}

	List<String> channels() {
		return allNotifiers.stream().map(Notifier::channel).toList();
	}

	Set<String> beanNames() {
		return notifiersByName.keySet();
	}

	/**
	 * {@link ObjectProvider#getIfAvailable()} returns {@code null} when the {@link ConsoleNotifier} bean is not in the context (any
	 * non-{@code local} profile), so this is empty there and populated under {@code local}.
	 */
	Optional<String> notifyConsoleIfPresent(final String message) {
		return Optional.ofNullable(consoleNotifierProvider.getIfAvailable())
					   .map(console -> console.send(message));
	}
}