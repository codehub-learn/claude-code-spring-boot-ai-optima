package gr.codelearn.springbootai.training.foundation.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * Lifecycle style 2: implementing a Spring callback interface directly, no annotations. Framework-coupled but refactor-safe: a rename
 * cannot silently drop the hook the way removing an easy-to-miss annotation could.
 */
@Component
class InterfaceLifecycleBean implements DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(InterfaceLifecycleBean.class);

	private boolean destroyed;

	@Override
	public void destroy() {
		destroyed = true;
		log.info("InterfaceLifecycleBean destroyed via DisposableBean");
	}

	boolean isDestroyed() {
		return destroyed;
	}
}