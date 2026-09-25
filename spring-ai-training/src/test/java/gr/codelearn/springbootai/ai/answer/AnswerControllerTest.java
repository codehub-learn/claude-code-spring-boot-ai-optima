package gr.codelearn.springbootai.ai.answer;

import gr.codelearn.springbootai.ai.answer.dto.AnswerRequest;
import gr.codelearn.springbootai.ai.answer.dto.AnswerRequest.Document;
import gr.codelearn.springbootai.ai.answer.dto.AnswerResponse;
import gr.codelearn.springbootai.ai.answer.dto.AnswerResponse.TokenUsage;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;

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
 * Unit 8.2 slice test for {@link AnswerController}: the JSON contract with and without documents, the validation rules, and the 502
 * mapping. {@link AnswerService} is mocked.
 */
@WebMvcTest(AnswerController.class)
class AnswerControllerTest {

	private static final String ANSWERS = "/api/v1/answers";
	private static final AnswerResponse RESPONSE = new AnswerResponse("Two years [1].", "claude-sonnet-5", new TokenUsage(120, 8, 128));

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private AnswerService answerService;

	@Test
	void questionWithoutDocumentsIsAccepted() {
		given(answerService.answer(new AnswerRequest("What is Spring AI?", null))).willReturn(RESPONSE);

		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": "What is Spring AI?"}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content")
				.isEqualTo("Two years [1].");
	}

	@Test
	void questionWithDocumentsIsPassedOnInOrder() {
		given(answerService.answer(new AnswerRequest("Warranty?", List.of(new Document("Warranty", "2 years."),
																		  new Document("Shipping", "3 days."))))).willReturn(RESPONSE);

		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": "Warranty?", "documents": [
																						     {"title": "Warranty", "text": "2 years."},
																						     {"title": "Shipping", "text": "3 days."}]}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.usage.promptTokens")
				.convertTo(Integer.class)
				.isEqualTo(120);
	}

	@Test
	void blankQuestionReturns400() {
		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": " "}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("question");
		verifyNoInteractions(answerService);
	}

	@Test
	void blankDocumentTextReturns400WithTheElementsPath() {
		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": "Warranty?",
																						    "documents": [{"title": "Warranty", "text": ""}]}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("documents[0].text");
		verifyNoInteractions(answerService);
	}

	@Test
	void moreThanFiveDocumentsReturns400() {
		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": "Warranty?", "documents": [
																						     {"title": "1", "text": "a"}, {"title": "2", "text": "b"},
																						     {"title": "3", "text": "c"}, {"title": "4", "text": "d"},
																						     {"title": "5", "text": "e"}, {"title": "6", "text": "f"}]}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("documents");
		verifyNoInteractions(answerService);
	}

	@Test
	void modelFailureReturns502() {
		given(answerService.answer(any())).willThrow(new ChatModelUnavailableException("The chat model is unavailable, try again later",
																					   null));

		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": "What is Spring AI?"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}
}