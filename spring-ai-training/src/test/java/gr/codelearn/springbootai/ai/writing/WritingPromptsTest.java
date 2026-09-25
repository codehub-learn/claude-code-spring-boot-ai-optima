package gr.codelearn.springbootai.ai.writing;

import gr.codelearn.springbootai.ai.writing.dto.ProductDescriptionRequest;
import gr.codelearn.springbootai.ai.writing.dto.ProductDescriptionRequest.Tone;
import gr.codelearn.springbootai.ai.writing.dto.SummaryRequest;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 8.1: the real {@code prompts/writing/*.st} templates, rendered from requests by {@link WritingPrompts}. No Spring context and no
 * model: this is the exact text the model will receive as the user message (with the platform's line breaks, see {@link #lf}).
 */
class WritingPromptsTest {

	private final WritingPrompts prompts = new WritingPrompts(new ClassPathResource("prompts/writing/summary.st"),
															  new ClassPathResource("prompts/writing/product-description.st"));

	@Test
	void summaryPutsTheLimitAndTheAudienceIntoTheInstructions() {
		final String prompt = lf(
				prompts.summary(new SummaryRequest("Spring Boot 4 ships on Spring Framework 7.", 40, "junior developers")));

		assertThat(prompt).startsWith("Summarize the text below in at most 40 words.")
						  .contains("Write the summary for this audience: junior developers.")
						  .endsWith("\"\"\"\nSpring Boot 4 ships on Spring Framework 7.\n\"\"\"");
	}

	@Test
	void summaryWithoutAnAudienceLeavesThatSentenceOut() {
		final String prompt = lf(prompts.summary(new SummaryRequest("Some text.", 40, null)));

		assertThat(prompt).doesNotContain("audience").contains("at most 40 words.\nKeep to the facts");
	}

	@Test
	void summaryKeepsBracesInTheUsersTextAsTheyAre() {
		final String prompt = lf(prompts.summary(new SummaryRequest("Use {maxWords} and {if(x)} literally.", 40, null)));

		assertThat(prompt).contains("Use {maxWords} and {if(x)} literally.");
	}

	@Test
	void productDescriptionListsEveryFeatureAndDefaultsToEnglish() {
		final String prompt = lf(prompts.productDescription(new ProductDescriptionRequest("Aurora X1", "Audio",
																						  List.of("40-hour battery",
		                                                                                          "active noise cancelling"),
																						  Tone.FRIENDLY, null)));

		assertThat(prompt).contains("Product: Aurora X1\nCategory: Audio\n")
						  .contains("Key features:\n- 40-hour battery\n- active noise cancelling\n")
						  .contains("Tone: friendly.")
						  .contains("Language: English.");
	}

	@Test
	void productDescriptionUsesTheRequestedLanguage() {
		final String prompt = lf(prompts.productDescription(new ProductDescriptionRequest("Aurora X1", "Audio", List.of("USB-C"),
																						  Tone.PROFESSIONAL, "Greek")));

		assertThat(prompt).contains("Tone: professional.").contains("Language: Greek.");
	}

	/**
	 * ST writes line breaks as the platform's separator ({@code \r\n} on Windows); the assertions compare with {@code \n}.
	 */
	private static String lf(final String text) {
		return text.replace("\r\n", "\n");
	}
}