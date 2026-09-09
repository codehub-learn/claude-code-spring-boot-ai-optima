package gr.codelearn.springbootai.training.foundation.components;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Injects every {@link MessageProvider} the component scan found and reports, per bean, which stereotype annotation it actually carries —
 * resolved with {@link AnnotatedElementUtils} so a meta-annotation ({@code @Service}, {@code @Repository}) still counts as
 * {@code @Component}. Logs a one-line summary on boot; {@code ComponentStereotypeTest} asserts against {@link #stereotypesByType()}.
 *
 * <p>{@link ClassUtils#getUserClass} strips the CGLIB suffix first: {@link GreetingService} arrives here as a
 * proxy (it is advised by {@link ServiceTimingAspect}) and stereotype annotations are not inherited by the generated subclass.
 */
@Component
class StereotypeInventory {

	private static final Logger log = LoggerFactory.getLogger(StereotypeInventory.class);

	private final List<MessageProvider> providers;

	StereotypeInventory(final List<MessageProvider> providers) {
		this.providers = List.copyOf(providers);
	}

	@PostConstruct
	void logInventory() {
		log.info("MessageProvider beans found by component scan: {}", stereotypesByType());
	}

	/**
	 * Simple class name -> the stereotype label the bean declares.
	 */
	Map<String, String> stereotypesByType() {
		return providers.stream()
						.collect(Collectors.toMap(p -> ClassUtils.getUserClass(p.getClass()).getSimpleName(),
												  MessageProvider::stereotype));
	}

	List<String> messages() {
		return providers.stream().map(MessageProvider::message).toList();
	}

	/**
	 * Every stereotype is, transitively, a {@code @Component} — that is why one scan picks up all three.
	 */
	boolean allBeansAreComponents() {
		return providers.stream()
						.allMatch(p -> AnnotatedElementUtils.hasAnnotation(ClassUtils.getUserClass(p.getClass()),
																		   Component.class));
	}
}