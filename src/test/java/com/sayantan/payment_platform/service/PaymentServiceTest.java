package com.sayantan.payment_platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sayantan.payment_platform.exception.GatewayTimeoutException;
import com.sayantan.payment_platform.integration.gateway.MockPaymentGatewayClient;
import com.sayantan.payment_platform.integration.gateway.model.GatewayPaymentRequest;
import com.sayantan.payment_platform.integration.gateway.model.GatewayPaymentResponse;
import com.sayantan.payment_platform.integration.gateway.model.GatewayPaymentStatus;
import com.sayantan.payment_platform.model.PaymentPreparation;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.model.PaymentStatus;
import com.sayantan.payment_platform.repository.PaymentEventRepository;
import com.sayantan.payment_platform.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventRepository paymentEventRepository;

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

    @Mock
    private MockPaymentGatewayClient mockPaymentGatewayClient;

    private PaymentService paymentService;
    private PaymentRequest request;
    private PaymentResponse processingPayment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
            paymentRepository,
            paymentEventRepository,
            paymentPersistenceService,
            mockPaymentGatewayClient
        );

        request = new PaymentRequest(
            "ORDER-1001",
            new BigDecimal("150.00"),
            "INR"
        );

        processingPayment = new PaymentResponse(
            42L,
            request.orderId(),
            request.amount(),
            request.currency(),
            PaymentStatus.PROCESSING
        );
    }

    @Test
    void approvedGatewayResponseCompletesPaymentAsSuccess() {
        when(paymentPersistenceService.preparePayment(request, "key-1"))
            .thenReturn(new PaymentPreparation(processingPayment, true));
        when(mockPaymentGatewayClient.processPayment(any(GatewayPaymentRequest.class)))
            .thenReturn(new GatewayPaymentResponse(
                "gw_approved",
                GatewayPaymentStatus.APPROVED,
                "Payment approved"
            ));

        PaymentResponse successfulPayment = withStatus(PaymentStatus.SUCCESS);
        when(paymentPersistenceService.completePayment(42L, PaymentStatus.SUCCESS))
            .thenReturn(successfulPayment);

        PaymentResponse result = paymentService.createPayment(request, "key-1");

        assertSame(successfulPayment, result);
        verify(paymentPersistenceService).completePayment(42L, PaymentStatus.SUCCESS);

        ArgumentCaptor<GatewayPaymentRequest> gatewayRequest =
            ArgumentCaptor.forClass(GatewayPaymentRequest.class);
        verify(mockPaymentGatewayClient).processPayment(gatewayRequest.capture());
        assertEquals(42L, gatewayRequest.getValue().paymentId());
        assertEquals("ORDER-1001", gatewayRequest.getValue().orderId());
        assertEquals(new BigDecimal("150.00"), gatewayRequest.getValue().amount());
        assertEquals("INR", gatewayRequest.getValue().currency());
    }

    @Test
    void declinedGatewayResponseCompletesPaymentAsFailed() {
        when(paymentPersistenceService.preparePayment(request, "key-2"))
            .thenReturn(new PaymentPreparation(processingPayment, true));
        when(mockPaymentGatewayClient.processPayment(any(GatewayPaymentRequest.class)))
            .thenReturn(new GatewayPaymentResponse(
                "gw_declined",
                GatewayPaymentStatus.DECLINED,
                "Payment declined"
            ));

        PaymentResponse failedPayment = withStatus(PaymentStatus.FAILED);
        when(paymentPersistenceService.completePayment(42L, PaymentStatus.FAILED))
            .thenReturn(failedPayment);

        PaymentResponse result = paymentService.createPayment(request, "key-2");

        assertSame(failedPayment, result);
        verify(paymentPersistenceService).completePayment(42L, PaymentStatus.FAILED);
    }

    @Test
    void existingIdempotentPaymentDoesNotCallGateway() {
        PaymentResponse existingPayment = withStatus(PaymentStatus.SUCCESS);
        when(paymentPersistenceService.preparePayment(request, "existing-key"))
            .thenReturn(new PaymentPreparation(existingPayment, false));

        PaymentResponse result = paymentService.createPayment(request, "existing-key");

        assertSame(existingPayment, result);
        verifyNoInteractions(mockPaymentGatewayClient);
        verify(paymentPersistenceService, never())
            .completePayment(any(), any());
    }

    @Test
    void gatewayTimeoutLeavesCompletionUncalled() {
        when(paymentPersistenceService.preparePayment(request, "timeout-key"))
            .thenReturn(new PaymentPreparation(processingPayment, true));
        when(mockPaymentGatewayClient.processPayment(any(GatewayPaymentRequest.class)))
            .thenThrow(new GatewayTimeoutException(
                "Payment gateway did not respond in time",
                new RuntimeException("timeout")
            ));

        assertThrows(
            GatewayTimeoutException.class,
            () -> paymentService.createPayment(request, "timeout-key")
        );

        verify(paymentPersistenceService, never())
            .completePayment(any(), any());
    }

    @Test
    void concurrentIdempotencyCollisionReturnsExistingPayment() {
        DataIntegrityViolationException collision =
            new DataIntegrityViolationException("duplicate key");
        when(paymentPersistenceService.preparePayment(request, "racing-key"))
            .thenThrow(collision);

        PaymentResponse existingPayment = withStatus(PaymentStatus.PROCESSING);
        when(paymentPersistenceService.findExistingPayment(request, "racing-key"))
            .thenReturn(Optional.of(new PaymentPreparation(existingPayment, false)));

        PaymentResponse result = paymentService.createPayment(request, "racing-key");

        assertSame(existingPayment, result);
        verifyNoInteractions(mockPaymentGatewayClient);
        verify(paymentPersistenceService, never())
            .completePayment(any(), any());
    }

    private PaymentResponse withStatus(PaymentStatus status) {
        return new PaymentResponse(
            processingPayment.id(),
            processingPayment.orderId(),
            processingPayment.amount(),
            processingPayment.currency(),
            status
        );
    }
}

