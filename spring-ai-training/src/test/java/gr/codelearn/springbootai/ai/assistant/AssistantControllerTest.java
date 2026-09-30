package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantRequest;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.TokenUsage;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.conversation.ConversationNotFoundException;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit 11.1 slice test for {@link AssistantController}: the JSON contract, validation, the 404 and 502 mappings, and which conversation a
 * message continues (body id, then cookie, then a new one). The service is mocked.
 */
@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

	private static final UUID CONVERSATION = UUID.fromString("0b7f3c1e-6c1e-4a52-9d0a-1f8f0c7a9e10");

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private AssistantService assistantService;

	@Test
	void theAnswerComesWithTheToolCallsAndTheUsage() {
		given(assistantService.answer(new AssistantRequest("What does LAP-0002 cost?", null)))
				.willReturn(new AssistantResponse(CONVERSATION, "2199.00 EUR [LAP-0002].",
												  List.of(new ToolCall("getProduct", "{\"input\":{\"sku\":\"LAP-0002\"}}", 180, 6, null)),
												  "qwen3.5:4b", new TokenUsage(1652, 60, 1712)));

		assertThat(post("{\"question\": \"What does LAP-0002 cost?\"}")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																													 {"conversationId": "%s", "content": "2199.00 EUR [LAP-0002].",
																													  "toolCalls": [{"name": "getProduct", "arguments": "{\\"input\\":{\\"sku\\":\\"LAP-0002\\"}}", "resultChars": 180, "error": null}],
																													  "model": "qwen3.5:4b", "usage": {"totalTokens": 1712}}""".formatted(
				CONVERSATION));
	}

	@Test
	void aConversationIdIsPassedOnAndAnUnknownOneIsA404() {
		given(assistantService.answer(new AssistantRequest("And the price?", CONVERSATION))).willReturn(
				new AssistantResponse(CONVERSATION, "2199.00 EUR.", List.of(), "qwen3.5:4b", new TokenUsage(100, 10, 110)));
		final UUID unknown = UUID.randomUUID();
		given(assistantService.answer(new AssistantRequest("Hello again", unknown))).willThrow(new ConversationNotFoundException(unknown));

		assertThat(post("{\"question\": \"And the price?\", \"conversationId\": \"%s\"}".formatted(CONVERSATION))).hasStatusOk()
																												  .bodyJson()
																												  .extractingPath(
																														  "$" +
																														  ".conversationId")
																												  .isEqualTo(
																														  CONVERSATION.toString());
		assertThat(post("{\"question\": \"Hello again\", \"conversationId\": \"%s\"}".formatted(unknown))).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void aConversationIdThatIsNotAUuidIsA400() {
		assertThat(post("{\"question\": \"Hi\", \"conversationId\": \"not-a-uuid\"}")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(assistantService);
	}

	@Test
	void aBlankQuestionIsA400() {
		assertThat(post("{\"question\": \" \"}")).hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.errors[0].field")
												 .isEqualTo("question");
		verifyNoInteractions(assistantService);
	}

	@Test
	void anUnavailableModelIsA502() {
		given(assistantService.answer(any())).willThrow(new ChatModelUnavailableException("The chat model is unavailable", null));

		assertThat(post("{\"question\": \"Hi\"}")).hasStatus(HttpStatus.BAD_GATEWAY);
	}

	@Test
	void everyAnswerSetsAScopedHttpOnlySessionCookieWithItsConversation() {
		given(assistantService.answer(new AssistantRequest("Hi", null))).willReturn(answerIn(CONVERSATION));

		final String cookie = post("{\"question\": \"Hi\"}").exchange().getResponse().getHeader(HttpHeaders.SET_COOKIE);

		assertThat(cookie).startsWith("assistant-conversation=" + CONVERSATION)
						  .contains("Path=/api/v1/assistant")
						  .contains("HttpOnly")
						  .contains("SameSite=Lax")
						  .doesNotContain("Max-Age")
						  .doesNotContain("Secure");
	}

	@Test
	void theCookieContinuesTheConversationWhenTheBodyHasNoId() {
		given(assistantService.answer(new AssistantRequest("And the price?", CONVERSATION))).willReturn(answerIn(CONVERSATION));

		assertThat(post("{\"question\": \"And the price?\"}", CONVERSATION.toString())).hasStatusOk()
																					   .bodyJson()
																					   .extractingPath("$.conversationId")
																					   .isEqualTo(CONVERSATION.toString());
		then(assistantService).should().answer(new AssistantRequest("And the price?", CONVERSATION));
	}

	@Test
	void anIdInTheBodyWinsOverTheCookie() {
		final UUID other = UUID.randomUUID();
		given(assistantService.answer(new AssistantRequest("Hi", CONVERSATION))).willReturn(answerIn(CONVERSATION));

		final String cookie = post("{\"question\": \"Hi\", \"conversationId\": \"%s\"}".formatted(CONVERSATION), other.toString())
				.exchange().getResponse().getHeader(HttpHeaders.SET_COOKIE);

		then(assistantService).should(never()).answer(new AssistantRequest("Hi", other));
		assertThat(cookie).startsWith("assistant-conversation=" + CONVERSATION);
	}

	@Test
	void aCookieWhoseConversationIsGoneStartsANewOneInsteadOfFailing() {
		final UUID gone = UUID.randomUUID();
		given(assistantService.answer(new AssistantRequest("Hi", gone))).willThrow(new ConversationNotFoundException(gone));
		given(assistantService.answer(new AssistantRequest("Hi", null))).willReturn(answerIn(CONVERSATION));

		final var response = post("{\"question\": \"Hi\"}", gone.toString()).exchange();

		assertThat(response).hasStatusOk();
		assertThat(response.getResponse().getHeader(HttpHeaders.SET_COOKIE)).startsWith("assistant-conversation=" + CONVERSATION);
	}

	@Test
	void aCookieThatIsNotAUuidIsIgnored() {
		given(assistantService.answer(new AssistantRequest("Hi", null))).willReturn(answerIn(CONVERSATION));

		assertThat(post("{\"question\": \"Hi\"}", "not-a-uuid")).hasStatusOk();
		then(assistantService).should().answer(new AssistantRequest("Hi", null));
	}

	@Test
	void forgettingDeletesTheCookiesConversationAndClearsTheCookie() {
		final var response = mvc.delete().uri("/api/v1/assistant/conversation").cookie(
				                        new Cookie(AssistantController.COOKIE, CONVERSATION.toString()))
								.exchange();

		assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(response.getResponse().getHeader(HttpHeaders.SET_COOKIE)).startsWith("assistant-conversation=;")
																			.contains("Max-Age=0")
																			.contains("Path=/api/v1/assistant");
		then(assistantService).should().forget(CONVERSATION);
	}

	@Test
	void forgettingWithoutACookieDoesNothingButStillClears() {
		assertThat(mvc.delete().uri("/api/v1/assistant/conversation")).hasStatus(HttpStatus.NO_CONTENT);

		then(assistantService).should(never()).forget(any());
	}

	private static AssistantResponse answerIn(final UUID conversation) {
		return new AssistantResponse(conversation, "Hello", List.of(), "qwen3.5:4b", new TokenUsage(100, 10, 110));
	}

	private MockMvcTester.MockMvcRequestBuilder post(final String body, final String cookie) {
		return post(body).cookie(new Cookie(AssistantController.COOKIE, cookie));
	}

	private MockMvcTester.MockMvcRequestBuilder post(final String body) {
		return mvc.post().uri("/api/v1/assistant/answers").contentType(MediaType.APPLICATION_JSON).content(body);
	}
}