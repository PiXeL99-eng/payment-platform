package com.sayantan.payment_platform.service;

import org.springframework.stereotype.Service;

import com.sayantan.payment_platform.exception.PaymentNotFoundException;
import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.repository.PaymentRepository;

@Service
public class PaymentService {

    public PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(PaymentRequest request) {
        Payment payment = new Payment(
            request.orderId(),
            request.amount(),
            request.currency(),
            "CREATED"
        );

        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long id) {
        return paymentRepository
        .findById(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
