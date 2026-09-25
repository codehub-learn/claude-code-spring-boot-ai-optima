/**
 * Unit 8.3: structured outputs. A supplier's free-text product blurb becomes a validated {@code ExtractedProduct} record: Spring AI's
 * {@code responseEntity(...)} maps the completion, its {@code StructuredOutputValidationAdvisor} re-prompts on schema violations, and
 * Jakarta Bean Validation checks the business rules the schema can't carry.
 */
@NullMarked
package gr.codelearn.springbootai.ai.extraction;

import org.jspecify.annotations.NullMarked;