package gr.codelearn.springbootai.training.foundation.components;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Cross-cutting method timing, expressed once instead of scattered {@code System.nanoTime()} around every method. {@code @Aspect} is
 * recognised because Boot's AOP auto-configuration is active (aspectjweaver on the classpath); {@code @Component} makes it a bean so Spring
 * can weave it.
 *
 * <p>The pointcut is deliberately narrow — {@code @Service} beans <em>in this package only</em> — so it does
 * not silently start timing the real services that arrive in U3/U4.
 */
@Aspect
@Component
class ServiceTimingAspect {

	private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

	private final AtomicReference<Timing> lastTiming = new AtomicReference<>();

	@Pointcut("within(gr.codelearn.springbootai.training.foundation.components..*) "
			  + "&& @within(org.springframework.stereotype.Service)")
	void serviceMethodInThisPackage() {
	}

	@Around("serviceMethodInThisPackage()")
	Object timeMethod(final ProceedingJoinPoint joinPoint) throws Throwable {
		final long startNanos = System.nanoTime();
		try {
			return joinPoint.proceed();
		} finally {
			final long millis = (System.nanoTime() - startNanos) / 1_000_000;
			final String method = joinPoint.getSignature().toShortString();
			lastTiming.set(new Timing(method, millis));
			log.debug("{} took {} ms", method, millis);
		}
	}

	@Nullable Timing lastTiming() {
		return lastTiming.get();
	}

	record Timing(String method, long millis) {
	}
}