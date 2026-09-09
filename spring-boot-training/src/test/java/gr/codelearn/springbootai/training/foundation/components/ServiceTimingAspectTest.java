package gr.codelearn.springbootai.training.foundation.components;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.3: the {@code @Aspect} times {@code @Service} calls in {@code foundation.components} and nothing else — a {@code @Repository} call
 * in the same package is not advised.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceTimingAspectTest {

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