package com.example.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房东收入记录实体类
 */
@Data
@TableName("landlord_income")
public class LandlordIncome {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房东ID
     */
    private Long landlordId;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 收入类型：RENTAL-租金收入/REFUND-退款扣减
     */
    private String incomeType;

    /**
     * 金额（正数为收入，负数为扣减）
     */
    private BigDecimal amount;

    /**
     * 租客支付总额
     */
    private BigDecimal totalPayment;

    /**
     * 押金金额
     */
    private BigDecimal depositAmount;

    /**
     * 分成比例（默认0.80即80%）
     */
    private BigDecimal commissionRate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
