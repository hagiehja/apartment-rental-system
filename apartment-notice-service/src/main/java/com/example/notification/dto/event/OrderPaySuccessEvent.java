package com.example.notification.dto.event;

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
    private String houseId;
    // 房源标题，用于消息内容展示
    private String houseTitle;
}
