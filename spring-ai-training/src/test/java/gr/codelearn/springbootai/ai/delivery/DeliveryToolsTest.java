package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.delivery.dto.DeliveryEstimate;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Unit 11.3: the delivery tool's definition and input validation (11.2's mechanism on a second tool), without a model or a Spring context.
 */
class DeliveryToolsTest {

	private final DeliveryService deliveryService = mock(DeliveryService.class);
	private final ToolCallback tool = ToolCallbacks.from(validated(new DeliveryTools(deliveryService)))[0];

	private static DeliveryTools validated(final DeliveryTools target) {
		final ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(true);
		factory.addAdvice(new MethodValidationInterceptor(Validation.buildDefaultValidatorFactory()));
		return (DeliveryTools) factory.getProxy();
	}

	@Test
	void theToolTellsTheModelItsAnswerCanBeAnError() {
		assertThat(tool.getToolDefinition().name()).isEqualTo("estimateDelivery");
		assertThat(tool.getToolDefinition().description()).contains("can be unavailable").contains("do not make up an estimate");
	}

	@Test
	void aValidCallReachesTheServiceAndTheEstimateComesBackAsJson() {
		given(deliveryService.estimate("LAP-0002", "10431")).willReturn(
				new DeliveryEstimate("LAP-0002", "10431", 3, new BigDecimal("4.90"), "Stub Express"));

		assertThat(tool.call("{\"input\":{\"sku\":\"LAP-0002\",\"postcode\":\"10431\"}}")).contains("\"days\":3")
																						  .contains("\"carrier\":\"Stub Express\"");
	}

	@Test
	void aPostcodeThatIsNotFiveDigitsNeverReachesTheCarrier() {
		assertThatThrownBy(() -> tool.call("{\"input\":{\"sku\":\"LAP-0002\",\"postcode\":\"104 31\"}}"))
				.isInstanceOf(ToolExecutionException.class)
				.hasRootCauseInstanceOf(ConstraintViolationException.class)
				.rootCause()
				.hasMessageContaining("postcode: must be exactly five digits");

		then(deliveryService).shouldHaveNoInteractions();
	}

	@Test
	void argumentsThatAreNotTheToolsShapeNeverReachTheServiceEither() {
		for (final String call : new String[]{"not json", "{\"input\":\"a string, not an object\"}",
											  "{\"input\":{\"sku\":[\"LAP-0002\"],\"postcode\":\"10431\"}}"}) {
			assertThatThrownBy(() -> tool.call(call)).isInstanceOf(ToolExecutionException.class)
													 .hasRootCauseInstanceOf(JacksonException.class);
		}

		then(deliveryService).shouldHaveNoInteractions();
	}
}