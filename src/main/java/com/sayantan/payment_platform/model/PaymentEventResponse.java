package com.sayantan.payment_platform.model;

import java.time.Instant;

public record PaymentEventResponse(
    Long id,
    Long paymentId,
    PaymentStatus status,
    Instant createdAt
) {}

