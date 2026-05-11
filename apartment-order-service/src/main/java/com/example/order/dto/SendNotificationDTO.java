package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationDTO {
    private Long userId;
    private String templateCode;
    private Map<String, String> params;
    private Long bizId;
}
