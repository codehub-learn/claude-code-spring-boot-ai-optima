package gr.codelearn.springbootai.ai.mcp.catalog;

/**
 * A category and how many products it holds, as returned by {@code listCategories}.
 */
public record CategorySummary(String name, int productCount) {
}