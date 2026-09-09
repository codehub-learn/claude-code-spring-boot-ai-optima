package gr.codelearn.springbootai.training.foundation.beans;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * {@code FactoryBean<Widget>}: reach for this when *building* the object needs logic beyond a constructor call (a fluent builder, a lookup,
 * ...). Spring registers the bean name (the default {@code @Component} name, {@code widgetFactoryBean}) to the
 * {@linkplain #getObject() product}; prefix it with {@code &} to reach the factory itself.
 */
@Component
class WidgetFactoryBean implements FactoryBean<Widget> {

	@Override
	public Widget getObject() {
		return new Widget("factory");
	}

	@Override
	public Class<?> getObjectType() {
		return Widget.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}