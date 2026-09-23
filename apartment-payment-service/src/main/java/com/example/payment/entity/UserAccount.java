package com.example.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户账户实体类
 */
@Data
@TableName("user_account")
public class UserAccount implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long accountId; // 账户ID

    private Long userId; // 用户ID

    private BigDecimal balance; // 账户余额

    private BigDecimal frozenAmount; // 冻结金额

    private BigDecimal totalIncome; // 累计收入

    private BigDecimal totalExpense; // 累计支出

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间
}
