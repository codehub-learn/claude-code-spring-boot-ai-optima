package gr.codelearn.springbootai.training.foundation.beans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.2: three ways to hook into bean creation and destruction, all exercised on the same context. Each bean reference is captured
 * inside {@code run(...)} and asserted again after the runner closes the context, proving the destroy callback actually ran.
 */
class BeanLifecycleTest {
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(BeansConfig.class, AnnotationLifecycleBean.class, InterfaceLifecycleBean.class);

	@Test
	void postConstructAndPreDestroyAnnotationsRun() {
		final AtomicReference<AnnotationLifecycleBean> bean = new AtomicReference<>();
		runner.run(context -> {
			bean.set(context.getBean(AnnotationLifecycleBean.class));
			assertThat(bean.get().isInitialized()).isTrue();
			assertThat(bean.get().isShutdown()).isFalse();
		});
		assertThat(bean.get().isShutdown()).isTrue();
	}

	@Test
	void disposableBeanInterfaceRunsOnClose() {
		final AtomicReference<InterfaceLifecycleBean> bean = new AtomicReference<>();
		runner.run(context -> {
			bean.set(context.getBean(InterfaceLifecycleBean.class));
			assertThat(bean.get().isDestroyed()).isFalse();
		});
		assertThat(bean.get().isDestroyed()).isTrue();
	}

	@Test
	void beanMethodInitAndDestroyMethodsRun() {
		final AtomicReference<ThirdPartyResource> resource = new AtomicReference<>();
		runner.run(context -> {
			resource.set(context.getBean(ThirdPartyResource.class));
			assertThat(resource.get().isStarted()).isTrue();
			assertThat(resource.get().isStopped()).isFalse();
		});
		assertThat(resource.get().isStopped()).isTrue();
	}
}