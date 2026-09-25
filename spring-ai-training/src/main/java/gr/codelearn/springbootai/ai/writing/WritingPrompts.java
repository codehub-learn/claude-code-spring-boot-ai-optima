package gr.codelearn.springbootai.ai.writing;

import gr.codelearn.springbootai.ai.writing.dto.ProductDescriptionRequest;
import gr.codelearn.springbootai.ai.writing.dto.SummaryRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Unit 8.1: one reusable {@link PromptTemplate} per use case, each loaded once from its {@code .st} file under {@code prompts/writing}. The
 * files hold the wording; this class only maps a request to the template's variables and renders it.
 *
 * <p>The templates use StringTemplate (ST) syntax with Spring AI's default {@code {}} delimiters:
 *
 * <ul>
 *   <li>{@code {name}}: a plain variable, replaced by the value's {@code toString()};</li>
 *   <li>{@code {if(audience)}...{endif}} and {@code {if(language)}...{else}...{endif}}: optional parts, left out when the value is
 *   {@code null};</li>
 *   <li>{@code {features:{feature | - {feature}\n}}}: a list, one bullet per element.</li>
 * </ul>
 *
 * <p>Spring AI checks every render: a variable the template uses must be in the map, or rendering fails with
 * {@code IllegalStateException} ("Not all variables were replaced"). That includes the optional ones, so they go in as {@code null}
 * values, which is why the maps are {@link HashMap}s ({@code Map.of} rejects nulls). Values are inserted as plain text and never parsed
 * as template syntax, so braces in a user's text can't break or alter the template.
 */
@Component
class WritingPrompts {

	private final PromptTemplate summary;
	private final PromptTemplate productDescription;

	WritingPrompts(@Value("classpath:prompts/writing/summary.st") final Resource summary,
				   @Value("classpath:prompts/writing/product-description.st") final Resource productDescription) {
		this.summary = new PromptTemplate(summary);
		this.productDescription = new PromptTemplate(productDescription);
	}

	String summary(final SummaryRequest request) {
		final Map<String, @Nullable Object> variables = new HashMap<>();
		variables.put("text", request.text());
		variables.put("maxWords", request.maxWords());
		variables.put("audience", request.audience());
		return summary.render(variables);
	}

	String productDescription(final ProductDescriptionRequest request) {
		final Map<String, @Nullable Object> variables = new HashMap<>();
		variables.put("name", request.name());
		variables.put("category", request.category());
		variables.put("features", request.features());
		variables.put("tone", request.tone().name().toLowerCase(Locale.ROOT));
		variables.put("language", request.language());
		return productDescription.render(variables);
	}
}