package com.example.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情DTO
 */
@Data
public class OrderDetailDTO {
    private Long orderId;
    private String orderNo;
    private Long tenantId;
    private Long houseId;
    private String houseTitle; // 房源标题
    private String houseAddress; // 房源地址
    private Long landlordId;
    private String landlordName; // 房东姓名
    private LocalDate rentStartDate;
    private LocalDate rentEndDate;
    private Integer rentMonths;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private BigDecimal totalAmount;
    private BigDecimal firstPaymentAmount;
    private Boolean installmentEnabled;
    private String orderStatus;
    private String paymentStatus;
    private String remark;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private LocalDateTime refundTime;
    private String cancelReason;

    // 分期计划
    private List<InstallmentDTO> installments;
}
