package gr.codelearn.springbootai.ai.extraction;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.common.InvalidModelOutputException;
import gr.codelearn.springbootai.ai.common.InvalidModelOutputException.Problem;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.extraction.dto.ExtractedProduct;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionRequest;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionRequest.OutputMode;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionResponse;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionResponse.TokenUsage;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Unit 8.3: a completion mapped straight to a record, in three layers of defence:
 *
 * <ol>
 *   <li><b>Schema in.</b> {@link BeanOutputConverter} generates a JSON schema from {@link ExtractedProduct}. In {@code prompt} mode it is
 *   appended to the user message as format instructions; in {@code native} mode it is sent as the provider's structured output
 *   option instead ({@code useProviderStructuredOutput()}).</li>
 *   <li><b>Schema out, with retry.</b> {@link StructuredOutputValidationAdvisor} validates the reply against the same schema and, when it
 *   fails, calls the model again with the validation error appended to the user message, at most {@value #REPAIR_ATTEMPTS} more
 *   times. It gives up silently: the last reply is returned as is. Its schema also accepts {@code null} for optional properties
 *   (see {@link #allowNullForOptionalProperties}).</li>
 *   <li><b>Rules.</b> The converter maps the JSON to the record, then Jakarta Bean Validation checks what the schema can't say (a
 *   positive price, an ISO currency code, price and currency together).</li>
 * </ol>
 *
 * <p>Output that survives none of that becomes {@link InvalidModelOutputException} (502); a failed call stays
 * {@link ChatModelUnavailableException} (502). No {@code @Transactional}: nothing is persisted.
 */
@Service
class ExtractionService {

	/**
	 * Extra calls after the first when the reply breaks the schema. Each one costs a full round trip.
	 */
	static final int REPAIR_ATTEMPTS = 2;

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String UNAVAILABLE = "The chat model is unavailable, try again later";
	private static final String INVALID = "The chat model returned an unusable product listing";

	private final ChatClient chatClient;
	private final Validator validator;
	private final PromptTemplate productTemplate;
	private final BeanOutputConverter<ExtractedProduct> converter = new BeanOutputConverter<>(ExtractedProduct.class);
	private final StructuredOutputValidationAdvisor validationAdvisor;

	ExtractionService(final ChatClient chatClient, final Validator validator,
					  @Value("classpath:prompts/extraction/product.st") final Resource productTemplate) {
		this.chatClient = chatClient;
		this.validator = validator;
		this.productTemplate = new PromptTemplate(productTemplate);
		final String validationSchema = allowNullForOptionalProperties(converter.getJsonSchema());
		this.validationAdvisor = StructuredOutputValidationAdvisor.builder()
																  .outputJsonSchema(validationSchema)
																  .maxRepeatAttempts(REPAIR_ATTEMPTS)
																  .build();
	}

	ProductExtractionResponse extract(final ProductExtractionRequest request) {
		final OutputMode mode = Objects.requireNonNullElse(request.outputMode(), OutputMode.PROMPT);
		final ResponseEntity<ChatResponse, ExtractedProduct> result = call(productTemplate.render(Map.of("text", request.text())), mode);
		final ExtractedProduct product = result.entity();
		final ChatResponse response = result.response();
		if (product == null || response == null) {
			throw new InvalidModelOutputException(INVALID, List.of(new Problem("$", "no output")), null);
		}
		validate(product);
		final Usage usage = response.getMetadata().getUsage();
		return new ProductExtractionResponse(product, mode, response.getMetadata().getModel(),
											 new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}

	/**
	 * The validation advisor runs per call, next to the central client's default advisors; the converter's schema is the one it checks.
	 */
	private ResponseEntity<ChatResponse, ExtractedProduct> call(final String userMessage, final OutputMode mode) {
		try {
			return chatClient.prompt()
							 .system(SystemPromptParams::fill)
							 .user(userMessage)
							 .advisors(validationAdvisor)
							 .call()
							 .responseEntity(converter, spec -> {
								 if (mode == OutputMode.NATIVE) {
									 spec.useProviderStructuredOutput();
								 }
							 });
		} catch (JacksonException ex) {
			// The repair attempts ran out and the last reply still isn't JSON for the record.
			throw new InvalidModelOutputException(INVALID, List.of(new Problem("$", ex.getOriginalMessage())), ex);
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException(UNAVAILABLE, ex);
		}
	}

	/**
	 * The generated schema leaves a {@code @Nullable} component out of {@code required} but keeps its type non-null ({@code "number"}), so
	 * a reply with {@code "price": null} fails validation although the record accepts it, and every such reply costs a repair call
	 * (measured on {@code qwen3.5:4b}: three calls for one product, all rejected for the nulls). The validation schema therefore also
	 * allows {@code null} for every optional property; the schema the model is shown stays as generated.
	 */
	static String allowNullForOptionalProperties(final String schema) {
		final ObjectNode root = (ObjectNode) JSON.readTree(schema);
		final Set<String> required = new HashSet<>();
		root.path("required").forEach(name -> required.add(name.asString()));
		root.path("properties").properties().forEach(property -> {
			if (!required.contains(property.getKey()) && property.getValue() instanceof ObjectNode definition
				&& definition.get("type") instanceof StringNode type) {
				definition.putArray("type").add(type.asString()).add("null");
			}
		});
		return JSON.writeValueAsString(root);
	}

	private void validate(final ExtractedProduct product) {
		final Set<ConstraintViolation<ExtractedProduct>> violations = validator.validate(product);
		if (!violations.isEmpty()) {
			throw new InvalidModelOutputException(INVALID, violations.stream()
																	 .map(v -> new Problem(v.getPropertyPath().toString(), v.getMessage()))
																	 .sorted(Comparator.comparing(Problem::field))
																	 .toList(), null);
		}
	}
}