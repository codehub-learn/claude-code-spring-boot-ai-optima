package gr.codelearn.springbootai.ai.conversation;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageRequest;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageResponse.Role;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationReplyResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationReplyResponse.TokenUsage;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit 9.1 slice test for {@link ConversationController}: the resource shape (201 + Location, 200, 204), validation, and the 404 / 502
 * mappings (9.2 dropped the 409 limits). {@link ConversationService} is mocked.
 */
@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

	private static final UUID ID = UUID.fromString("7d1f0c9e-2b0a-4d7e-9a53-3c1f4f6a8b21");
	private static final String CONVERSATIONS = "/api/v1/conversations";
	private static final String MESSAGES = CONVERSATIONS + "/" + ID + "/messages";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ConversationService conversationService;

	@Test
	void createReturns201WithLocation() {
		given(conversationService.create()).willReturn(new ConversationResponse(ID, Instant.parse("2026-09-25T10:00:00Z")));

		assertThat(mvc.post().uri(CONVERSATIONS)).hasStatus(HttpStatus.CREATED)
												 .hasHeader("Location", "http://localhost" + CONVERSATIONS + "/" + ID)
												 .bodyJson()
												 .extractingPath("$.id")
												 .isEqualTo(ID.toString());
	}

	@Test
	void sendReturnsTheReplyAndTheTurn() {
		given(conversationService.send(ID, new ConversationMessageRequest("And in Greek?"))).willReturn(
				new ConversationReplyResponse(ID, 2, "Γεια σου.", "claude-sonnet-5", new TokenUsage(120, 5, 125)));

		assertThat(mvc.post().uri(MESSAGES).contentType(MediaType.APPLICATION_JSON).content("""
																							{"content": "And in Greek?"}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"conversationId": "7d1f0c9e-2b0a-4d7e-9a53-3c1f4f6a8b21", "turn": 2, "content": "Γεια σου."}""");
	}

	@Test
	void historyListsTheRoles() {
		given(conversationService.history(ID)).willReturn(List.of(new ConversationMessageResponse(Role.USER, "Hi"),
																  new ConversationMessageResponse(Role.ASSISTANT, "Hello!")));

		assertThat(mvc.get().uri(MESSAGES)).hasStatusOk().bodyJson().isEqualTo("""
																			   [{"role": "user", "content": "Hi"},
																			    {"role": "assistant", "content": "Hello!"}]""");
	}

	@Test
	void deleteReturns204() {
		assertThat(mvc.delete().uri(CONVERSATIONS + "/" + ID)).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void blankMessageReturns400() {
		assertThat(mvc.post().uri(MESSAGES).contentType(MediaType.APPLICATION_JSON).content("""
																							{"content": ""}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("content");
		verifyNoInteractions(conversationService);
	}

	@Test
	void malformedIdReturns400() {
		assertThat(mvc.get().uri(CONVERSATIONS + "/not-a-uuid/messages")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(conversationService);
	}

	@Test
	void unknownConversationReturns404() {
		given(conversationService.history(ID)).willThrow(new ConversationNotFoundException(ID));

		assertThat(mvc.get().uri(MESSAGES)).hasStatus(HttpStatus.NOT_FOUND)
										   .bodyJson()
										   .extractingPath("$.detail")
										   .isEqualTo("No conversation with id " + ID);
	}

	@Test
	void unknownConversationOnDeleteReturns404() {
		willThrow(new ConversationNotFoundException(ID)).given(conversationService).delete(ID);

		assertThat(mvc.delete().uri(CONVERSATIONS + "/" + ID)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void modelFailureReturns502() {
		given(conversationService.send(any(), any())).willThrow(new ChatModelUnavailableException("The chat model is unavailable", null));

		assertThat(mvc.post().uri(MESSAGES).contentType(MediaType.APPLICATION_JSON).content("""
																							{"content": "Hi"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}
}