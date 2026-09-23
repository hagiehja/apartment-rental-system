package com.example.payment.enums;

/**
 * 交易类型枚举
 */
public enum TransactionType {
    PAYMENT("支付"),
    REFUND("退款"),
    INCOME("收入");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
