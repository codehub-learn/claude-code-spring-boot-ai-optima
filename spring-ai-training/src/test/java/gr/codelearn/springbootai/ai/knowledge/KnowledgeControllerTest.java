package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerRequest;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse.Source;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse.TokenUsage;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeEvaluationResponse;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeEvaluationResponse.QuestionResult;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeEvaluationResponse.StrategyResult;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeIngestionResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit 10.1 slice test for {@link KnowledgeController}: the JSON contract of the endpoints, validation, and the 502 mapping. The services
 * are mocked. Unit 10.2: the per-strategy ingestion counts and the evaluation endpoint.
 */
@WebMvcTest(KnowledgeController.class)
class KnowledgeControllerTest {

	private static final String ANSWERS = "/api/v1/knowledge/answers";
	private static final String INGESTIONS = "/api/v1/knowledge/ingestions";
	private static final String EVALUATIONS = "/api/v1/knowledge/evaluations";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private KnowledgeIngestionService ingestionService;

	@MockitoBean
	private KnowledgeService knowledgeService;

	@MockitoBean
	private KnowledgeEvaluationService evaluationService;

	@Test
	void ingestionReturnsTheCounts() {
		given(ingestionService.ingest()).willReturn(
				new KnowledgeIngestionResponse(24, 8, Map.of("whole", 8, "token", 34, "markdown", 61), 127, 9500));

		assertThat(mvc.post().uri(INGESTIONS)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																						   {"catalogDocuments": 24, "bundledDocuments": 8, "chunks": {"whole": 8, "token": 34, "markdown": 61}, "totalRows": 127}""");
	}

	@Test
	void answerReturnsTheSources() {
		given(knowledgeService.answer(new KnowledgeAnswerRequest("GPU laptop?"))).willReturn(
				new KnowledgeAnswerResponse("The Latitude 16 Workstation [catalog:LAP-0002].",
											List.of(new Source("catalog", "LAP-0002", "Latitude 16 Workstation", null, 0.61)), "qwen3" +
																															   ".5:4b",
											new TokenUsage(400, 12, 412)));

		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": "GPU laptop?"}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"content": "The Latitude 16 Workstation [catalog:LAP-0002].",
									 "sources": [{"source": "catalog", "ref": "LAP-0002", "title": "Latitude 16 Workstation",
									              "similarity": 0.61}]}""");
	}

	@Test
	void blankQuestionReturns400() {
		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": " "}"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[0].field")
				.isEqualTo("question");
		verifyNoInteractions(knowledgeService);
	}

	@Test
	void oversizedQuestionReturns400() {
		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("{\"question\": \"" + "x".repeat(1001) + "\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(knowledgeService);
	}

	@Test
	void modelFailureReturns502() {
		given(knowledgeService.answer(any())).willThrow(
				new ChatModelUnavailableException("The chat or embedding model is unavailable", null));

		assertThat(mvc.post().uri(ANSWERS).contentType(MediaType.APPLICATION_JSON).content("""
																						   {"question": "GPU laptop?"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY);
	}

	@Test
	void evaluationReturnsThePerStrategyMetrics() {
		given(evaluationService.evaluate(4)).willReturn(new KnowledgeEvaluationResponse(
				4, 19, List.of(new StrategyResult("markdown", 61, 0.824, 0.941, 0.87, 0.62, 0.45, 0.41, 310)),
				List.of(new QuestionResult("Cash on delivery fee?", "terms-of-sale", Map.of("markdown", 1)))));

		assertThat(mvc.post().uri(EVALUATIONS)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																							{"topK": 4, "strategies": [{"strategy": "markdown", "hitAt1": 0.824}], "details": [{"ranks": {"markdown": 1}}]}""");
	}

	@Test
	void evaluationTopKOutOfRangeReturns400() {
		assertThat(mvc.post().uri(EVALUATIONS + "?top-k=0")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(evaluationService);
	}

	@Test
	void ingestionFailureReturns502() {
		given(ingestionService.ingest()).willThrow(new ChatModelUnavailableException("Ingestion failed", null));

		assertThat(mvc.post().uri(INGESTIONS)).hasStatus(HttpStatus.BAD_GATEWAY);
	}
}