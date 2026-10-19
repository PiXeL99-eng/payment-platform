package com.sayantan.payment_platform.exception;

public class GatewayUnavailableException extends RuntimeException {

    public GatewayUnavailableException(String message) {
        super(message);
    }

    public GatewayUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

