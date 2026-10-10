package com.sayantan.payment_platform.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sayantan.payment_platform.exception.InvalidPaymentStateException;
import com.sayantan.payment_platform.exception.PaymentNotFoundException;
import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentEvent;
import com.sayantan.payment_platform.model.PaymentPreparation;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.model.PaymentStatus;
import com.sayantan.payment_platform.repository.PaymentEventRepository;
import com.sayantan.payment_platform.repository.PaymentRepository;

@Service 
public class PaymentPersistenceService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentPersistenceService(
        PaymentRepository paymentRepository,
        PaymentEventRepository paymentEventRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Transactional  // If i throw RuntimeException in the below code anywhere, Spring automatically Rolls Back the Database
    public PaymentPreparation preparePayment(
        PaymentRequest request,
        String idempotencyKey
    ) {

        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        if (existingPayment.isPresent()) {
            return new PaymentPreparation(
                toResponse(existingPayment.get()),
                false
            );
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

        savedPayment.setStatus(PaymentStatus.PROCESSING);

        PaymentEvent paymentEvent2 = new PaymentEvent(
            savedPayment.getId(),
            savedPayment.getStatus()
        );

        paymentEventRepository.save(paymentEvent2);

        return new PaymentPreparation(
            toResponse(savedPayment),
            true
        );
    }

    @Transactional
    public PaymentResponse completePayment(
        Long paymentId,
        PaymentStatus finalStatus
    ) {
        if (finalStatus != PaymentStatus.SUCCESS && finalStatus != PaymentStatus.FAILED) {
            throw new IllegalArgumentException(
                "Final payment status must be SUCCESS or FAILED"
            );
        }

        Payment payment = paymentRepository
        .findById(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStateException(
                "Payment must be PROCESSING before completion"
            );
        }

        payment.setStatus(finalStatus);

        PaymentEvent paymentEvent = new PaymentEvent(
            payment.getId(),
            finalStatus
        );

        paymentEventRepository.save(paymentEvent);

        return toResponse(payment);
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
