package gr.codelearn.springbootai.training.foundation.components;

import org.springframework.stereotype.Service;

/**
 * {@code @Service} — semantically "a unit of business logic". Functionally it is a {@code @Component}: same scan, same lifecycle. The extra
 * meaning is what {@link ServiceTimingAspect}'s pointcut keys off ({@code @within(Service)}), and what tooling and readers use to place the
 * class in the architecture.
 */
@Service
class GreetingService implements MessageProvider {

	@Override
	public String stereotype() {
		return "service";
	}

	@Override
	public String message() {
		return greet("world");
	}

	/**
	 * Timed by {@link ServiceTimingAspect} because this class is a {@code @Service} in this package. {@code public} so the CGLIB proxy can
	 * advise it without relying on same-package visibility.
	 */
	public String greet(final String name) {
		return "Hello, " + name + "!";
	}
}