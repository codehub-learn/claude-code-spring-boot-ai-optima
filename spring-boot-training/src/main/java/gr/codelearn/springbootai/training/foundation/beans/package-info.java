/**
 * Unit 2.2 — bean definition mechanics.
 *
 * <p>What lives here:
 * <ul>
 *   <li>{@link BeansConfig} — {@code @Configuration} with several {@code @Bean} methods, one per concept
 *       below, instead of component scanning;</li>
 *   <li>singleton vs {@code prototype} scope on two {@link Widget} beans;</li>
 *   <li>three lifecycle-hook styles: {@link AnnotationLifecycleBean} ({@code @PostConstruct} /
 *       {@code @PreDestroy}), {@link InterfaceLifecycleBean} ({@code DisposableBean}), and
 *       {@link ThirdPartyResource} ({@code @Bean(initMethod = ..., destroyMethod = ...)});</li>
 *   <li>{@link LazyGreeter} — {@code @Lazy}, constructed only on first use;</li>
 *   <li>{@link PremiumSupportChannel} — registered only when {@code app.foundation.premium-support=true}
 *       ({@code @ConditionalOnProperty});</li>
 *   <li>{@link WidgetFactoryBean} — a {@code FactoryBean<Widget>};</li>
 *   <li>{@link LoggingBeanPostProcessor} — logs every project bean right after it initializes.</li>
 * </ul>
 *
 * <p>Everything is package-private: nothing outside {@code foundation.beans} wires these beans.
 */
@NullMarked
package gr.codelearn.springbootai.training.foundation.beans;

import org.jspecify.annotations.NullMarked;
