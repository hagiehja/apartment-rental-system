package com.example.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁订单实体类
 */
@Data
@TableName("rental_order")
public class RentalOrder implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long orderId; // 订单ID

    private String orderNo; // 订单编号

    private Long tenantId; // 租客用户ID

    private Long houseId; // 房源ID

    private Long landlordId; // 房东用户ID

    private LocalDate rentStartDate; // 租期开始日期

    private LocalDate rentEndDate; // 租期结束日期

    private Integer rentMonths; // 租赁月数

    private BigDecimal monthlyRent; // 月租金

    private BigDecimal deposit; // 押金

    private BigDecimal totalAmount; // 订单总金额

    private BigDecimal firstPaymentAmount; // 首付金额

    private Integer installmentEnabled; // 是否分期（0-否，1-是）

    private String orderStatus; // 订单状态

    private String paymentStatus; // 支付状态

    private String cancelReason; // 取消原因

    private String remark; // 备注

    private LocalDateTime expireTime; // 订单过期时间

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间

    private LocalDateTime payTime; // 支付完成时间

    private LocalDateTime cancelTime; // 取消时间

    private LocalDateTime refundTime; // 退款时间
}
