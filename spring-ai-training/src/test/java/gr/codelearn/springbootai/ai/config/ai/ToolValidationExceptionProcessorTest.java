package gr.codelearn.springbootai.ai.config.ai;

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

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 11.2: the message a model gets when a tool rejects its arguments, and that every other failure keeps Spring AI's default (its own
 * message). The violations are the real ones from the input records' constraints.
 */
class ToolValidationExceptionProcessorTest {

	private static final ToolDefinition TOOL = DefaultToolDefinition.builder()
	                                                                .name("getProduct")
	                                                                .description("d")
	                                                                .inputSchema("{}")
	                                                                .build();

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	private final ToolValidationExceptionProcessor processor = new ToolValidationExceptionProcessor();

	@Test
	void aViolationBecomesAMessageThatSaysWhatToFix() {
		final var violations = validator.validate(new ProductLookupInput("the blue laptop"));

		assertThat(processor.process(new ToolExecutionException(TOOL, new ConstraintViolationException(violations)))).isEqualTo(
				"Invalid arguments, the tool did not run: sku must be letters, a dash and digits, like LAP-0002. "
				+ "Fix the arguments and call the tool again.");
	}

	@Test
	void everyViolationIsListedInAStableOrder() {
		final Set<ConstraintViolation<ProductSearchInput>> violations =
				validator.validate(new ProductSearchInput("x".repeat(61), new BigDecimal("-1")));

		assertThat(processor.process(new ToolExecutionException(TOOL, new ConstraintViolationException(violations))))
				.startsWith("Invalid arguments, the tool did not run: keywords must be at most 60 characters")
				.contains("; maxPrice must be greater than 0");
	}

	@Test
	void anyOtherFailureKeepsTheDefaultMessage() {
		assertThat(processor.process(new ToolExecutionException(TOOL, new IllegalStateException("No product with SKU AUD-9999"))))
				.isEqualTo("No product with SKU AUD-9999");
	}
}