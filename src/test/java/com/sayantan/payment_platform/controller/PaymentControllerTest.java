package com.sayantan.payment_platform.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sayantan.payment_platform.exception.GatewayTimeoutException;
import com.sayantan.payment_platform.exception.GatewayUnavailableException;
import com.sayantan.payment_platform.exception.GlobalExceptionHandler;
import com.sayantan.payment_platform.exception.IdempotencyConflictException;
import com.sayantan.payment_platform.exception.PaymentNotFoundException;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.model.PaymentStatus;
import com.sayantan.payment_platform.service.PaymentService;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    private static final String VALID_REQUEST = """
        {
          "orderId": "ORDER-1001",
          "amount": 150.00,
          "currency": "INR"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void validPaymentRequestReturnsPaymentResponse() throws Exception {
        PaymentResponse response = new PaymentResponse(
            42L,
            "ORDER-1001",
            new BigDecimal("150.00"),
            "INR",
            PaymentStatus.SUCCESS
        );

        when(paymentService.createPayment(any(PaymentRequest.class), eq("key-1")))
            .thenReturn(response);

        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void invalidPaymentBodyReturnsBadRequest() throws Exception {
        String invalidRequest = """
            {
              "orderId": "",
              "amount": 0,
              "currency": ""
            }
            """;

        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "key-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message", containsString("Order ID is required")))
            .andExpect(jsonPath("$.message", containsString("Amount must be greater than zero")))
            .andExpect(jsonPath("$.message", containsString("Currency is required")));
    }

    @Test
    void missingIdempotencyKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Idempotency-Key header is required"));
    }

    @Test
    void blankIdempotencyKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", " ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Idempotency-Key must not be blank"));
    }

    @Test
    void unknownPaymentReturnsNotFound() throws Exception {
        when(paymentService.getPayment(999L))
            .thenThrow(new PaymentNotFoundException(999L));

        mockMvc.perform(get("/payments/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void idempotencyConflictReturnsConflict() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class), eq("conflict-key")))
            .thenThrow(new IdempotencyConflictException(
                "Idempotency-Key was already used for a different payment request"
            ));

        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "conflict-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void gatewayUnavailableReturnsBadGateway() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class), eq("unavailable-key")))
            .thenThrow(new GatewayUnavailableException("Could not connect to payment gateway"));

        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "unavailable-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void gatewayTimeoutReturnsGatewayTimeout() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class), eq("timeout-key")))
            .thenThrow(new GatewayTimeoutException(
                "Payment gateway did not respond in time",
                new RuntimeException("timeout")
            ));

        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "timeout-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.status").value(504));
    }
}

