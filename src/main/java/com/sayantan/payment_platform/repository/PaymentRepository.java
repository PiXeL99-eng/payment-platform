package com.sayantan.payment_platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sayantan.payment_platform.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
}
