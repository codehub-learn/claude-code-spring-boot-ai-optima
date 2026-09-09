package gr.codelearn.springbootai.training.foundation.components;

import org.springframework.stereotype.Repository;

/**
 * {@code @Repository} — "a data-access component". Also a {@code @Component} for scanning, but it carries one real extra behaviour: Spring
 * registers a {@code PersistenceExceptionTranslationPostProcessor} that wraps vendor {@code SQLException}s thrown from these beans into
 * Spring's {@code DataAccessException} hierarchy. Nothing to translate here (no datasource until U3), so this is a stub that just names its
 * stereotype.
 */
@Repository
class MessageRepositoryStub implements MessageProvider {

	@Override
	public String stereotype() {
		return "repository";
	}

	@Override
	public String message() {
		return "message from a @Repository (no real datasource yet)";
	}
}