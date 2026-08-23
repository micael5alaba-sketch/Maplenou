package com.maplenou.backend.order;

public enum OrderStatus {
    CREATED,
    PAID,
    CLOSED,
    CANCELLED,
    PAYMENT_FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
