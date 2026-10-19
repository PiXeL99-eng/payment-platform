package com.sayantan.payment_platform.integration.gateway;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.sayantan.payment_platform.exception.GatewayTimeoutException;
import com.sayantan.payment_platform.exception.GatewayUnavailableException;
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
        try {
            GatewayPaymentResponse response = restClient.post()
            .uri("/gateway/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(GatewayPaymentResponse.class);

            if (response == null) {
                throw new GatewayUnavailableException(
                    "Payment gateway returned an empty response"
                );
            }

            return response;
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new GatewayTimeoutException(
                    "Payment gateway did not respond in time",
                    ex
                );
            }

            throw new GatewayUnavailableException(
                "Could not connect to payment gateway",
                ex
            );
        } catch (RestClientException ex) {
            throw new GatewayUnavailableException(
                "Payment gateway returned an invalid response",
                ex
            );
        }
    }

    // Java exceptions can wrap other exceptions.
    // This method checks whether an exception — or any exception wrapped inside it — was ultimately caused by a timeout.
    private boolean isTimeout(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                || cause instanceof SocketTimeoutException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
