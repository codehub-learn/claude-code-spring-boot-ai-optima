package gr.codelearn.springbootai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.template.ValidationMode;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit 8.1: how Spring AI's {@link PromptTemplate} fills a template, proven without any model. Templates use StringTemplate (ST) syntax
 * with {@code {}} delimiters by default, and Spring AI validates every render.
 *
 * <ul>
 *   <li>variables, a {@code toString()} of any value, from a string or a {@code Resource} ({@code .st} file);</li>
 *   <li>ST logic: optional parts ({@code if} / {@code else}) and lists;</li>
 *   <li>validation: every variable the template uses must be in the map, the optional ones included (as {@code null});</li>
 *   <li>pitfalls: ST's {@code separator} option is mistaken for a variable, literal braces (JSON) clash with the delimiters unless
 *   the renderer uses others, and line breaks come out in the platform's format;</li>
 *   <li>what the result can be: a {@code String}, a {@code Message}, or a whole {@link Prompt}.</li>
 * </ul>
 */
class PromptTemplatesTest {

	@Test
	void variablesAreReplacedByTheValuesToString() {
		final PromptTemplate template = new PromptTemplate("Summarize in at most {maxWords} words for {audience}.");

		assertThat(template.render(Map.of("maxWords", 50, "audience", "beginners"))).isEqualTo(
				"Summarize in at most 50 words for beginners.");
	}

	@Test
	void aTemplateIsUsuallyLoadedFromAResourceAndReusedWithDifferentValues() {
		final PromptTemplate template = PromptTemplate.builder().resource(resource("Translate to {language}: {text}")).build();

		assertThat(template.render(Map.of("language", "Greek", "text", "Good morning"))).isEqualTo("Translate to Greek: Good morning");
		assertThat(template.render(Map.of("language", "Italian", "text", "Thank you"))).isEqualTo("Translate to Italian: Thank you");
	}

	@Test
	void anOptionalPartIsLeftOutWhenItsValueIsNull() {
		final PromptTemplate template = new PromptTemplate("Summarize the text.{if(audience)} Write it for {audience}.{endif}");

		assertThat(template.render(values("audience", "managers"))).isEqualTo("Summarize the text. Write it for managers.");
		assertThat(template.render(values("audience", null))).isEqualTo("Summarize the text.");
	}

	@Test
	void pitfallAnOptionalVariableMustStillBeInTheMap() {
		// Spring AI's validation looks at which variables the template names, not at whether they sit inside an if. A missing key fails;
		// the key with a null value (a HashMap: Map.of rejects nulls) is what "absent" means.
		final PromptTemplate template = new PromptTemplate("Summarize the text.{if(audience)} Write it for {audience}.{endif}");

		assertThatThrownBy(() -> template.render(Map.of())).isInstanceOf(IllegalStateException.class)
														   .hasMessageContaining("Missing variable names are: [audience]");
	}

	@Test
	void aMissingVariableFailsTheRenderInsteadOfReachingTheModel() {
		final PromptTemplate template = new PromptTemplate("Summarize in at most {maxWords} words: {text}");

		assertThatThrownBy(() -> template.render(Map.of("text", "..."))).isInstanceOf(IllegalStateException.class)
																		.hasMessageContaining("[maxWords]");
	}

	@Test
	void aListIsRenderedOneLinePerElement() {
		// {list:{element | ...}} applies the inner template to every element. The newline inside it ends each bullet.
		final PromptTemplate template = new PromptTemplate("Key features:\n{features:{feature | - {feature}\n}}");

		assertThat(lf(template.render(Map.of("features", List.of("40-hour battery", "USB-C"))))).isEqualTo(
				"Key features:\n- 40-hour battery\n- USB-C\n");
	}

	@Test
	void pitfallLineBreaksComeOutInThePlatformsFormat() {
		// ST writes every line break, from the template and from the values, as System.lineSeparator(): \r\n on Windows, \n elsewhere.
		// Harmless for a model, but a test comparing rendered text must normalize it (see lf below).
		final PromptTemplate template = new PromptTemplate("Line one\nLine two: {value}");

		assertThat(template.render(Map.of("value", "a\nb"))).isEqualTo(
				"Line one" + System.lineSeparator() + "Line two: a" + System.lineSeparator() + "b");
	}

	@Test
	void pitfallStsSeparatorOptionIsMistakenForAMissingVariable() {
		// Plain ST would join the elements with the separator. Spring AI's validator reads "separator" as a template variable and fails;
		// the newline inside the element template above avoids the option altogether.
		final PromptTemplate template = new PromptTemplate("{features:{feature | - {feature}}; separator=\"\\n\"}");

		assertThatThrownBy(() -> template.render(Map.of("features", List.of("a", "b")))).isInstanceOf(IllegalStateException.class)
																						.hasMessageContaining("[separator]");
	}

	@Test
	void pitfallLiteralBracesClashWithTheDefaultDelimiters() {
		// A JSON example in the prompt text is parsed as ST syntax and the template is rejected.
		final PromptTemplate template = new PromptTemplate("Reply as JSON like {\"summary\": \"...\"} about {topic}.");

		assertThatThrownBy(() -> template.render(Map.of("topic", "Spring"))).isInstanceOf(IllegalArgumentException.class)
																			.hasMessageContaining("not valid");
	}

	@Test
	void customDelimitersLetTheTemplateContainBraces() {
		final PromptTemplate template = PromptTemplate.builder()
													  .renderer(StTemplateRenderer.builder()
																				  .startDelimiterToken('<')
																				  .endDelimiterToken('>')
																				  .build())
													  .template("Reply as JSON like {\"summary\": \"...\"} about <topic>.")
													  .build();

		assertThat(template.render(Map.of("topic", "Spring"))).isEqualTo("Reply as JSON like {\"summary\": \"...\"} about Spring.");
	}

	@Test
	void valuesAreInsertedAsTextNeverParsedAsTemplateSyntax() {
		// A user's text with braces or ST expressions can't break or extend the template (prompt injection is another matter, 8.2).
		final PromptTemplate template = new PromptTemplate("Summarize: {text}");

		assertThat(template.render(Map.of("text", "{maxWords} {if(x)}hi{endif} <b>"))).isEqualTo(
				"Summarize: {maxWords} {if(x)}hi{endif} <b>");
	}

	@Test
	void validationCanBeRelaxedButThenAMissingValueIsSilentlyEmpty() {
		final PromptTemplate template = PromptTemplate.builder()
													  .renderer(StTemplateRenderer.builder().validationMode(ValidationMode.NONE).build())
													  .template("Summarize in at most {maxWords} words.")
													  .build();

		assertThat(template.render(Map.of())).isEqualTo("Summarize in at most  words.");
	}

	@Test
	void aTemplateCanProduceAMessageOrAWholePrompt() {
		final Message user = new PromptTemplate("Explain {topic}.").createMessage(Map.of("topic", "records"));
		final Message system = new SystemPromptTemplate("You are a {role}.").createMessage(Map.of("role", "Java tutor"));
		final Prompt prompt = new PromptTemplate("Explain {topic}.").create(Map.of("topic", "sealed types"));

		assertThat(user.getMessageType()).isEqualTo(MessageType.USER);
		assertThat(user.getText()).isEqualTo("Explain records.");
		assertThat(system.getMessageType()).isEqualTo(MessageType.SYSTEM);
		assertThat(system.getText()).isEqualTo("You are a Java tutor.");
		assertThat(prompt.getUserMessage().getText()).isEqualTo("Explain sealed types.");
	}

	private static Map<String, Object> values(final String key, final Object value) {
		final Map<String, Object> values = new HashMap<>();
		values.put(key, value);
		return values;
	}

	/**
	 * ST writes line breaks as the platform's separator ({@code \r\n} on Windows); the assertions compare with {@code \n}.
	 */
	private static String lf(final String text) {
		return text.replace("\r\n", "\n");
	}

	private static ByteArrayResource resource(final String text) {
		return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
	}
}