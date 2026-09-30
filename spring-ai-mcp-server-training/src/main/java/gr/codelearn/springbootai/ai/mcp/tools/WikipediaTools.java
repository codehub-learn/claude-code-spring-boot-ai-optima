package gr.codelearn.springbootai.ai.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A tool that reaches outside: it searches Wikipedia (the MediaWiki action API, no key needed) and returns the best few articles with their
 * intro text. This is what a model without web access cannot do, and the reason to run it in an MCP server: the network access, the
 * timeouts and the parsing live here, and every MCP client gets them.
 *
 * <p>A failure (Wikipedia down, slow, or an unexpected answer) throws with a plain sentence. The MCP server sends it to the client as an
 * error result, so the model reads it instead of getting a stack trace or an empty answer.
 */
@Component
public class WikipediaTools {

	private static final int MAX_QUERY_LENGTH = 200;
	private static final int MAX_EXTRACT_CHARS = 500;

	private final RestClient restClient;
	private final int maxResults;

	WikipediaTools(final RestClient wikipediaClient, final WikipediaProperties properties) {
		this.restClient = wikipediaClient;
		this.maxResults = properties.maxResults();
	}

	@Tool(description = "Searches Wikipedia and returns the best matching articles: title, a short intro and the link. Use it for facts "
						+ "about people, places, events, technology or history that you are not sure about or that may have changed. "
						+ "Search in English keywords, not a full sentence.")
	public String searchWikipedia(
			@ToolParam(description = "Search keywords in English, for example: Spring Framework release history") final String query) {
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("The query is empty. Give a few search keywords.");
		}
		if (query.length() > MAX_QUERY_LENGTH) {
			throw new IllegalArgumentException("The query is too long (" + MAX_QUERY_LENGTH + " characters at most). Use a few keywords.");
		}
		final List<String> articles = search(query.strip()).stream().map(WikipediaTools::describe).toList();
		return articles.isEmpty() ? "No Wikipedia article matches '" + query.strip() + "'. Try different keywords."
								  : String.join("\n\n", articles);
	}

	private List<JsonNode> search(final String query) {
		final JsonNode body;
		try {
			body = restClient.get()
							 .uri(uri -> uri.path("/w/api.php")
											.queryParam("action", "query")
											.queryParam("format", "json")
											.queryParam("generator", "search")
											.queryParam("gsrsearch", "{q}")
											.queryParam("gsrlimit", maxResults)
											.queryParam("prop", "extracts|info")
											.queryParam("exintro", "1")
											.queryParam("explaintext", "1")
											.queryParam("exlimit", "max")
											.queryParam("inprop", "url")
											.build(query))
							 .retrieve()
							 .body(JsonNode.class);
		} catch (RestClientException ex) {
			throw new IllegalStateException("Wikipedia could not be reached right now. Tell the user the search is unavailable.", ex);
		}
		if (body == null) {
			throw new IllegalStateException("Wikipedia gave an empty answer. Tell the user the search is unavailable.");
		}
		// No match: the query object is missing. Matches come unordered in a map; "index" is the search rank.
		final List<JsonNode> ranked = new ArrayList<>();
		body.path("query").path("pages").forEach(ranked::add);
		ranked.sort((a, b) -> Integer.compare(a.path("index").asInt(), b.path("index").asInt()));
		return ranked;
	}

	private static String describe(final JsonNode page) {
		String extract = page.path("extract").asString("").strip();
		if (extract.length() > MAX_EXTRACT_CHARS) {
			extract = extract.substring(0, MAX_EXTRACT_CHARS).strip() + "...";
		}
		return page.path("title").asString("") + "\n" + (extract.isEmpty() ? "(no intro text)" : extract) + "\n"
			   + page.path("fullurl").asString("");
	}
}