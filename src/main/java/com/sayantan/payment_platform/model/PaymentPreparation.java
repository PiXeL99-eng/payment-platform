package com.sayantan.payment_platform.model;

public record PaymentPreparation(
    PaymentResponse payment,
    boolean newlyCreated
) {}
