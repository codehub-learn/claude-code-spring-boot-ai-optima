package gr.codelearn.springbootai.training.foundation.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runs {@code postProcessAfterInitialization} for every bean in the context, right after its init callbacks ({@code @PostConstruct}, the
 * {@code @Bean(initMethod = ...)}) have run. Filtered to this project's own beans so it doesn't drown in framework-internal ones.
 * {@link #observedBeanNames()} is how the 2.2 test proves it actually saw {@link BeansConfig}'s beans get created — and that a
 * {@code prototype} / {@code @Lazy} bean that nobody asked for does not appear.
 */
@Component
class LoggingBeanPostProcessor implements BeanPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(LoggingBeanPostProcessor.class);

	private final List<String> observed = new CopyOnWriteArrayList<>();

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		if (bean.getClass().getPackageName().startsWith("gr.codelearn.springbootai.training")) {
			observed.add(beanName);
			log.debug("Bean ready: '{}' ({})", beanName, bean.getClass().getSimpleName());
		}
		return bean;
	}

	List<String> observedBeanNames() {
		return List.copyOf(observed);
	}
}