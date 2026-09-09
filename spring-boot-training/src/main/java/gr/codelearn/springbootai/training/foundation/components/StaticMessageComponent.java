package gr.codelearn.springbootai.training.foundation.components;

import org.springframework.stereotype.Component;

/**
 * The plain stereotype: {@code @Component} is the generic "container-managed bean" marker that every other stereotype specializes.
 */
@Component
class StaticMessageComponent implements MessageProvider {

	@Override
	public String stereotype() {
		return "component";
	}

	@Override
	public String message() {
		return "message from a plain @Component";
	}
}