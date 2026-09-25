package gr.codelearn.springbootai.ai.writing;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.writing.dto.ProductDescriptionRequest;
import gr.codelearn.springbootai.ai.writing.dto.ProductDescriptionRequest.Tone;
import gr.codelearn.springbootai.ai.writing.dto.SummaryRequest;
import gr.codelearn.springbootai.ai.writing.dto.WritingResponse;
import gr.codelearn.springbootai.ai.writing.dto.WritingResponse.TokenUsage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit 8.1 slice test for {@link WritingController}: the JSON contract of both use cases, their validation rules, and the 502 mapping.
 * {@link WritingService} is mocked.
 */
@WebMvcTest(WritingController.class)
class WritingControllerTest {

	private static final String SUMMARIES = "/api/v1/summaries";
	private static final String PRODUCT_DESCRIPTIONS = "/api/v1/product-descriptions";
	private static final WritingResponse RESPONSE = new WritingResponse("Done.", "claude-sonnet-5", new TokenUsage(80, 20, 100));

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private WritingService writingService;

	@Test
	void summaryReturnsTheGeneratedText() {
		given(writingService.summarize(new SummaryRequest("Long text.", 50, null))).willReturn(RESPONSE);

		assertThat(mvc.post().uri(SUMMARIES).contentType(MediaType.APPLICATION_JSON).content("""
																							 {"text": "Long text.", "maxWords": 50}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content")
				.isEqualTo("Done.");
	}

	@Test
	void summaryWordLimitOutsideTheRangeReturns400() {
		assertThat(mvc.post().uri(SUMMARIES).contentType(MediaType.APPLICATION_JSON).content("""
																							 {"text": "Long text.", "maxWords": 5}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("maxWords");
		verifyNoInteractions(writingService);
	}

	@Test
	void summaryWithoutTextReturns400() {
		assertThat(mvc.post().uri(SUMMARIES).contentType(MediaType.APPLICATION_JSON).content("""
																							 {"text": " ", "maxWords": 50}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("text");
		verifyNoInteractions(writingService);
	}

	@Test
	void productDescriptionReadsLowerCaseTone() {
		given(writingService.describeProduct(new ProductDescriptionRequest("Aurora X1", "Audio", List.of("USB-C"), Tone.PLAYFUL, "Greek")))
				.willReturn(RESPONSE);

		assertThat(mvc.post().uri(PRODUCT_DESCRIPTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																										{"name": "Aurora X1", "category": "Audio", "features": ["USB-C"],
																										 "tone": "playful", "language": "Greek"}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content")
				.isEqualTo("Done.");
	}

	@Test
	void productDescriptionWithoutFeaturesReturns400() {
		assertThat(mvc.post().uri(PRODUCT_DESCRIPTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																										{"name": "Aurora X1", "category": "Audio", "features": [],
																										 "tone": "friendly"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("features");
		verifyNoInteractions(writingService);
	}

	@Test
	void productDescriptionWithAnUnknownToneReturns400() {
		assertThat(mvc.post().uri(PRODUCT_DESCRIPTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																										{"name": "Aurora X1", "category": "Audio", "features": ["USB-C"],
																										 "tone": "sarcastic"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(writingService);
	}

	@Test
	void modelFailureReturns502() {
		given(writingService.summarize(any())).willThrow(new ChatModelUnavailableException("The chat model is unavailable, try again " +
																						   "later",
																						   null));

		assertThat(mvc.post().uri(SUMMARIES).contentType(MediaType.APPLICATION_JSON).content("""
																							 {"text": "Long text.", "maxWords": 50}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}
}