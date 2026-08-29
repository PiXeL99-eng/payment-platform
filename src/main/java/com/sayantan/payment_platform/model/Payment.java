package com.sayantan.payment_platform.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    @Column(precision = 19, scale = 2) // Total no of digits = 19, Digits after decimal point = 2
    private BigDecimal amount;

    private String currency;

    // If we didn't mention EnumType as String, JPA saves Enums into our database as default integers = EnumType.ORDINAL
    // Ordinal means first Enum value is 0, then 1, then 2... This could create issues later
    // Read on what issues can happen..
    @Enumerated(EnumType.STRING) 
    private PaymentStatus status;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    public Payment() {
    }

    public Payment(
        String orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String idempotencyKey
    ) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }
}
