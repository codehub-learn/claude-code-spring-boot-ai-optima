package gr.codelearn.springbootai.training.foundation.components;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.3: the {@code @Aspect} times {@code @Service} calls in {@code foundation.components} and nothing else — a {@code @Repository} call
 * in the same package is not advised.
 */
class ServiceTimingAspectTest extends AbstractOracleIntegrationTest {

	@Autowired
	private GreetingService greetingService;

	@Autowired
	private MessageRepositoryStub messageRepositoryStub;

	@Autowired
	private ServiceTimingAspect aspect;

	@Test
	void serviceCallIsTimed() {
		greetingService.greet("Ada");

		final ServiceTimingAspect.Timing timing = aspect.lastTiming();
		assertThat(timing).isNotNull();
		assertThat(timing.method()).contains("greet");
		assertThat(timing.millis()).isNotNegative();
	}

	@Test
	void nonServiceCallIsNotTimed() {
		final ServiceTimingAspect.Timing before = aspect.lastTiming();

		messageRepositoryStub.message();

		assertThat(aspect.lastTiming()).isEqualTo(before);
	}
}