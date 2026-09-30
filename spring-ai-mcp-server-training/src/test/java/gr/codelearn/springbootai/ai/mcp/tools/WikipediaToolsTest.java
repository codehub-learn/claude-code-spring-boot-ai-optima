package gr.codelearn.springbootai.ai.mcp.tools;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The Wikipedia tool against a mock server: no network. The request shape (MediaWiki action API) and the parsing of its answer are what can
 * break; whether Wikipedia is up is what the live run in the README shows.
 */
class WikipediaToolsTest {

	private static final String TWO_HITS = """
										   {"batchcomplete": "", "query": {"pages": {
										     "222": {"pageid": 222, "title": "Spring Boot", "index": 2, "extract": "Spring Boot is a Java framework.",
										             "fullurl": "https://en.wikipedia.org/wiki/Spring_Boot"},
										     "111": {"pageid": 111, "title": "Spring Framework", "index": 1, "extract": "The Spring Framework is a Java platform.",
										             "fullurl": "https://en.wikipedia.org/wiki/Spring_Framework"}}}}""";

	private final RestClient.Builder builder = RestClient.builder().baseUrl("https://en.wikipedia.org");
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final WikipediaTools tools = new WikipediaTools(builder.build(), properties());

	@Test
	void theBestMatchesComeBackInSearchRankWithIntroAndLink() {
		server.expect(requestTo(containsString("/w/api.php")))
			  .andExpect(method(HttpMethod.GET))
			  .andExpect(requestTo(containsString("gsrsearch=Spring%20Framework")))
			  .andExpect(requestTo(containsString("gsrlimit=3")))
			  .andRespond(withSuccess(TWO_HITS, MediaType.APPLICATION_JSON));

		final String result = tools.searchWikipedia("Spring Framework");

		assertThat(result).isEqualTo("""
									 Spring Framework
									 The Spring Framework is a Java platform.
									 https://en.wikipedia.org/wiki/Spring_Framework
									 
									 Spring Boot
									 Spring Boot is a Java framework.
									 https://en.wikipedia.org/wiki/Spring_Boot""");
		server.verify();
	}

	@Test
	void aLongIntroIsCutSoOneArticleCannotFloodTheModelsContext() {
		final String longText = "word ".repeat(400);
		server.expect(requestTo(containsString("/w/api.php")))
			  .andRespond(withSuccess("{\"query\": {\"pages\": {\"1\": {\"title\": \"Long\", \"index\": 1, \"extract\": \"" + longText
									  + "\", \"fullurl\": \"https://en.wikipedia.org/wiki/Long\"}}}}", MediaType.APPLICATION_JSON));

		final String result = tools.searchWikipedia("long");

		assertThat(result).contains("Long\n").endsWith("...\nhttps://en.wikipedia.org/wiki/Long");
		assertThat(result.length()).isLessThan(600);
	}

	@Test
	void noMatchIsAnAnswerNotAnError() {
		server.expect(requestTo(containsString("/w/api.php")))
			  .andRespond(withSuccess("{\"batchcomplete\": \"\"}", MediaType.APPLICATION_JSON));

		assertThat(tools.searchWikipedia("qzxwvk")).isEqualTo("No Wikipedia article matches 'qzxwvk'. Try different keywords.");
	}

	@Test
	void aWikipediaFailureBecomesASentenceForTheModel() {
		server.expect(requestTo(containsString("/w/api.php"))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThatIllegalStateException().isThrownBy(() -> tools.searchWikipedia("Spring"))
										 .withMessage("Wikipedia could not be reached right now. Tell the user the search is unavailable" +
													  ".");
	}

	@Test
	void anEmptyOrOversizedQueryIsRejectedBeforeAnyRequest() {
		assertThatIllegalArgumentException().isThrownBy(() -> tools.searchWikipedia("  ")).withMessageContaining("empty");
		assertThatIllegalArgumentException().isThrownBy(() -> tools.searchWikipedia("x".repeat(201))).withMessageContaining("too long");
		server.verify();
	}

	private static WikipediaProperties properties() {
		return new WikipediaProperties("https://en.wikipedia.org", Duration.ofSeconds(2), Duration.ofSeconds(5), 3);
	}
}