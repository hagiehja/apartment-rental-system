package com.example.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单返回结果DTO
 */
@Data
public class OrderCreateResultDTO {
    private String orderNo; // 订单编号
    private BigDecimal totalAmount; // 订单总金额
    private BigDecimal deposit; // 押金
    private BigDecimal firstPaymentAmount; // 首付金额
    private Boolean installmentEnabled; // 是否分期
    private LocalDateTime expireTime; // 过期时间
}
