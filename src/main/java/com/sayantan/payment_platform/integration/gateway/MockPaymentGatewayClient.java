package com.sayantan.payment_platform.integration.gateway;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sayantan.payment_platform.integration.gateway.model.GatewayPaymentRequest;
import com.sayantan.payment_platform.integration.gateway.model.GatewayPaymentResponse;

@Component  // Makes this class available for Injection
public class MockPaymentGatewayClient {
    private final RestClient restClient;

    public MockPaymentGatewayClient(RestClient mockPaymentGatewayRestClient) {
        this.restClient = mockPaymentGatewayRestClient;
    }

    public GatewayPaymentResponse processPayment(
        GatewayPaymentRequest request
    ) {
        return restClient.post()
        .uri("/gateway/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .body(GatewayPaymentResponse.class);
    }
}
