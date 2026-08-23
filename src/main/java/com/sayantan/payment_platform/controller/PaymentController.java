package com.sayantan.payment_platform.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController // Annotation that tells Spring that this class contains HTTP endpoints
public class PaymentController {

    // Annotation that tells the when HTTP GET request comes to /hello endpoint, execute this method
    @GetMapping("/hello")
    public String hello() {
        return "Payment service is running";
    }
}
