package com.sayantan.payment_platform.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sayantan.payment_platform.exception.PaymentNotFoundException;
import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.model.PaymentStatus;
import com.sayantan.payment_platform.repository.PaymentRepository;

@Service
public class PaymentService {

    public PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse createPayment(PaymentRequest request) {
        Payment payment = new Payment(
            request.orderId(),
            request.amount(),
            request.currency(),
            PaymentStatus.CREATED
        );

        Payment savedPayment = paymentRepository.save(payment);
        return toResponse(savedPayment);
    }

    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository
        .findById(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));

        return toResponse(payment);
    }

    public List<PaymentResponse> getPayments() {
        return paymentRepository
        .findAll()
        .stream()       // .stream() takes the list and puts the items on like a conveyor belt one by one, so that we can apply functions like map(). we cannot directly apply .map on a list in Java
        .map(this::toResponse)
        .toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus()
        );
    }
}
