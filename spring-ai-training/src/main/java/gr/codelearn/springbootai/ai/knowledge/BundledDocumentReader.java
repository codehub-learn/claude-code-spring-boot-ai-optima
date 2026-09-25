package gr.codelearn.springbootai.ai.knowledge;

import org.springframework.ai.document.Document;
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
import java.util.Map;
import java.util.Objects;

/**
 * Unit 10.1: the store's policy documents ({@code classpath:knowledge/*.md}: returns, warranty, shipping, a laptop buying guide), one
 * {@link Document} per file, whole. They are short enough to embed in one piece; splitting longer text into chunks is 10.2.
 *
 * <p>Same shape as the catalog documents: the text starts with the citation tag {@code [doc:name]} (the file name without
 * {@code .md}), the title is the file's first {@code #} heading, the id is derived from the name.
 */
@Component
class BundledDocumentReader {

	static final String SOURCE = "doc";

	private static final String LOCATION = "classpath:knowledge/*.md";

	private final ResourcePatternResolver resolver;

	BundledDocumentReader(final ResourceLoader resourceLoader) {
		this.resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}

	List<Document> read() {
		try {
			return Arrays.stream(resolver.getResources(LOCATION))
						 .sorted(Comparator.comparing(resource -> Objects.requireNonNullElse(resource.getFilename(), "")))
						 .map(BundledDocumentReader::toDocument)
						 .toList();
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot list " + LOCATION, ex);
		}
	}

	private static Document toDocument(final Resource resource) {
		final String filename = Objects.requireNonNull(resource.getFilename());
		final String name = filename.substring(0, filename.length() - ".md".length());
		final String content;
		try {
			content = resource.getContentAsString(StandardCharsets.UTF_8).strip();
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + filename, ex);
		}
		final String title = content.lines().filter(line -> line.startsWith("# ")).findFirst().map(line -> line.substring(2)).orElse(name);
		return Document.builder()
					   .id(CatalogDocumentReader.idFor(SOURCE, name))
					   .text("[%s:%s] %s".formatted(SOURCE, name, content))
					   .metadata(Map.of("source", SOURCE, "ref", name, "title", title))
					   .build();
	}
}