package com.example.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignedEvent {
    private Long contractId;
    private Long userId; // The person who performed the signing action
    private String userType;
}
