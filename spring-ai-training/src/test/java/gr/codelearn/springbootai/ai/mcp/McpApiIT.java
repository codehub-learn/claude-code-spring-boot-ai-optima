package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 11.4 integration test: the {@code mcp} feature end to end with the real central {@code ChatClient} and Spring AI's tool loop. The
 * MCP client is off in the {@code test} profile (there is no server), so a stub {@link ToolCallbackProvider} stands in for the one the MCP
 * client starter publishes: from the feature's point of view they are the same bean type. Only the model is scripted. The wire itself
 * (HTTP, the MCP handshake) is covered by {@code McpServerIT} in the server module and by the live run in the README.
 */
@AutoConfigureMockMvc
class McpApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ScriptedModel model;

	@Autowired
	private StubMcpTools stubTools;

	@BeforeEach
	void reset() {
		model.prompts.clear();
		stubTools.down.set(false);
	}

	@Test
	void theToolsTheServerOffersAreListedWithDescriptionAndSchema() {
		assertThat(mvc.get().uri("/api/v1/mcp/tools")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																								   [{"name": "currentTime", "description": "Stub clock: the time in a time zone"}]""");
		assertThat(mvc.get().uri("/api/v1/mcp/tools")).bodyJson().extractingPath("$[0].inputSchema").asString().contains("zoneId");
	}

	@Test
	void theModelCallsTheRemoteToolAndItsResultGoesBackToTheModel() {
		final var response = ask("What time is it in Athens?").exchange();

		assertThat(response).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																		 {"content": "Tool said: \\"13:15 in Europe/Athens\\"",
																		  "toolCalls": [{"name": "currentTime", "arguments": "{\\"zoneId\\":\\"Europe/Athens\\"}", "error": null}],
																		  "usage": {"promptTokens": 200, "completionTokens": 20, "totalTokens": 220}}""");
		// Two rounds: the question with the tool definitions, then the tool call and its result.
		assertThat(model.prompts).hasSize(2);
		final ToolCallingChatOptions options = (ToolCallingChatOptions) model.prompts.getFirst().getOptions();
		assertThat(options.getToolCallbacks()).extracting(tool -> tool.getToolDefinition().name()).containsExactly("currentTime");
		assertThat(model.prompts.getLast().getInstructions()).extracting(Message::getMessageType).map(Enum::name)
															 .containsExactly("SYSTEM", "USER", "ASSISTANT", "TOOL");
	}

	@Test
	void aFailingRemoteToolReachesTheModelAsAMessageAndIsRecorded() {
		final var response = ask("What time is it on Mars?").exchange();

		assertThat(response).hasStatusOk().bodyJson().extractingPath("$.toolCalls[0].error").asString().isNotBlank();
		final var toolResult = (ToolResponseMessage) model.prompts.getLast().getInstructions().getLast();
		assertThat(String.valueOf(toolResult.getResponses().getFirst().responseData())).isNotBlank();
	}

	@Test
	void aQuestionThatNeedsNoToolIsAnsweredWithoutOne() {
		assertThat(ask("What is the capital of France?")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																									  {"content": "No tool needed.", "toolCalls": []}""");
		assertThat(model.prompts).hasSize(1);
	}

	@Test
	void aServerThatIsDownIsA502AndTheModelIsNotCalled() {
		stubTools.down.set(true);

		assertThat(ask("What time is it in Athens?")).hasStatus(502).bodyJson().extractingPath("$.detail")
													 .isEqualTo("The MCP server is unavailable, try again later");
		assertThat(mvc.get().uri("/api/v1/mcp/tools")).hasStatus(502);
		assertThat(model.prompts).isEmpty();
	}

	@Test
	void aFailingModelIsA502() {
		assertThat(ask("Please explode boom")).hasStatus(502);
	}

	private MockMvcTester.MockMvcRequestBuilder ask(final String question) {
		return mvc.post().uri("/api/v1/mcp/answers").contentType(MediaType.APPLICATION_JSON).content(
				"{\"question\": \"" + question + "\"}");
	}

	/**
	 * What the MCP client starter's provider does for a remote server, minus the network: list the tools, then call them. {@code down}
	 * makes listing fail the way an unreachable server does.
	 */
	static final class StubMcpTools implements ToolCallbackProvider {

		private final AtomicBoolean down = new AtomicBoolean();
		private final ToolCallback[] tools = ToolCallbacks.from(new ClockTool());

		@Override
		public ToolCallback[] getToolCallbacks() {
			if (down.get()) {
				throw new IllegalStateException("Connection refused");
			}
			return tools;
		}
	}

	static final class ClockTool {

		@Tool(description = "Stub clock: the time in a time zone")
		public String currentTime(final String zoneId) {
			if (zoneId.startsWith("Mars")) {
				throw new IllegalArgumentException("Unknown time zone '" + zoneId + "'");
			}
			return "13:15 in " + zoneId;
		}
	}

	@TestConfiguration
	static class StubConfig {

		@Bean
		@Primary
		ScriptedModel scriptedModel() {
			return new ScriptedModel();
		}

		@Bean
		StubMcpTools stubMcpTools() {
			return new StubMcpTools();
		}
	}

	/**
	 * Round 1: "Athens" -> a {@code currentTime} call for Europe/Athens, "Mars" -> one for Mars/Olympus (fails), "boom" -> the model
	 * throws, else a plain answer. Round 2 (a tool response is present): answers with the tool's result. 100 prompt + 10 completion tokens
	 * per round.
	 */
	static final class ScriptedModel implements ChatModel {

		private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

		@Override
		public ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			final Message last = prompt.getInstructions().getLast();
			final AssistantMessage reply;
			if (last instanceof ToolResponseMessage toolResponse) {
				reply = new AssistantMessage("Tool said: " + toolResponse.getResponses().getFirst().responseData());
			} else {
				final String question = prompt.getUserMessage().getText();
				if (question.contains("boom")) {
					throw new IllegalStateException("the model exploded");
				}
				if (question.contains("Athens")) {
					reply = currentTime("Europe/Athens");
				} else if (question.contains("Mars")) {
					reply = currentTime("Mars/Olympus");
				} else {
					reply = new AssistantMessage("No tool needed.");
				}
			}
			return new ChatResponse(List.of(new Generation(reply)),
									ChatResponseMetadata.builder().model("scripted").usage(new DefaultUsage(100, 10)).build());
		}

		private static AssistantMessage currentTime(final String zone) {
			return AssistantMessage.builder()
								   .content("")
								   .toolCalls(List.of(new ToolCall("call-1", "function", "currentTime", "{\"zoneId\":\"" + zone + "\"}")))
								   .build();
		}

		@Override
		public ChatOptions getOptions() {
			// The ChatClient only attaches tools when the model's options are ToolCallingChatOptions (every real provider's are).
			return ToolCallingChatOptions.builder().build();
		}
	}
}