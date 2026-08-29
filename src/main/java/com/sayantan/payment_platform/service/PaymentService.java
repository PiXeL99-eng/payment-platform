package com.sayantan.payment_platform.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sayantan.payment_platform.exception.PaymentNotFoundException;
import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentEvent;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.model.PaymentStatus;
import com.sayantan.payment_platform.repository.PaymentEventRepository;
import com.sayantan.payment_platform.repository.PaymentRepository;

@Service
public class PaymentService {

    public PaymentRepository paymentRepository;
    public PaymentEventRepository paymentEventRepository;

    public PaymentService(
        PaymentRepository paymentRepository,
        PaymentEventRepository paymentEventRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Transactional  // If i throw RuntimeException in the below code anywhere, Spring automatically Rolls Back the Database
    public PaymentResponse createPayment(
        PaymentRequest request,
        String idempotencyKey
    ) {

        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        if (existingPayment.isPresent()) {
            return toResponse(existingPayment.get());
        }

        Payment payment = new Payment(
            request.orderId(),
            request.amount(),
            request.currency(),
            PaymentStatus.CREATED,
            idempotencyKey
        );

        Payment savedPayment = paymentRepository.save(payment);

        // throw new RuntimeException("Testing transaction rollback");

        PaymentEvent paymentEvent = new PaymentEvent(
            savedPayment.getId(),
            savedPayment.getStatus()
        );

        paymentEventRepository.save(paymentEvent);

        return toResponse(savedPayment);
    }

    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository
        .findById(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));

        return toResponse(payment);
    }

    public Page<PaymentResponse> getPayments(Pageable pageable) {
        return paymentRepository
        .findAll(pageable)  // Spring JPA repository already provides findAll with pageable parameter, so it takes pageable object and returns a Page of Payments (like a list of Payments)
        .map(this::toResponse);     // Apply map function to each Payment, and convert it to PaymentResponse
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
