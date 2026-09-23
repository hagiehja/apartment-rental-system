package com.example.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付单DTO
 */
@Data
public class PaymentCreateDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo; // 订单号

    private Long installmentId; // 分期ID（可选）

    @NotNull(message = "支付方式不能为空")
    private String paymentMethod; // 支付方式
}
