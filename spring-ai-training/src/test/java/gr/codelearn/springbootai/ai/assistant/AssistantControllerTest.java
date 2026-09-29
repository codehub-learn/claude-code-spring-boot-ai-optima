package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantRequest;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.TokenUsage;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;
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
 * Unit 11.1 slice test for {@link AssistantController}: the JSON contract, validation, and the 502 mapping. The service is mocked.
 */
@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private AssistantService assistantService;

	@Test
	void theAnswerComesWithTheToolCallsAndTheUsage() {
		given(assistantService.answer(new AssistantRequest("What does LAP-0002 cost?")))
				.willReturn(new AssistantResponse("2199.00 EUR [LAP-0002].",
												  List.of(new ToolCall("getProduct", "{\"sku\":\"LAP-0002\"}", 180, 6, null)),
												  "qwen3.5:4b", new TokenUsage(1652, 60, 1712)));

		assertThat(post("{\"question\": \"What does LAP-0002 cost?\"}")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																													 {"content": "2199.00 EUR [LAP-0002].",
																													  "toolCalls": [{"name": "getProduct", "arguments": "{\\"sku\\":\\"LAP-0002\\"}", "resultChars": 180, "error": null}],
																													  "model": "qwen3.5:4b", "usage": {"totalTokens": 1712}}""");
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

	private MockMvcTester.MockMvcRequestBuilder post(final String body) {
		return mvc.post().uri("/api/v1/assistant/answers").contentType(MediaType.APPLICATION_JSON).content(body);
	}
}