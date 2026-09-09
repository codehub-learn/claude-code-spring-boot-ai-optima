package gr.codelearn.springbootai.training.foundation.beans;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.2: {@code getBean("widgetFactoryBean")} returns the {@link FactoryBean#getObject() product}, and the {@code &}-prefixed name
 * reaches the factory itself.
 */
class WidgetFactoryBeanTest {
	private final ApplicationContextRunner runner =
			new ApplicationContextRunner().withUserConfiguration(WidgetFactoryBean.class);

	@Test
	void beanNameResolvesToTheProduct() {
		runner.run(context -> {
			Widget widget = context.getBean("widgetFactoryBean", Widget.class);
			assertThat(widget.origin()).isEqualTo("factory");
		});
	}

	@Test
	void ampersandPrefixReachesTheFactoryItself() {
		runner.run(context -> {
			Object raw = context.getBean("&widgetFactoryBean");
			assertThat(raw).isInstanceOf(FactoryBean.class);
			assertThat(((FactoryBean<?>) raw).getObjectType()).isEqualTo(Widget.class);
		});
	}
}