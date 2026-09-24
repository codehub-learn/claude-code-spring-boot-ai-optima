package gr.codelearn.springbootai.ai.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 6.2 integration test: the full context (Anthropic starter, {@code config.ai.ChatClientConfig}, the advisor chain,
 * {@code config.web.ApiExceptionHandler}, API versioning, springdoc) over HTTP via {@link MockMvcTester}. Only the provider is replaced: a
 * {@code @Primary} stub {@link ChatModel} stands in for {@code AnthropicChatModel}, so no API key or network is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatApiIT {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void chatCompletionFlowsThroughTheRealChatClientToTheModel() {
		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).content("""
																											  {"message": "Say hi"}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"content": "echo: Say hi", "model": "stub-model",
									 "usage": {"promptTokens": 12, "completionTokens": 5, "totalTokens": 17}}""");
	}

	@Test
	void modelFailureIsMappedTo502() {
		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).content("""
																											  {"message": "FAIL please"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson()
				.extractingPath("$.status")
				.convertTo(Integer.class)
				.isEqualTo(502);
	}

	@Test
	void openApiSpecIsStillServedAlongsideTheVersionedApi() {
		assertThat(mvc.get().uri("/v3/api-docs"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.paths")
				.asMap()
				.containsKey("/api/v1/chat-completions");
	}

	@TestConfiguration
	static class StubModelConfig {

		/**
		 * Echoes the user message back with fixed metadata; a message containing "FAIL" simulates a provider outage.
		 */
		@Bean
		@Primary
		ChatModel stubChatModel() {
			return prompt -> {
				final List<Message> messages = prompt.getInstructions();
				final String userText = messages.getLast().getText();
				if (userText != null && userText.contains("FAIL")) {
					throw new IllegalStateException("simulated provider outage");
				}
				final ChatResponseMetadata metadata = ChatResponseMetadata.builder()
																		  .model("stub-model")
																		  .usage(new DefaultUsage(12, 5))
																		  .build();
				return new ChatResponse(List.of(new Generation(new AssistantMessage("echo: " + userText))), metadata);
			};
		}
	}
}