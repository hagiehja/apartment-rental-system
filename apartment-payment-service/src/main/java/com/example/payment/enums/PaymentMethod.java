package com.example.payment.enums;

/**
 * 支付方式枚举
 */
public enum PaymentMethod {
    BALANCE("余额支付");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
