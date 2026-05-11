package com.example.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付单返回DTO
 */
@Data
public class PaymentCreateResultDTO {
    private String paymentNo; // 支付单号
    private BigDecimal amount; // 支付金额
    private BigDecimal currentBalance; // 当前余额
    private BigDecimal balanceAfterPayment; // 支付后余额
}
