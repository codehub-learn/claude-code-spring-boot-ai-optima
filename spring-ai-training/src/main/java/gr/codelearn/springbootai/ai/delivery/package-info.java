/**
 * Unit 11.3: a tool that depends on something outside the application. {@code DeliveryTools} offers a delivery estimate to a model;
 * {@code DeliveryService} combines the catalog with an external carrier service, reached through {@code CarrierQuoteClient}. The carrier
 * can be down, slow, busy or unwilling, and each of those reaches the model as a message it can act on (see
 * {@code config.ai.ToolErrorProcessor}), never as a stack trace.
 */
@NullMarked
package gr.codelearn.springbootai.ai.delivery;

import org.jspecify.annotations.NullMarked;
