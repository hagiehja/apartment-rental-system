package com.example.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单实体类
 */
@Data
@TableName("payment")
public class Payment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long paymentId; // 支付ID

    private String paymentNo; // 支付单号

    private String orderNo; // 关联订单号

    private Long installmentId; // 分期ID

    private Long userId; // 支付用户ID

    private BigDecimal amount; // 支付金额

    private String paymentMethod; // 支付方式

    private String paymentStatus; // 支付状态

    private BigDecimal refundAmount; // 退款金额

    private LocalDateTime refundTime; // 退款时间

    private LocalDateTime successTime; // 支付成功时间

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间
}
