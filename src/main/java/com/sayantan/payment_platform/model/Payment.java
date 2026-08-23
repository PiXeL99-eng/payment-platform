package com.sayantan.payment_platform.model;

public record Payment(
    String paymentId,
    String orderId,
    double amount,
    String currency,
    String status
) {
}