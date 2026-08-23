package com.sayantan.payment_platform.model;

public record PaymentRequest(
    String orderId,
    double amount,
    String currency
) {
}
