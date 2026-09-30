package gr.codelearn.springbootai.ai.config.ai;

import gr.codelearn.springbootai.ai.common.ExternalServiceException;
import gr.codelearn.springbootai.ai.common.ExternalServiceException.Kind;
import gr.codelearn.springbootai.ai.common.ModelReadableException;
import gr.codelearn.springbootai.ai.product.dto.ProductLookupInput;
import gr.codelearn.springbootai.ai.product.dto.ProductSearchInput;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Units 11.2 and 11.3: the text a model gets for each kind of tool failure. Validation violations are the real ones from the input records'
 * constraints; every failure is wrapped in a {@link ToolExecutionException}, as Spring AI does.
 */
class ToolErrorProcessorTest {

	private static final ToolDefinition TOOL = DefaultToolDefinition.builder()
																	.name("estimateDelivery")
																	.description("d")
																	.inputSchema("{}")
																	.build();

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	private final ToolErrorProcessor processor = new ToolErrorProcessor();

	@Test
	void aViolationBecomesAMessageThatSaysWhatToFix() {
		final var violations = validator.validate(new ProductLookupInput("the blue laptop"));

		assertThat(processor.process(wrapped(new ConstraintViolationException(violations)))).isEqualTo(
				"Invalid arguments, the tool did not run: sku must be letters, a dash and digits, like LAP-0002. "
				+ "Fix the arguments and call the tool again.");
	}

	@Test
	void everyViolationIsListedInAStableOrder() {
		final Set<ConstraintViolation<ProductSearchInput>> violations =
				validator.validate(new ProductSearchInput("x".repeat(61), new BigDecimal("-1")));

		assertThat(processor.process(wrapped(new ConstraintViolationException(violations))))
				.startsWith("Invalid arguments, the tool did not run: keywords must be at most 60 characters")
				.contains("; maxPrice must be greater than 0");
	}

	@Test
	void anExternalFailureSaysWhatFailedAndWhatToDoAboutIt() {
		assertThat(processor.process(
				wrapped(new ExternalServiceException(Kind.UNAVAILABLE, "The delivery service is not responding (3 attempts)",
													 new IllegalStateException("connect timed out to 10.1.2.3"))))).isEqualTo(
				"The delivery service is not responding (3 attempts). " + Kind.UNAVAILABLE.advice());
		assertThat(processor.process(wrapped(new ExternalServiceException(Kind.RATE_LIMITED, "The delivery service is busy", null))))
				.isEqualTo("The delivery service is busy. Do not retry now. Tell the customer to ask again in a few minutes.");
		assertThat(processor.process(
				wrapped(new ExternalServiceException(Kind.REJECTED, "The carrier does not deliver to postcode 40404", null))))
				.contains("does not deliver to postcode 40404").contains("Retrying with the same input will not help");
	}

	@Test
	void theAdviceOfEachKindTellsTheModelNotToGuess() {
		assertThat(Kind.UNAVAILABLE.advice()).contains("Do not retry").contains("do not guess");
		assertThat(Kind.RATE_LIMITED.advice()).contains("Do not retry now");
		assertThat(Kind.REJECTED.advice()).contains("will not help");
	}

	@Test
	void argumentsThatAreNotJsonOfTheToolsShapeGetAMessageWithoutJacksonsDetail() {
		final var failure = assertThatThrownBy(() -> JsonMapper.shared().readValue("\"a string, not an object\"",
																				   ProductLookupInput.class))
				.actual();

		assertThat(processor.process(wrapped(failure))).isEqualTo(ToolErrorProcessor.MALFORMED)
													   .doesNotContain("ProductLookupInput")
													   .contains("Fix the arguments and call the tool again");
	}

	@Test
	void aModelReadableExceptionKeepsItsOwnMessage() {
		assertThat(processor.process(wrapped(new NoSuchThing("No product with SKU AUD-9999")))).isEqualTo("No product with SKU AUD-9999");
	}

	@Test
	void theFailureIsFoundThroughTheCausesSpringAiWrapsItIn() {
		final var nested = new RuntimeException("invocation failed",
		                                        new RuntimeException("wrapper", new NoSuchThing("No product with SKU X-1")));

		assertThat(processor.process(wrapped(nested))).isEqualTo("No product with SKU X-1");
	}

	@Test
	void anUnexpectedExceptionNeverLeaksItsMessageToTheModel() {
		final String leaky = "ORA-00942: table or view does not exist at jdbc:oracle:thin:@db.internal:1521/prod";

		final String message = processor.process(wrapped(new IllegalStateException(leaky)));

		assertThat(message).isEqualTo(ToolErrorProcessor.UNEXPECTED_MESSAGE).doesNotContain("ORA-00942").doesNotContain("db.internal");
		assertThat(message).contains("Do not guess");
	}

	@Test
	void describeGivesTheSameTextWithoutNeedingAToolExecutionException() {
		assertThat(processor.describe(new NoSuchThing("No product with SKU AUD-9999"))).isEqualTo("No product with SKU AUD-9999");
		assertThat(processor.describe(new IllegalStateException("secret"))).isEqualTo(ToolErrorProcessor.UNEXPECTED_MESSAGE);
	}

	private static ToolExecutionException wrapped(final Throwable cause) {
		return new ToolExecutionException(TOOL, cause);
	}

	private static final class NoSuchThing extends RuntimeException implements ModelReadableException {

		NoSuchThing(final String message) {
			super(message);
		}
	}
}