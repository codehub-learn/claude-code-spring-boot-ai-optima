package gr.codelearn.springbootai.ai;

import gr.codelearn.springbootai.ai.extraction.dto.ExtractedProduct;
import gr.codelearn.springbootai.ai.extraction.dto.ExtractedProduct.Category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit 8.3: how Spring AI 2.0.1 maps a completion to a record, proven with a scripted stub model (no provider, no Spring context).
 *
 * <ul>
 *   <li>the JSON schema {@link BeanOutputConverter} generates from a record, and what decides "required";</li>
 *   <li>the two ways it reaches the model: format instructions in the user message, or the provider's structured output option;</li>
 *   <li>{@link StructuredOutputValidationAdvisor}: re-prompting with the schema error, and giving up silently;</li>
 *   <li>what the converter forgives (code fences, thinking tags) and what only Jakarta Bean Validation catches.</li>
 * </ul>
 */
class StructuredOutputTest {

	private static final String VALID = """
										{"name": "Aurora X1", "category": "AUDIO", "price": 129.90, "currency": "EUR",
										 "features": ["40-hour battery"], "inStock": true}""";

	private final BeanOutputConverter<ExtractedProduct> converter = new BeanOutputConverter<>(ExtractedProduct.class);

	@Test
	void theSchemaComesFromTheRecord() {
		assertThat(converter.getJsonSchema()).contains("\"enum\" : [ \"AUDIO\", \"COMPUTING\", \"PHONES\", \"HOME\", \"OTHER\" ]")
											 .contains("\"description\" : \"The closest category; OTHER when none fits\"")
											 .contains("\"additionalProperties\" : false")
											 .doesNotContain("priceWithCurrency");
	}

	@Test
	void nullableComponentsAreOptionalButNotNullTyped() {
		// JSpecify @Nullable drops the field from "required"; its type stays "number", so the model must leave it out, not send null.
		assertThat(converter.getJsonSchema()).contains("\"required\" : [ \"category\", \"features\", \"name\" ]")
											 .contains("\"type\" : \"number\"")
											 .doesNotContain("\"null\"");
	}

	@Test
	void pitfallASpringdocSchemaAnnotationMakesANullableFieldRequired() {
		// Spring AI reads @io.swagger...Schema too, and its default requiredMode (AUTO) counts as required, before JSpecify is looked at.
		record WithSchema(String name, @Schema(example = "9.99") @Nullable BigDecimal price) {
		}
		record WithoutSchema(String name, @Nullable BigDecimal price) {
		}

		assertThat(new BeanOutputConverter<>(WithSchema.class).getJsonSchema()).contains("\"required\" : [ \"name\", \"price\" ]");
		assertThat(new BeanOutputConverter<>(WithoutSchema.class).getJsonSchema()).contains("\"required\" : [ \"name\" ]");
	}

	@Test
	void promptModeAppendsTheSchemaToTheUserMessage() {
		final ScriptedModel model = new ScriptedModel(false, VALID);

		final ExtractedProduct product = ChatClient.create(model).prompt().user("Extract it.").call().entity(ExtractedProduct.class);

		assertThat(product).isEqualTo(new ExtractedProduct("Aurora X1", Category.AUDIO, new BigDecimal("129.90"), "EUR",
														   List.of("40-hour battery"), true));
		assertThat(model.userMessages().getFirst()).startsWith("Extract it.")
												   .contains("Your response should be in JSON format.", "\"enum\" : [ \"AUDIO\"");
		assertThat(model.outputSchemas().getFirst()).isNull();
	}

	@Test
	void nativeModeSendsTheSchemaAsAnOptionInstead() {
		final ScriptedModel model = new ScriptedModel(true, VALID);

		ChatClient.create(model).prompt().user("Extract it.").call().entity(ExtractedProduct.class,
		                                                                    spec -> spec.useProviderStructuredOutput());

		assertThat(model.userMessages().getFirst()).isEqualTo("Extract it.");
		assertThat(model.outputSchemas().getFirst()).contains("\"enum\" : [ \"AUDIO\"");
	}

	@Test
	void pitfallNativeModeFallsBackToPromptModeSilentlyWhenTheModelCantDoIt() {
		// The options of a model without structured output support are not StructuredOutputChatOptions: no error, no warning.
		final ScriptedModel model = new ScriptedModel(false, VALID);

		ChatClient.create(model).prompt().user("Extract it.").call().entity(ExtractedProduct.class,
		                                                                    spec -> spec.useProviderStructuredOutput());

		assertThat(model.userMessages().getFirst()).contains("Your response should be in JSON format.");
	}

	@Test
	void theConverterStripsCodeFencesAndThinkingTags() {
		final String json = """
							{"name": "A", "category": "HOME", "features": []}""";

		assertThat(converter.convert("```json\n" + json + "\n```")).isEqualTo(converter.convert("<think>hmm</think>" + json))
																   .extracting(ExtractedProduct::price)
																   .isNull();
	}

	@Test
	void theValidationAdvisorRepromptsWithTheSchemaError() {
		// An explicit null for a nullable number breaks the schema ("number" only), so it costs a second call.
		final ScriptedModel model = new ScriptedModel(false, """
															 {"name": "A", "category": "HOME", "price": null, "features": []}""", """
																																  {"name": "A", "category": "HOME", "features": []}""");

		final ExtractedProduct product = ChatClient.create(model)
												   .prompt()
												   .user("Extract it.")
												   .advisors(advisor(2))
												   .call()
												   .entity(converter);

		assertThat(product).isNotNull().extracting(ExtractedProduct::price).isNull();
		assertThat(model.userMessages()).hasSize(2);
		// The feedback is appended to the original user message (the format instructions follow it). It does not name the field.
		assertThat(model.userMessages().get(1)).startsWith("Extract it." + System.lineSeparator()
														   + "Output JSON validation failed because of: null found, number expected"
														   + System.lineSeparator() + "Your response should be in JSON format.");
	}

	@Test
	void theValidationAdvisorGivesUpSilentlyAndTheConverterThrows() {
		final ScriptedModel model = new ScriptedModel(false, "Sorry, no JSON.", "Still none.", "Nope.");

		assertThatThrownBy(() -> ChatClient.create(model).prompt().user("Extract it.").advisors(advisor(2)).call().entity(converter))
				.isInstanceOf(JacksonException.class);
		assertThat(model.userMessages()).hasSize(3);
	}

	@Test
	void validJsonCanStillBreakTheRulesOnlyJakartaKnows() {
		// Schema-valid: every type is right. Jakarta: a negative price, a lower-case currency.
		final ExtractedProduct product = converter.convert("""
														   {"name": "A", "category": "HOME", "price": -5, "currency": "eur", "features": []}""");
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

		assertThat(validator.validate(product)).extracting(violation -> violation.getPropertyPath().toString())
											   .containsExactlyInAnyOrder("price", "currency");
		final Set<ConstraintViolation<ExtractedProduct>> half = validator.validate(
				converter.convert("{\"name\": \"A\", \"category\": \"HOME\", \"price\": 5, \"features\": []}"));
		assertThat(half).extracting(ConstraintViolation::getMessage).containsExactly(
				"price and currency must be both present or both absent");
	}

	private StructuredOutputValidationAdvisor advisor(final int repairAttempts) {
		return StructuredOutputValidationAdvisor.builder()
		                                        .outputJsonSchema(converter.getJsonSchema())
		                                        .maxRepeatAttempts(repairAttempts)
		                                        .build();
	}

	/**
	 * Replies with the scripted texts in order and records each prompt's user message and native output schema. Its options support native
	 * structured output only when asked to.
	 */
	private static final class ScriptedModel implements ChatModel {

		private final boolean nativeSupport;
		private final Deque<String> replies;
		private final List<String> userMessages = new ArrayList<>();
		private final List<@Nullable String> outputSchemas = new ArrayList<>();

		ScriptedModel(final boolean nativeSupport, final String... replies) {
			this.nativeSupport = nativeSupport;
			this.replies = new ArrayDeque<>(List.of(replies));
		}

		@Override
		public ChatResponse call(final Prompt prompt) {
			userMessages.add(prompt.getUserMessage().getText());
			outputSchemas.add(prompt.getOptions() instanceof StructuredOutputChatOptions options ? options.getOutputSchema() : null);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(replies.pop()))));
		}

		@Override
		public ChatOptions getOptions() {
			return nativeSupport ? StructuredOutputChatOptions.builder().build() : ChatOptions.builder().build();
		}

		List<String> userMessages() {
			return userMessages;
		}

		List<@Nullable String> outputSchemas() {
			return outputSchemas;
		}
	}
}