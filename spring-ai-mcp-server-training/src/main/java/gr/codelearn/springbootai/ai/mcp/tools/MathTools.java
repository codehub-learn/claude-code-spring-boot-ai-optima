package gr.codelearn.springbootai.ai.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A second toy tool: exact decimal addition, which language models get wrong more often than they should.
 */
@Component
public class MathTools {

	@Tool(description = "Adds two decimal numbers exactly and returns the sum as text. Use it instead of adding numbers yourself.")
	public String add(@ToolParam(description = "First number, for example 19.99") final BigDecimal a,
					  @ToolParam(description = "Second number, for example 0.01") final BigDecimal b) {
		return a.add(b).toPlainString();
	}
}
