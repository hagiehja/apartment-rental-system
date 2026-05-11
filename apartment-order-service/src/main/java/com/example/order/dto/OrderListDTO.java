package com.example.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单列表DTO
 */
@Data
public class OrderListDTO {
    private String orderNo;
    private Long houseId;
    private String houseTitle;
    private String houseAddress;
    private LocalDate rentStartDate;
    private LocalDate rentEndDate;
    private Integer rentMonths;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private Boolean installmentEnabled;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}
