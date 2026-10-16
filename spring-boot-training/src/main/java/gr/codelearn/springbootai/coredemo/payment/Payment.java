package gr.codelearn.springbootai.coredemo.payment;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

public record Payment(long id, long orderId, BigDecimal amount, PaymentMethod method, PaymentStatus status, @Nullable Instant paidAt) {
}
