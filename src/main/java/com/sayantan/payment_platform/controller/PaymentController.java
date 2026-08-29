package com.sayantan.payment_platform.controller;

import org.springframework.web.bind.annotation.RestController;

import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.model.PaymentResponse;
import com.sayantan.payment_platform.service.PaymentService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;



@RestController // Annotation that tells Spring that this class contains HTTP endpoints
public class PaymentController {

    public final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Annotation that tells the when HTTP GET request comes to /hello endpoint, execute this method
    @GetMapping("/hello")
    public String hello() {
        return "Payment service is running";
    }

    @PostMapping("/payments")
    public PaymentResponse createPayment(
        @Valid @RequestBody PaymentRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) { 
        // @RequestBody annotation tells Spring to take the JSON data sent at this POST endpoint, 
        // and convert it to PaymentRequest Class
        // @RequestHeader annotation takes the Header (Idempotency-Key: abc-123) and stores it in idempotencyKey String

        return paymentService.createPayment(request, idempotencyKey);
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse getPayment(@PathVariable Long id) {
        //@PathVariable is required when a parameter is part of the URL itself, like here id
        return paymentService.getPayment(id);
    }

    @GetMapping("/payments") // Takes URL parameters -> GET /payments?page=1&size=10; This also works -> GET http://localhost:8080/payments?page=0&size=5&sort=id,desc
    public Page<PaymentResponse> getPayments(
        @PageableDefault(size = 10) Pageable pageable   // Spring automatically takes the parameters and converts it to Pageable object
    ) {
        return paymentService.getPayments(pageable);
    }
    
}
