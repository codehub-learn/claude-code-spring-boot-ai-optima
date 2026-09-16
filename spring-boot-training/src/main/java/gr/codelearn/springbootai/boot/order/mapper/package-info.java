/**
 * Record&#8596;record mapping between the order wire contract ({@code order.dto}) and the internal command/result types
 * ({@code order.internal}) — {@code PlaceOrderRequest} &#8594; {@code PlaceOrderCommand} in, {@code OrderConfirmation} &#8594;
 * {@code OrderResponse} out (U4.1). No entity in sight, so {@code OrderController} calls it directly. U4.4 folds it into a MapStruct
 * mapper.
 */
@NullMarked
package gr.codelearn.springbootai.boot.order.mapper;

import org.jspecify.annotations.NullMarked;