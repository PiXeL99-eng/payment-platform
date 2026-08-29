package com.sayantan.payment_platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sayantan.payment_platform.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> { // Here Long is the Primary Key Type of the Entity managed by this Repository 
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
