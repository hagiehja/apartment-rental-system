package com.example.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分期计划DTO
 */
@Data
public class InstallmentDTO {
    private Long installmentId;
    private Integer periodNo; // 期数
    private BigDecimal amount; // 应付金额
    private LocalDate dueDate; // 应付日期
    private String paymentStatus; // 支付状态
    private String paymentNo; // 支付单号
    private LocalDateTime paymentTime; // 支付时间
}
