package com.example.order.dto;

import lombok.Data;
import java.util.List;

/**
 * 订单查询DTO
 */
@Data
public class OrderQueryDTO {
    private String orderStatus; // 订单状态
    private List<String> orderStatusList; // 订单状态列表
    private String paymentStatus; // 支付状态
    private Integer pageNum = 1; // 页码
    private Integer pageSize = 10; // 每页大小
}
