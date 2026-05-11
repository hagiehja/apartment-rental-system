package com.example.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRefundEvent {
    private Long contractId;
    private Long userId;
    private BigDecimal refundAmount;
    private String reason;
}
