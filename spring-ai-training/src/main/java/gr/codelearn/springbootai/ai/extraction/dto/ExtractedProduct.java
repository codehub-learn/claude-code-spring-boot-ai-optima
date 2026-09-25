package gr.codelearn.springbootai.ai.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unit 8.3: the record the model's completion is mapped to. Two kinds of annotation, for two checks:
 *
 * <ul>
 *   <li>the <b>shape</b> (property names, types, the {@link Category} values, descriptions) goes into the JSON schema Spring AI generates
 *   from this record and sends to the model; {@code @JsonPropertyDescription} is how a field explains itself to the model;</li>
 *   <li>the <b>rules</b> (Jakarta Bean Validation) are checked in Java after mapping. The generated schema does not carry them, so the
 *   model never sees "positive" or "three capital letters": valid JSON can still break them.</li>
 * </ul>
 *
 * <p>A JSpecify {@code @Nullable} component is left out of the schema's {@code required} list, but its type stays non-null
 * ({@code "number"}, not {@code ["number", "null"]}): the model must <i>omit</i> an unknown value, an explicit {@code null} fails the
 * schema. And no springdoc {@code @Schema} here: Spring AI reads it too, and its default {@code requiredMode} makes the field required.
 *
 * @param name     the product's name
 * @param category the closest catalog category
 * @param price    the price, {@code null} when the text states none
 * @param currency ISO 4217 code of the price, {@code null} when there is no price
 * @param features the key features, as short phrases
 * @param inStock  availability, {@code null} when the text does not say
 */
public record ExtractedProduct(
		@JsonPropertyDescription("The product's name, as the text gives it") @NotBlank @Size(max = 200) String name,
		@JsonPropertyDescription("The closest category; OTHER when none fits") @NotNull Category category,
		@JsonPropertyDescription("The price as a plain number without a currency symbol; leave out when the text states no price")
		@Positive @Nullable BigDecimal price,
		@JsonPropertyDescription("The ISO 4217 code of the price's currency, e.g. EUR; leave out when there is no price")
		@Pattern(regexp = "[A-Z]{3}") @Nullable String currency,
		@JsonPropertyDescription("The key features as short phrases, at most 10; an empty list when none are stated")
		@NotNull @Size(max = 10) List<@NotBlank @Size(max = 200) String> features,
		@JsonPropertyDescription("true or false when the text says whether it is in stock; leave out otherwise")
		@Nullable Boolean inStock) {

	/**
	 * A cross-field rule the schema can't express: a price and its currency come together.
	 */
	@JsonIgnore
	@AssertTrue(message = "price and currency must be both present or both absent")
	public boolean isPriceWithCurrency() {
		return (price == null) == (currency == null);
	}

	/**
	 * The catalog categories the model must choose from; the schema lists them as an enum.
	 */
	public enum Category {
		AUDIO, COMPUTING, PHONES, HOME, OTHER
	}
}