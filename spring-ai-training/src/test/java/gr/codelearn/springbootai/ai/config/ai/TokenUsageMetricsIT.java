package gr.codelearn.springbootai.ai.config.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 10.4: token usage becomes metrics without any code in the features. The real {@code OllamaChatModel} and
 * {@code OllamaEmbeddingModel} run against a stub Ollama server (a JDK {@link HttpServer} answering {@code /api/chat} and
 * {@code /api/embed} with fixed token counts), so the observations Spring AI records inside the providers are the ones checked here, read
 * back from the context's {@link MeterRegistry}. Exporting (OTLP) is off in tests; recording is not. A recording {@link ObservationHandler}
 * also sees every finished observation, to check that none of them carries the question.
 */
@AutoConfigureMockMvc
class TokenUsageMetricsIT extends AbstractOracleIntegrationTest {

	private static final String CHAT_MODEL = "qwen3.5:4b";
	private static final String EMBEDDING_MODEL = "qwen3-embedding:0.6b";
	private static final HttpServer OLLAMA = startStubOllama();
	private static final List<Observation.Context> OBSERVED = new CopyOnWriteArrayList<>();

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private MeterRegistry meterRegistry;

	@DynamicPropertySource
	static void ollama(final DynamicPropertyRegistry registry) {
		registry.add("app.ai.chat-provider", () -> "ollama");
		registry.add("spring.ai.ollama.base-url", () -> "http://localhost:" + OLLAMA.getAddress().getPort());
	}

	@AfterAll
	static void stopStubOllama() {
		OLLAMA.stop(0);
	}

	@Test
	void aChatCallCountsInputOutputAndTotalTokensPerModel() {
		final double input = tokens("input", CHAT_MODEL);
		final double output = tokens("output", CHAT_MODEL);
		final double total = tokens("total", CHAT_MODEL);
		final long calls = chatCalls();

		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Hi\"}"))
				.hasStatus(HttpStatus.OK);

		// The provider's own counts (prompt_eval_count / eval_count), not an estimate.
		assertThat(tokens("input", CHAT_MODEL) - input).isEqualTo(42);
		assertThat(tokens("output", CHAT_MODEL) - output).isEqualTo(7);
		assertThat(tokens("total", CHAT_MODEL) - total).isEqualTo(49);
		assertThat(chatCalls()).isEqualTo(calls + 1);
		// Low-cardinality tags only: provider, operation, requested and answering model. Never the prompt.
		assertThat(counter("input", CHAT_MODEL).getId().getTags()).extracting(Tag::getKey)
																  .containsExactlyInAnyOrder("gen_ai.operation.name",
		                                                                                     "gen_ai.request.model",
																							 "gen_ai.response.model", "gen_ai.system",
																							 "gen_ai.token.type");
		assertThat(counter("input", CHAT_MODEL).getId().getTag("gen_ai.system")).isEqualTo("ollama");
		assertThat(counter("input", CHAT_MODEL).getId().getTag("gen_ai.request.model")).isEqualTo(CHAT_MODEL);
	}

	@Test
	void aRagAnswerAlsoCountsTheEmbeddingTokensAndTimesTheVectorSearch() {
		// Pitfall: the first vector search in a process embeds "Hello World" once more. The search's observation asks the embedding model
		// for its dimensions, and for a model Spring AI doesn't know (qwen3-embedding) it finds out by embedding a test string. Those
		// tokens are counted too, so warm up first and measure the next question.
		ask("Is there a warm-up question?");
		final double embeddingInput = tokens("input", EMBEDDING_MODEL);
		final double chatInput = tokens("input", CHAT_MODEL);
		final long searches = vectorSearches();

		ask("How long is the laptop warranty?");

		// One question: one embedding of it, one vector search, one chat call.
		assertThat(tokens("input", EMBEDDING_MODEL) - embeddingInput).isEqualTo(5);
		assertThat(tokens("input", CHAT_MODEL) - chatInput).isEqualTo(42);
		assertThat(vectorSearches()).isEqualTo(searches + 1);
		// Pitfall: the embedding observation's request model is "none" (the Ollama embedding options are merged after the observation
		// starts), so dashboards group by gen_ai.response.model.
		assertThat(counter("input", EMBEDDING_MODEL).getId().getTag("gen_ai.request.model")).isEqualTo("none");
		assertThat(counter("input", EMBEDDING_MODEL).getId().getTag("gen_ai.operation.name")).isEqualTo("embedding");
	}

	@Test
	void noObservationCarriesTheQuestion() {
		final String question = "Can I return opened earbuds?";
		OBSERVED.clear();

		ask(question);

		assertThat(OBSERVED).anySatisfy(context -> assertThat(context).isInstanceOf(VectorStoreObservationContext.class));
		// Spring AI 2.0.1 puts the search text into the vector store span as db.vector.query.content; ObservabilityConfig removes it.
		assertThat(OBSERVED).allSatisfy(context -> assertThat(context.getAllKeyValues().stream().map(Object::toString))
				.noneMatch(keyValue -> keyValue.contains(question)));
	}

	private void ask(final String question) {
		assertThat(mvc.post()
					  .uri("/api/v1/knowledge/answers")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("{\"question\":\"" + question + "\"}")).hasStatus(HttpStatus.OK);
	}

	private double tokens(final String type, final String model) {
		final Counter counter = meterRegistry.find("gen_ai.client.token.usage").tag("gen_ai.token.type", type)
											 .tag("gen_ai.response.model", model).counter();
		return counter == null ? 0 : counter.count();
	}

	private Counter counter(final String type, final String model) {
		return meterRegistry.get("gen_ai.client.token.usage").tag("gen_ai.token.type", type).tag("gen_ai.response.model", model).counter();
	}

	private long chatCalls() {
		final Timer timer = meterRegistry.find("gen_ai.client.operation").tag("gen_ai.operation.name", "chat").timer();
		return timer == null ? 0 : timer.count();
	}

	private long vectorSearches() {
		final Timer timer = meterRegistry.find("db.vector.client.operation").tag("db.operation.name", "query").timer();
		return timer == null ? 0 : timer.count();
	}

	@TestConfiguration
	static class RecordingObservations {

		@Bean
		ObservationHandler<Observation.Context> recordingObservationHandler() {
			return new ObservationHandler<>() {

				@Override
				public void onStop(final Observation.Context context) {
					OBSERVED.add(context);
				}

				@Override
				public boolean supportsContext(final Observation.Context context) {
					return true;
				}
			};
		}
	}

	private static HttpServer startStubOllama() {
		try {
			final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/api/chat", exchange -> respond(exchange, """
																			{"model":"%s","created_at":"2026-09-28T12:00:00Z","message":{"role":"assistant","content":"Hello there."},
																			 "done":true,"done_reason":"stop","total_duration":1000000,"prompt_eval_count":42,"eval_count":7}
																			""".formatted(CHAT_MODEL)));
			// A 1024-dimension vector (the store's size), all equal: fine for an empty or small table.
			final String vector = Collections.nCopies(1024, "0.03125").stream().collect(Collectors.joining(","));
			server.createContext("/api/embed", exchange -> respond(exchange, """
																			 {"model":"%s","embeddings":[[%s]],"total_duration":1000000,"prompt_eval_count":5}
																			 """.formatted(EMBEDDING_MODEL, vector)));
			server.start();
			return server;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void respond(final HttpExchange exchange, final String json) throws IOException {
		exchange.getRequestBody().readAllBytes();
		final byte[] body = json.replace("\n", "").getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, body.length);
		try (var out = exchange.getResponseBody()) {
			out.write(body);
		}
	}
}