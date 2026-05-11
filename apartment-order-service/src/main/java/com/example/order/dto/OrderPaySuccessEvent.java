package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaySuccessEvent {
    private String orderNo;
    private Long tenantId;
    private Long landlordId;
    private BigDecimal amount;
    // 房源ID（String类型保持与前端兼容）
    private String houseId;
    // 房源标题，用于消息通知中展示具体房源名称
    private String houseTitle;
}
