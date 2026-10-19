package com.sayantan.payment_platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sayantan.payment_platform.model.PaymentEvent;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAscIdAsc(Long paymentId);
}
