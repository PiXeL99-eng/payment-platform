package com.sayantan.payment_platform.exception;

public class GatewayTimeoutException extends RuntimeException {

    public GatewayTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

