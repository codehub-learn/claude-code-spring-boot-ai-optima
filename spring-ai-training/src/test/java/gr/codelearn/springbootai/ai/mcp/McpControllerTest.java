package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.TokenUsage;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.mcp.dto.McpAnswerRequest;
import gr.codelearn.springbootai.ai.mcp.dto.McpAnswerResponse;
import gr.codelearn.springbootai.ai.mcp.dto.McpToolResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit 11.4 slice test for {@link McpController}: the JSON contract, validation and the 502 mappings. The service is mocked.
 */
@WebMvcTest(McpController.class)
class McpControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private McpService mcpService;

	@Test
	void theToolsAreListedWithTheirSchemas() {
		given(mcpService.listTools()).willReturn(List.of(new McpToolResponse("currentTime", "Time in a zone", "{\"type\":\"object\"}")));

		assertThat(mvc.get().uri("/api/v1/mcp/tools")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																								   [{"name": "currentTime", "description": "Time in a zone", "inputSchema": "{\\"type\\":\\"object\\"}"}]""");
	}

	@Test
	void anUnreachableServerIsA502() {
		given(mcpService.listTools()).willThrow(new McpServerUnavailableException("The MCP server is unavailable, try again later", null));

		assertThat(mvc.get().uri("/api/v1/mcp/tools")).hasStatus(HttpStatus.BAD_GATEWAY).bodyJson()
													  .extractingPath("$.detail").isEqualTo(
				                                              "The MCP server is unavailable, try again later");
	}

	@Test
	void theAnswerComesWithTheToolCallsAndTheUsage() {
		given(mcpService.answer(new McpAnswerRequest("What time is it in Athens?")))
				.willReturn(
						new McpAnswerResponse("13:15", List.of(new ToolCall("currentTime", "{\"zoneId\":\"Europe/Athens\"}", 30, 4, null)),
											  "qwen3.5:4b", new TokenUsage(300, 20, 320)));

		assertThat(post("{\"question\": \"What time is it in Athens?\"}")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																													   {"content": "13:15", "toolCalls": [{"name": "currentTime", "resultChars": 30, "error": null}],
																													    "model": "qwen3.5:4b", "usage": {"totalTokens": 320}}""");
	}

	@Test
	void aFailingModelIsA502() {
		given(mcpService.answer(new McpAnswerRequest("Hi"))).willThrow(
				new ChatModelUnavailableException("The chat model is unavailable", null));

		assertThat(post("{\"question\": \"Hi\"}")).hasStatus(HttpStatus.BAD_GATEWAY);
	}

	@Test
	void aBlankQuestionIsA400AndTheServiceIsNotCalled() {
		assertThat(post("{\"question\": \"  \"}")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(mcpService);
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResult post(final String body) {
		return mvc.post().uri("/api/v1/mcp/answers").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}
}