package com.example.notification.dto;

import lombok.Data;

import java.util.Map;

/**
 * 发送消息DTO
 */
@Data
public class SendNotificationDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板参数(用于替换模板中的变量)
     */
    private Map<String, String> params;

    /**
     * 业务ID(可选,如订单ID、合同ID等)
     */
    private Long bizId;
}
