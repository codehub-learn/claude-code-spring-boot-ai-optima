/**
 * Unit 2.3 — component stereotypes and the container services that hang off them.
 *
 * <p>What lives here:
 * <ul>
 *   <li>{@link MessageProvider} with three implementations — {@link StaticMessageComponent}
 *       ({@code @Component}), {@link GreetingService} ({@code @Service}), {@link MessageRepositoryStub}
 *       ({@code @Repository}) — all found by the same component scan because {@code @Service} and
 *       {@code @Repository} are themselves meta-annotated {@code @Component};</li>
 *   <li>{@link ConfigAccessDemo} — the same property read three ways: {@code @Value}, {@code Environment},
 *       and the typed {@code @ConfigurationProperties} record;</li>
 *   <li>the application-event model: {@link GreetingEventPublisher} ({@code ApplicationEventPublisher}) and
 *       {@link GreetingEventListener} ({@code @EventListener}, {@code @Async @EventListener}, and a
 *       {@code @TransactionalEventListener} whose live demo waits for U3.4);</li>
 *   <li>{@link HeartbeatTask} — a {@code @Scheduled} method;</li>
 *   <li>{@link AsyncGreeter} — an {@code @Async} method returning a {@code CompletableFuture};</li>
 *   <li>{@link ServiceTimingAspect} — an {@code @Aspect} that times {@code @Service} calls in this package.</li>
 * </ul>
 *
 * <p>{@link ComponentsConfig} carries {@code @EnableAsync} + {@code @EnableScheduling} for the whole
 * application context. Everything is package-private: nothing outside {@code foundation.components} wires it.
 */
@NullMarked
package gr.codelearn.springbootai.training.foundation.components;

import org.jspecify.annotations.NullMarked;
