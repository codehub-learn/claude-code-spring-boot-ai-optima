package gr.codelearn.springbootai.training.foundation.beans;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lifecycle style 1: JSR-250 annotations on your own class. The default choice.
 */
@Component
class AnnotationLifecycleBean {

	private static final Logger log = LoggerFactory.getLogger(AnnotationLifecycleBean.class);

	private boolean initialized;
	private boolean shutdown;

	@PostConstruct
	void init() {
		initialized = true;
		log.info("AnnotationLifecycleBean initialized via @PostConstruct");
	}

	@PreDestroy
	void shutdown() {
		shutdown = true;
		log.info("AnnotationLifecycleBean shut down via @PreDestroy");
	}

	boolean isInitialized() {
		return initialized;
	}

	boolean isShutdown() {
		return shutdown;
	}
}
