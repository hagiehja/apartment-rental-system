package com.example.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignedEvent {
    private Long contractId;
    private Long userId;
    private String userType;
}
