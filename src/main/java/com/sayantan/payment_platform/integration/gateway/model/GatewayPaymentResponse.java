package com.sayantan.payment_platform.integration.gateway.model;

public record GatewayPaymentResponse(
    String gatewayTransactionId,
    GatewayPaymentStatus status,
    String message
) {}
