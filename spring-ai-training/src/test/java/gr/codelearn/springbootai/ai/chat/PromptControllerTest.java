package gr.codelearn.springbootai.ai.chat;

import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse.TokenUsage;
import gr.codelearn.springbootai.ai.chat.dto.PromptRequest;
import gr.codelearn.springbootai.ai.chat.dto.PromptRequest.PromptMessage;
import gr.codelearn.springbootai.ai.chat.dto.PromptRequest.Role;

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
 * Unit 6.3 slice test for {@link PromptController}: the role-tagged JSON contract, lower-case roles, and the validation rules (non-empty,
 * known roles, last message from the user). {@link ChatService} is mocked.
 */
@WebMvcTest(PromptController.class)
class PromptControllerTest {

	private static final String V1 = "/api/v1/prompts";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ChatService chatService;

	@Test
	void completeMapsLowerCaseRolesAndReturnsTheReply() {
		final PromptRequest expected = new PromptRequest(List.of(new PromptMessage(Role.SYSTEM, "Answer in one word."),
																 new PromptMessage(Role.USER, "Capital of Greece?"),
																 new PromptMessage(Role.ASSISTANT, "Athens."),
																 new PromptMessage(Role.USER, "And of Italy?")));
		given(chatService.completePrompt(expected)).willReturn(
				new ChatCompletionResponse("Rome.", "claude-sonnet-5", new TokenUsage(30, 2, 32)));

		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"messages": [
																					    {"role": "system",    "content": "Answer in one word."},
																					    {"role": "user",      "content": "Capital of Greece?"},
																					    {"role": "assistant", "content": "Athens."},
																					    {"role": "user",      "content": "And of Italy?"}]}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content")
				.isEqualTo("Rome.");
	}

	@Test
	void lastMessageNotFromTheUserReturns400() {
		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"messages": [
																					    {"role": "user",      "content": "Hi"},
																					    {"role": "assistant", "content": "Hello!"}]}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("lastMessageFromUser");
		verifyNoInteractions(chatService);
	}

	@Test
	void emptyMessageListReturns400() {
		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"messages": []}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("messages");
		verifyNoInteractions(chatService);
	}

	@Test
	void blankContentReturns400WithTheIndexedField() {
		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"messages": [{"role": "user", "content": " "}]}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("messages[0].content");
		verifyNoInteractions(chatService);
	}

	@Test
	void unknownRoleIsUnreadableAndReturns400() {
		// "tool" is a real Spring AI MessageType, but not one a client may send (U11).
		assertThat(mvc.post().uri(V1).contentType(MediaType.APPLICATION_JSON).content("""
																					  {"messages": [{"role": "tool", "content": "42"}]}"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(chatService);
	}
}