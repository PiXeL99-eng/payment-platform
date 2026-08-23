package com.sayantan.payment_platform.service;

import org.springframework.stereotype.Service;

import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentRequest;

@Service
public class PaymentService {
    public Payment createPayment(PaymentRequest request) {
        return new Payment(
            "PAY-1001",
            request.orderId(),
            request.amount(),
            request.currency(),
            "CREATED"
        );
    }
}
