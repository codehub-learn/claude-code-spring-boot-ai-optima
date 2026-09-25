package gr.codelearn.springbootai.ai.extraction;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.common.InvalidModelOutputException;
import gr.codelearn.springbootai.ai.common.InvalidModelOutputException.Problem;
import gr.codelearn.springbootai.ai.extraction.dto.ExtractedProduct;
import gr.codelearn.springbootai.ai.extraction.dto.ExtractedProduct.Category;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionRequest;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionRequest.OutputMode;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionResponse;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionResponse.TokenUsage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit 8.3 slice test for {@link ExtractionController}: the JSON contract, the output mode, the validation rules, and both 502 kinds.
 * {@link ExtractionService} is mocked.
 */
@WebMvcTest(ExtractionController.class)
class ExtractionControllerTest {

	private static final String EXTRACTIONS = "/api/v1/product-extractions";
	private static final ExtractedProduct PRODUCT = new ExtractedProduct("Aurora X1", Category.AUDIO, new BigDecimal("129.90"), "EUR",
																		 List.of("40-hour battery"), true);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ExtractionService extractionService;

	@Test
	void returnsTheTypedProduct() {
		given(extractionService.extract(new ProductExtractionRequest("Aurora X1 ...", null))).willReturn(
				new ProductExtractionResponse(PRODUCT, OutputMode.PROMPT, "claude-sonnet-5", new TokenUsage(400, 60, 460)));

		assertThat(mvc.post().uri(EXTRACTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																							   {"text": "Aurora X1 ..."}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"product": {"name": "Aurora X1", "category": "AUDIO", "price": 129.90, "currency": "EUR",
									             "features": ["40-hour battery"], "inStock": true},
									 "outputMode": "prompt", "model": "claude-sonnet-5"}""");
	}

	@Test
	void readsTheLowerCaseOutputMode() {
		given(extractionService.extract(new ProductExtractionRequest("Aurora X1 ...", OutputMode.NATIVE))).willReturn(
				new ProductExtractionResponse(PRODUCT, OutputMode.NATIVE, null, new TokenUsage(null, null, null)));

		assertThat(mvc.post().uri(EXTRACTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																							   {"text": "Aurora X1 ...", "outputMode": "native"}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.outputMode")
				.isEqualTo("native");
	}

	@Test
	void blankTextReturns400() {
		assertThat(mvc.post().uri(EXTRACTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																							   {"text": " "}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("text");
		verifyNoInteractions(extractionService);
	}

	@Test
	void unknownOutputModeReturns400() {
		assertThat(mvc.post().uri(EXTRACTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																							   {"text": "Aurora X1", "outputMode": "json"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(extractionService);
	}

	@Test
	void invalidModelOutputReturns502WithTheProblems() {
		given(extractionService.extract(any())).willThrow(new InvalidModelOutputException(
				"The chat model returned an unusable product listing", List.of(new Problem("price", "must be greater than 0")), null));

		assertThat(mvc.post().uri(EXTRACTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																							   {"text": "Aurora X1"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.bodyJson()
				.isLenientlyEqualTo("""
									{"status": 502, "errors": [{"field": "price", "message": "must be greater than 0"}]}""");
	}

	@Test
	void modelFailureReturns502() {
		given(extractionService.extract(any())).willThrow(new ChatModelUnavailableException("The chat model is unavailable", null));

		assertThat(mvc.post().uri(EXTRACTIONS).contentType(MediaType.APPLICATION_JSON).content("""
																							   {"text": "Aurora X1"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}
}