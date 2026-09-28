package gr.codelearn.springbootai.ai.config.ai;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Unit 10.3: logs the stages of a RAG call, <b>at DEBUG only</b>, as structured events: the message reads well in the local plain-text
 * console, and the same values go out as SLF4J key-value pairs, which Boot's structured (ECS JSON) logging writes as fields.
 *
 * <ol>
 *   <li><b>Retrieval</b> ({@code rag.stage=retrieval}): hit count, best similarity, the filter, and the size of the context the model is
 *   about to get (JTokkit estimate); then one event per hit ({@code rag.stage=hit}) with its rank, source, ref, section, chunk position and
 *   similarity. Logged <em>before</em> the model call, so it is there even when generation fails.</li>
 *   <li><b>Generation</b> ({@code rag.stage=generation}): duration, prompt and completion tokens, answer length, model; or
 *   ({@code rag.stage=generation-failed}) the duration and the exception type, rethrown untouched.</li>
 * </ol>
 *
 * <p>Nothing here logs the question, a chunk's text or the answer: only counts, ids, scores and times. Content goes through
 * {@code SimpleLoggerAdvisor}, which logs whole prompts and completions and is switched on in {@code application-local.yml} only
 * (CLAUDE.md: no full prompts or completions at INFO outside local). DEBUG for this class is on under {@code local} too (its package
 * logger), off in {@code application.yml}'s defaults and the test profile. When DEBUG is off the advisor is a pass-through: nothing is
 * counted or estimated.
 *
 * <p>Order: one step inside {@link QuestionAnswerAdvisor} ({@link RagConfig#ORDER} + 1), so the request it sees already carries the
 * retrieved documents in its context ({@link QuestionAnswerAdvisor#RETRIEVED_DOCUMENTS}) and the call it times is the model call alone.
 * Retrieval latency is not measured here; unit 10.4's spans time the vector search. Like the retrieval advisor, it is added per call.
 */
public class RagLoggingAdvisor implements CallAdvisor {

	static final int ORDER = RagConfig.ORDER + 1;

	private static final Logger log = LoggerFactory.getLogger(RagLoggingAdvisor.class);

	private final TokenCountEstimator tokenCounter = new JTokkitTokenCountEstimator();

	@Override
	public String getName() {
		return "RagLoggingAdvisor";
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public ChatClientResponse adviseCall(final ChatClientRequest request, final CallAdvisorChain chain) {
		if (!log.isDebugEnabled()) {
			return chain.nextCall(request);
		}
		logRetrieval(request.context());
		final long start = System.nanoTime();
		final ChatClientResponse response;
		try {
			response = chain.nextCall(request);
		} catch (RuntimeException ex) {
			final long millis = millisSince(start);
			log.atDebug()
			   .addKeyValue("rag.stage", "generation-failed")
			   .addKeyValue("rag.duration_ms", millis)
			   .addKeyValue("rag.error", ex.getClass().getSimpleName())
			   .log("RAG generation failed after {} ms: {}", millis, ex.getClass().getSimpleName());
			throw ex;
		}
		logGeneration(response.chatResponse(), millisSince(start));
		return response;
	}

	private void logRetrieval(final Map<String, @Nullable Object> context) {
		final List<Document> documents = retrievedDocuments(context);
		final String filter = String.valueOf(context.get(QuestionAnswerAdvisor.FILTER_EXPRESSION));
		if (documents.isEmpty()) {
			log.atDebug()
			   .addKeyValue("rag.stage", "retrieval")
			   .addKeyValue("rag.hits", 0)
			   .addKeyValue("rag.filter", filter)
			   .log("RAG retrieval: no document retrieved (filter: {}), the model answers without context", filter);
			return;
		}
		final int contextTokens = documents.stream().mapToInt(document -> tokenCounter.estimate(text(document))).sum();
		final double topSimilarity = score(documents.getFirst());
		log.atDebug()
		   .addKeyValue("rag.stage", "retrieval")
		   .addKeyValue("rag.hits", documents.size())
		   .addKeyValue("rag.top_similarity", topSimilarity)
		   .addKeyValue("rag.context_tokens", contextTokens)
		   .addKeyValue("rag.filter", filter)
		   .log("RAG retrieval: {} hits, top similarity {}, context ~{} tokens (filter: {})", documents.size(), format(topSimilarity),
				contextTokens, filter);
		for (int index = 0; index < documents.size(); index++) {
			logHit(index + 1, documents.get(index));
		}
	}

	private void logHit(final int rank, final Document document) {
		final Map<String, Object> metadata = document.getMetadata();
		final String source = VectorStoreMetadata.text(metadata.get("source"));
		final String ref = VectorStoreMetadata.text(metadata.get("ref"));
		final String section = VectorStoreMetadata.text(metadata.get("section"));
		final String chunking = VectorStoreMetadata.text(metadata.get("chunking"));
		// Chunk position as "markdown 3/12" (1-based); catalog rows have none and read "none".
		final String chunk = metadata.containsKey("chunk_index")
							 ? "%s %d/%s".formatted(chunking,
													new BigDecimal(VectorStoreMetadata.text(metadata.get("chunk_index"))).intValue() + 1,
													VectorStoreMetadata.text(metadata.get("chunk_count")))
							 : chunking;
		final double similarity = score(document);
		final int tokens = tokenCounter.estimate(text(document));
		log.atDebug()
		   .addKeyValue("rag.stage", "hit")
		   .addKeyValue("rag.rank", rank)
		   .addKeyValue("rag.source", source)
		   .addKeyValue("rag.ref", ref)
		   .addKeyValue("rag.section", section)
		   .addKeyValue("rag.chunk", chunk)
		   .addKeyValue("rag.similarity", similarity)
		   .addKeyValue("rag.tokens", tokens)
		   .log("RAG hit {}: {}:{}{} [{}], similarity {}, {} tokens", rank, source, ref, section.isEmpty() ? "" : " > " + section, chunk,
				format(similarity), tokens);
	}

	private static void logGeneration(final @Nullable ChatResponse response, final long millis) {
		final ChatResponseMetadata metadata = response == null ? null : response.getMetadata();
		final Usage usage = metadata == null ? null : metadata.getUsage();
		final Integer promptTokens = usage == null ? null : usage.getPromptTokens();
		final Integer completionTokens = usage == null ? null : usage.getCompletionTokens();
		final int answerChars = response == null || response.getResult() == null
								? 0 : Objects.requireNonNullElse(response.getResult().getOutput().getText(), "").length();
		final String model = metadata == null ? "" : metadata.getModel();
		log.atDebug()
		   .addKeyValue("rag.stage", "generation")
		   .addKeyValue("rag.duration_ms", millis)
		   .addKeyValue("rag.prompt_tokens", promptTokens)
		   .addKeyValue("rag.completion_tokens", completionTokens)
		   .addKeyValue("rag.answer_chars", answerChars)
		   .addKeyValue("rag.model", model)
		   .log("RAG generation: {} ms, {} prompt + {} completion tokens, answer {} chars, model {}", millis, promptTokens,
				completionTokens,
				answerChars, model);
	}

	private static List<Document> retrievedDocuments(final Map<String, @Nullable Object> context) {
		if (!(context.get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS) instanceof List<?> documents)) {
			return List.of();
		}
		return documents.stream().filter(Document.class::isInstance).map(Document.class::cast).toList();
	}

	private static String text(final Document document) {
		return Objects.requireNonNullElse(document.getText(), "");
	}

	private static double score(final Document document) {
		return document.getScore() == null ? 0 : Math.round(document.getScore() * 100) / 100d;
	}

	private static String format(final double similarity) {
		return String.format(Locale.ROOT, "%.2f", similarity);
	}

	private static long millisSince(final long start) {
		return (System.nanoTime() - start) / 1_000_000;
	}
}