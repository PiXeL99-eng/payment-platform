package com.sayantan.payment_platform.integration.gateway.model;

import java.math.BigDecimal;

public record GatewayPaymentRequest(
    Long paymentId,
    String orderId,
    BigDecimal amount,
    String currency
) {}
