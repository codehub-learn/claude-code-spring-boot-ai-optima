/**
 * Unit 9.1: stateful conversations. The server keeps each conversation's history and sends it back to the model on every turn; the client
 * only sends the conversation id (in the URL) and the new message. Contrast with the stateless {@code chat} endpoints (6.2, 6.3), where the
 * model sees only what the request carries.
 */
@NullMarked
package gr.codelearn.springbootai.ai.conversation;

import org.jspecify.annotations.NullMarked;