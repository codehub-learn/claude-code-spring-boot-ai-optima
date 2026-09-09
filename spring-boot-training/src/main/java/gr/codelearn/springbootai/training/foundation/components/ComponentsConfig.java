package gr.codelearn.springbootai.training.foundation.components;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the two container features that are opt-in even in a Spring Boot app: {@code @Async} method execution and {@code @Scheduled}
 * task triggering. Neither is active without the matching {@code @Enable...}.
 *
 * <p>This flips them on context-wide, not just for this package — that is how a real app does it. Boot already
 * auto-configures the executor behind {@code @Async} (a virtual-thread {@code SimpleAsyncTaskExecutor} here, because
 * {@code spring.threads.virtual.enabled=true}) and the scheduler behind {@code @Scheduled}, so no {@code Executor} / {@code TaskScheduler}
 * bean is declared.
 */
@Configuration
@EnableAsync
@EnableScheduling
class ComponentsConfig {
}