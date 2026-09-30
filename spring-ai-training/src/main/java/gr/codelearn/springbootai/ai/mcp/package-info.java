/**
 * Unit 11.4: the MCP client half. The tools of a remote MCP server ({@code spring-ai-mcp-server-training}) are discovered over HTTP and
 * offered to the model on this feature's calls, exactly like the local {@code @Tool} methods of {@code assistant}; the model cannot tell
 * the difference, the application can (a network hop, a server that may be down, tools it does not own).
 */
@NullMarked
package gr.codelearn.springbootai.ai.mcp;

import org.jspecify.annotations.NullMarked;
