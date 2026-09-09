package gr.codelearn.springbootai.training.foundation.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Unit 2.2 — everything wired via {@code @Bean} methods rather than component scanning: bean scope, three lifecycle-hook styles,
 * {@code @Lazy}, and a property-conditional bean.
 *
 * <p>{@code @Configuration} classes are themselves detected by component scan; nothing imports this class
 * explicitly.
 */
@Configuration
class BeansConfig {

	private static final Logger log = LoggerFactory.getLogger(BeansConfig.class);

	/**
	 * Default scope is singleton: one instance for the whole context, created eagerly during context refresh.
	 */
	@Bean
	Widget standardWidget() {
		return new Widget("standardWidget");
	}

	/**
	 * {@code prototype}: a new instance on every {@code getBean} call or injection point, created lazily. Spring hands it over and forgets
	 * it — no destroy callback for a prototype bean.
	 */
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	Widget customWidget() {
		return new Widget("customWidget");
	}

	/**
	 * {@code @Lazy} defers construction until the bean is first requested instead of during context refresh.
	 * {@link LazyGreeter#constructionCount()} is the proof.
	 */
	@Bean
	@Lazy
	LazyGreeter lazyGreeter() {
		return new LazyGreeter();
	}

	/**
	 * The "third-party class" style: no annotations on {@link ThirdPartyResource} itself, so init/destroy are wired externally on the
	 * {@code @Bean} method.
	 */
	@Bean(initMethod = "start", destroyMethod = "stop")
	ThirdPartyResource thirdPartyResource() {
		return new ThirdPartyResource();
	}

	/**
	 * Only registered when {@code app.foundation.premium-support=true}. The 2.2 tests flip this on and off with
	 * {@code ApplicationContextRunner}; try it for real with {@code --app.foundation.premium-support=true}.
	 */
	@Bean
	@ConditionalOnProperty(prefix = "app.foundation", name = "premium-support", havingValue = "true")
	PremiumSupportChannel premiumSupportChannel() {
		log.info("Premium support channel enabled");
		return new PremiumSupportChannel();
	}
}