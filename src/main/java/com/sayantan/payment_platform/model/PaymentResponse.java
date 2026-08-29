package com.sayantan.payment_platform.model;

import java.math.BigDecimal;

public record PaymentResponse(
    Long id,
    String orderId,
    BigDecimal amount,
    String currency,
    PaymentStatus status
) {
    
}
