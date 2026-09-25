package gr.codelearn.springbootai.ai.knowledge;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Unit 10.1: the store's policy documents, {@code classpath:knowledge/*.md}. Unit 10.2: read only; cutting them into chunks is
 * {@link DocumentChunker}'s job, once per {@link ChunkingStrategy}. Four are short (returns, warranty, shipping, a laptop buying guide),
 * four are long enough for chunking to matter (terms of sale, a care and troubleshooting guide, trade-in, business accounts).
 */
@Component
class BundledDocumentReader {

	static final String SOURCE = "doc";

	private static final String LOCATION = "classpath:knowledge/*.md";

	private final ResourcePatternResolver resolver;

	BundledDocumentReader(final ResourceLoader resourceLoader) {
		this.resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}

	List<BundledDocument> read() {
		try {
			return Arrays.stream(resolver.getResources(LOCATION))
						 .sorted(Comparator.comparing(resource -> Objects.requireNonNullElse(resource.getFilename(), "")))
						 .map(BundledDocumentReader::toBundledDocument)
						 .toList();
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot list " + LOCATION, ex);
		}
	}

	private static BundledDocument toBundledDocument(final Resource resource) {
		final String filename = Objects.requireNonNull(resource.getFilename());
		final String name = filename.substring(0, filename.length() - ".md".length());
		final String content;
		try {
			content = resource.getContentAsString(StandardCharsets.UTF_8).strip();
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + filename, ex);
		}
		final String title = content.lines().filter(line -> line.startsWith("# ")).findFirst().map(line -> line.substring(2)).orElse(name);
		return new BundledDocument(name, title, content);
	}

	/**
	 * One bundled Markdown file.
	 *
	 * @param name    the file name without {@code .md}: the {@code ref} in metadata and in the citation tag {@code [doc:name]}
	 * @param title   the first {@code #} heading
	 * @param content the Markdown text
	 */
	record BundledDocument(String name, String title, String content) {
	}
}