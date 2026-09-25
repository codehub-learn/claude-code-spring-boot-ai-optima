package gr.codelearn.springbootai.ai.chat;

import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionChunkResponse;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionRequest;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse.TokenUsage;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Units 6.2 / 6.3 slice test for {@link ChatController}: MVC infrastructure + {@code config.web.ApiExceptionHandler}, {@link ChatService}
 * mocked. Covers the JSON contract, Bean Validation (400), the model-failure mapping (502), Spring Framework 7 API versioning (unsupported
 * version → 400 before the handler runs), and the {@code text/event-stream} representation with its in-band {@code error} event.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

	private static final String V1 = "/api/v1/chat-completions";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ChatService chatService;

	@Test
	void completeReturnsTheReplyWithModelAndTokenUsage() {
		given(chatService.complete(new ChatCompletionRequest("Say hi")))
				.willReturn(new ChatCompletionResponse("Hi!", "claude-sonnet-5", new TokenUsage(12, 3, 15)));

		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"message": "Say hi"}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"content": "Hi!", "model": "claude-sonnet-5",
									 "usage": {"promptTokens": 12, "completionTokens": 3, "totalTokens": 15}}""");
	}

	@Test
	void blankMessageReturns400WithTheFieldError() {
		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"message": "  "}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("message");
		verifyNoInteractions(chatService);
	}

	@Test
	void modelFailureReturns502ProblemDetail() {
		given(chatService.complete(any())).willThrow(
				new ChatModelUnavailableException("The chat model is unavailable, try again later", new IllegalStateException("down")));

		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"message": "Say hi"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.bodyJson()
				.extractingPath("$.detail")
				.isEqualTo("The chat model is unavailable, try again later");
	}

	@Test
	void eventStreamAcceptHeaderStreamsJsonChunks() {
		given(chatService.stream(new ChatCompletionRequest("Say hi")))
				.willReturn(Flux.just(new ChatCompletionChunkResponse("Hi"), new ChatCompletionChunkResponse(" there")));

		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM).content("""
																														  {"message": "Say hi"}"""))
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
				.bodyText()
				.isEqualTo("""
						   event:chunk
						   data:{"content":"Hi"}
						   
						   event:chunk
						   data:{"content":" there"}
						   
						   """);
	}

	@Test
	void modelFailureMidStreamEndsWithAnErrorEvent() {
		given(chatService.stream(any())).willReturn(
				Flux.concat(Flux.just(new ChatCompletionChunkResponse("Hi")),
							Flux.error(new ChatModelUnavailableException("The chat model is unavailable, try again later", null))));

		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM).content("""
																														  {"message": "Say hi"}"""))
				.as("the status is already committed as 200 when the model fails")
				.hasStatusOk()
				.bodyText()
				.contains("event:chunk\ndata:{\"content\":\"Hi\"}")
				.contains("event:error\ndata:{")
				.contains("\"status\":502");
	}

	@Test
	void blankMessageIsRejectedBeforeStreaming() {
		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM).content("""
																														  {"message": ""}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(chatService);
	}

	@Test
	void unsupportedApiVersionReturns400() {
		assertThat(mvc.post().uri("/api/v2/chat-completions").contentType(MediaType.APPLICATION_JSON).content("""
																											  {"message": "Say hi"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(chatService);
	}
}