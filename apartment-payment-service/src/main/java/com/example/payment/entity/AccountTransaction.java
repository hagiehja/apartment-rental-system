package com.example.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户流水实体类
 */
@Data
@TableName("account_transaction")
public class AccountTransaction implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long transactionId; // 流水ID

    private String transactionNo; // 流水号

    private Long userId; // 用户ID

    private BigDecimal amount; // 交易金额

    private String transactionType; // 交易类型

    private BigDecimal balanceBefore; // 交易前余额

    private BigDecimal balanceAfter; // 交易后余额

    private String relatedNo; // 关联单号

    private String remark; // 备注

    private LocalDateTime createTime; // 创建时间
}
