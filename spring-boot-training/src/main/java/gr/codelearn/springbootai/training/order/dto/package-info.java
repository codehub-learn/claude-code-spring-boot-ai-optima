/**
 * REST request and response records for the order API — the public wire contract for {@code /api/v1/orders}. No entity leaks here
 * (CLAUDE.md); {@code order.mapper} bridges these and the internal command/result types, so U4.3 can reshape the wire contract without
 * touching {@code order.internal}. Bean Validation lands in U4.2.
 */
@NullMarked
package gr.codelearn.springbootai.training.order.dto;

import org.jspecify.annotations.NullMarked;
