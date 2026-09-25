package gr.codelearn.springbootai.ai.knowledge;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Unit 10.1: where the knowledge base reads its sources, bound from {@code app.ai.knowledge.*}.
 *
 * @param catalogSchema the Oracle schema that holds spring-boot-training's {@code product} and {@code category} tables ({@code training}
 *                      locally, readable by {@code ai_training} through {@code compose/oracle-startdb}). It is written into the SQL, so it
 *                      must be a plain identifier: the pattern keeps anything else out of the statement.
 */
@Validated
@ConfigurationProperties("app.ai.knowledge")
record KnowledgeProperties(@DefaultValue("training") @NotNull @Pattern(regexp = "[A-Za-z][A-Za-z0-9_$#]{0,127}") String catalogSchema) {
}