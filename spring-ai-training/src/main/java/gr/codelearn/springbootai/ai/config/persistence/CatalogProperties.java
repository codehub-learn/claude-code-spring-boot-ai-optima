package gr.codelearn.springbootai.ai.config.persistence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Where spring-boot-training's catalog lives, bound from {@code app.ai.catalog.*}. Read by two features: {@code knowledge} (10.1, to embed
 * it) and {@code product} (11.1, to look products up live). Unit 11.1 moved it here from {@code app.ai.knowledge.catalog-schema}.
 *
 * @param schema the Oracle schema that holds the {@code product} and {@code category} tables ({@code training} locally, readable by
 *               {@code ai_training} through {@code compose/oracle-startdb}). It is written into the SQL, so it must be a plain identifier:
 *               the pattern keeps anything else out of the statement.
 */
@Validated
@ConfigurationProperties("app.ai.catalog")
public record CatalogProperties(@DefaultValue("training") @NotNull @Pattern(regexp = "[A-Za-z][A-Za-z0-9_$#]{0,127}") String schema) {
}