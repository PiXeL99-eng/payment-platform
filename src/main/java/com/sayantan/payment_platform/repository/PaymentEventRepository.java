package com.sayantan.payment_platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sayantan.payment_platform.model.PaymentEvent;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

}
