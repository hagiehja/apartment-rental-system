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
 * 分期付款计划实体类
 */
@Data
@TableName("installment_plan")
public class InstallmentPlan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long installmentId; // 分期ID

    private String orderNo; // 订单编号

    private Integer periodNo; // 期数（1表示首付）

    private BigDecimal amount; // 应付金额

    private LocalDate dueDate; // 应付日期

    private String paymentStatus; // 支付状态

    private String paymentNo; // 支付单号

    private LocalDateTime paymentTime; // 实际支付时间

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间
}
