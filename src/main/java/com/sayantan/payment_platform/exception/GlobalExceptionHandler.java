package com.sayantan.payment_platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // Any Exception that any Controller throws, will be caught here, it's like a try-catch
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Converts a Server side Exception throw to HTTP Error like 404 Not Found
    public ErrorResponse handlePaymentNotFound(
        PaymentNotFoundException ex
    ) {
        return new ErrorResponse(
            404,
            ex.getMessage()
        );
    }
}
