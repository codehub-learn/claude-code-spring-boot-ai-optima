/**
 * Unit 2.1 — dependency-injection mechanics, shown with a set of small throwaway {@link Notifier} beans.
 *
 * <p>What lives here:
 * <ul>
 *   <li>constructor injection as the only wiring style (no field or setter injection anywhere);</li>
 *   <li>{@code @Primary} vs {@code @Qualifier} when one interface has several implementations;</li>
 *   <li>injecting every implementation as a {@code List<T>} (ordered by {@code @Order}) and as a
 *       {@code Map<String, T>} keyed by bean name;</li>
 *   <li>{@link org.springframework.beans.factory.ObjectProvider} for an optional, profile-gated collaborator;</li>
 *   <li>{@link ConsoleNotifier}, a {@code @Profile("local")}-scoped bean;</li>
 *   <li>{@link CircularDependencyExample} — a commented constructor cycle and how Boot 4 reports it.</li>
 * </ul>
 *
 * <p>Everything is package-private: nothing outside {@code foundation.di} wires these beans.
 */
@NullMarked
package gr.codelearn.springbootai.training.foundation.di;

import org.jspecify.annotations.NullMarked;
