package gr.codelearn.springbootai.ai.mcp.tools;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Where {@link WikipediaTools} searches and how long it waits ({@code app.wikipedia.*}). Without timeouts a slow Wikipedia would hold the
 * MCP call, and the model's tool loop behind it, for as long as it likes.
 *
 * @param baseUrl        the MediaWiki site, without a path
 * @param connectTimeout how long to wait for the connection
 * @param readTimeout    how long to wait for the answer
 * @param maxResults     how many articles one search returns
 */
@ConfigurationProperties("app.wikipedia")
public record WikipediaProperties(
		@DefaultValue("https://en.wikipedia.org") String baseUrl,
		@DefaultValue("2s") Duration connectTimeout,
		@DefaultValue("5s") Duration readTimeout,
		@DefaultValue("3") int maxResults) {
}
