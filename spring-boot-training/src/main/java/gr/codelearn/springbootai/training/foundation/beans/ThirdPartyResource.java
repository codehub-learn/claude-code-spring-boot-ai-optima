package gr.codelearn.springbootai.training.foundation.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stands in for a class you cannot annotate (imagine it comes from a library): no {@code @PostConstruct} / {@code @PreDestroy}, no
 * {@code InitializingBean} / {@code DisposableBean}. {@link BeansConfig#thirdPartyResource()} wires {@code start}/{@code stop} externally
 * via {@code @Bean(initMethod = ..., destroyMethod = ...)} instead.
 */
class ThirdPartyResource {

	private static final Logger log = LoggerFactory.getLogger(ThirdPartyResource.class);

	private boolean started;
	private boolean stopped;

	void start() {
		started = true;
		log.info("ThirdPartyResource started");
	}

	void stop() {
		stopped = true;
		log.info("ThirdPartyResource stopped");
	}

	boolean isStarted() {
		return started;
	}

	boolean isStopped() {
		return stopped;
	}
}