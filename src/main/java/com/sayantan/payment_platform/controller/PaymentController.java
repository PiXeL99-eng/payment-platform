package com.sayantan.payment_platform.controller;

import org.springframework.web.bind.annotation.RestController;

import com.sayantan.payment_platform.model.Payment;
import com.sayantan.payment_platform.model.PaymentRequest;
import com.sayantan.payment_platform.service.PaymentService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
    public Payment createPayment(@RequestBody PaymentRequest request) { 
        // @RequestBody annotation tells Spring to take the JSON data sent at this POST endpoint, 
        // and convert it to PaymentRequest Class

        return paymentService.createPayment(request);
    }
    
}
