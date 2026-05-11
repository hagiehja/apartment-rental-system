package com.example.contract.dto;

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
}