package gr.codelearn.springbootai.ai.answer;

import gr.codelearn.springbootai.ai.answer.dto.AnswerRequest;
import gr.codelearn.springbootai.ai.answer.dto.AnswerRequest.Document;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Unit 8.2: renders {@code prompts/answer/question.st}, one prompt in three tagged sections:
 *
 * <ul>
 *   <li>{@code <instructions>}: fixed text from the template file, the same on every call (only the wording for "with" or "without"
 *   documents is chosen);</li>
 *   <li>{@code <documents>}: the per-call context, one numbered {@code <document>} each, left out entirely when there is none;</li>
 *   <li>{@code <question>}: the per-call question, last, right before the model starts answering.</li>
 * </ul>
 *
 * <p>The tags mark where the application's text ends and the caller's data begins; the instructions tell the model to treat the documents
 * as reference material only. Values are never parsed as template syntax (8.1), so braces in a document stay as they are.
 *
 * <p>Each document is rendered on its own with {@code prompts/answer/document.st} and the results are listed by the main template. A single
 * template can't do it: Spring AI's variable check rejects a list element referenced more than once inside its element template
 * ({@code {document.title}} then {@code {document.text}}), see {@code PromptTemplatesTest}.
 */
@Component
class AnswerPrompts {

	private final PromptTemplate question;
	private final PromptTemplate document;

	AnswerPrompts(@Value("classpath:prompts/answer/question.st") final Resource question,
				  @Value("classpath:prompts/answer/document.st") final Resource document) {
		this.question = new PromptTemplate(question);
		this.document = new PromptTemplate(document);
	}

	String question(final AnswerRequest request) {
		final Map<String, @Nullable Object> variables = new HashMap<>();
		variables.put("question", request.question());
		variables.put("documents", documents(request.documents()));
		return question.render(variables);
	}

	/**
	 * Each document rendered and numbered from 1, or {@code null} when there are none, which leaves the {@code {if(documents)}} parts out.
	 */
	private @Nullable List<String> documents(final @Nullable List<Document> documents) {
		if (documents == null || documents.isEmpty()) {
			return null;
		}
		return IntStream.range(0, documents.size())
						.mapToObj(i -> document.render(Map.of("number", i + 1, "title", documents.get(i).title(), "text",
															  documents.get(i).text())))
						.toList();
	}
}