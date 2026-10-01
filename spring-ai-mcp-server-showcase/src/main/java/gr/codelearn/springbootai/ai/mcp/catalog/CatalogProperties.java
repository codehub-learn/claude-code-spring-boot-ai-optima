package gr.codelearn.springbootai.ai.mcp.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Where the catalog lives and how much one call may return, bound from {@code app.catalog.*}.
 *
 * @param schema       the Oracle schema holding the {@code product} and {@code category} tables ({@code training} locally). It is written
 *                     into the SQL, so it must be a plain identifier: the pattern keeps anything else out of the statement.
 * @param defaultLimit how many products a search returns when the caller names no limit
 * @param maxLimit     the most products one search may return; a bigger request is clamped, so one call can't flood the model's context
 * @param toolStyle    which annotation publishes the three catalog tools: {@code mcp} ({@code @McpTool}, the default) or {@code tool}
 *                     (Spring AI's {@code @Tool}, the 12.1 style). Only one runs, because two tools with one name would clash
 */
@Validated
@ConfigurationProperties("app.catalog")
record CatalogProperties(@DefaultValue("training") @NotNull @Pattern(regexp = "[A-Za-z][A-Za-z0-9_$#]{0,127}") String schema,
						 @DefaultValue("10") @Min(1) @Max(100) int defaultLimit,
						 @DefaultValue("20") @Min(1) @Max(100) int maxLimit,
						 @DefaultValue("mcp") @NotNull ToolStyle toolStyle) {

	enum ToolStyle {
		TOOL, MCP
	}
}