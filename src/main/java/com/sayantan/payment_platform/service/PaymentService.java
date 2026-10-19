package com.sayantan.payment_platform.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sayantan.payment_platform.exception.PaymentNotFoundException;
import com.sayantan.payment_platform.integration.gateway.MockPaymentGatewayClient;
import com.sayantan.payment_platform.integration.gateway.model.GatewayPaymentRequest;
import com.sayantan.payment_platform.integration.gateway.model.GatewayPaymentResponse;
import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentEvent;
import com.sayantan.payment_platform.model.PaymentEventResponse;
import com.sayantan.payment_platform.model.PaymentPreparation;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.model.PaymentStatus;
import com.sayantan.payment_platform.repository.PaymentEventRepository;
import com.sayantan.payment_platform.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentPersistenceService paymentPersistenceService;
    private final MockPaymentGatewayClient mockPaymentGatewayClient;

    public PaymentService(
        PaymentRepository paymentRepository,
        PaymentEventRepository paymentEventRepository,
        PaymentPersistenceService paymentPersistenceService,
        MockPaymentGatewayClient mockPaymentGatewayClient
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.paymentPersistenceService = paymentPersistenceService;
        this.mockPaymentGatewayClient = mockPaymentGatewayClient;
    }

    public PaymentResponse createPayment(
        PaymentRequest request,
        String idempotencyKey
    ) {
        PaymentPreparation paymentPreparation;

        try {
            paymentPreparation = paymentPersistenceService.preparePayment(
                request,
                idempotencyKey
            );
        } catch (DataIntegrityViolationException ex) {
            // concurrent idempotency-key collision recovery.
            paymentPreparation = paymentPersistenceService
                .findExistingPayment(request, idempotencyKey)
                .orElseThrow(() -> ex);
        }

        if (!paymentPreparation.newlyCreated()) {
            return paymentPreparation.payment();
        }

        GatewayPaymentRequest gatewayPaymentRequest = toGatewayPaymentRequest(paymentPreparation.payment());
        
        GatewayPaymentResponse gatewayPaymentResponse = mockPaymentGatewayClient.processPayment(gatewayPaymentRequest);

        PaymentStatus finalStatus = switch(gatewayPaymentResponse.status()) {
            case APPROVED -> PaymentStatus.SUCCESS;
            case DECLINED -> PaymentStatus.FAILED;
        };

        return paymentPersistenceService.completePayment(
            paymentPreparation.payment().id(),
            finalStatus
        );
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

    public List<PaymentEventResponse> getPaymentEvents(Long paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new PaymentNotFoundException(paymentId);
        }

        return paymentEventRepository
            .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
            .stream()
            .map(this::toEventResponse)
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

    private GatewayPaymentRequest toGatewayPaymentRequest(PaymentResponse paymentResponse) {
        return new GatewayPaymentRequest(
            paymentResponse.id(),
            paymentResponse.orderId(),
            paymentResponse.amount(),
            paymentResponse.currency()
        );
    }

    private PaymentEventResponse toEventResponse(PaymentEvent event) {
        return new PaymentEventResponse(
            event.getId(),
            event.getPaymentId(),
            event.getStatus(),
            event.getCreatedAt()
        );
    }

}
