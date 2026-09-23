package com.example.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRefundEvent {
    private Long contractId;
    private Long userId; // The person who initiated termination (usually tenant)
    private BigDecimal refundAmount;
    private String reason;
}
