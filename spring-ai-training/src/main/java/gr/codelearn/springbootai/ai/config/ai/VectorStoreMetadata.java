package gr.codelearn.springbootai.ai.config.ai;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

/**
 * Reads a {@code Document} metadata value that came back from the Oracle vector store.
 */
public final class VectorStoreMetadata {

	private static final JsonMapper JSON = JsonMapper.shared();

	private VectorStoreMetadata() {
	}

	/**
	 * A metadata value as plain text. OracleVectorStore (2.0.1) hands back the driver's JSON values ({@code OracleJsonString}), whose
	 * {@code toString()} is the JSON text: {@code "LAP-0002"} with the quotes. Parsed as JSON, it is the string again.
	 */
	public static String text(final @Nullable Object value) {
		if (value == null || value instanceof String) {
			return Objects.requireNonNullElse((String) value, "");
		}
		final String json = value.toString();
		return json.startsWith("\"") ? JSON.readValue(json, String.class) : json;
	}
}