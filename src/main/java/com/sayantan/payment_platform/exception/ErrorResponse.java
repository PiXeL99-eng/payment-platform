package com.sayantan.payment_platform.exception;

public record ErrorResponse(
    int status,
    String message
) {
    
}
