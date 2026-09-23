package com.example.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户余额DTO
 */
@Data
public class AccountBalanceDTO {
    private Long userId; // 用户ID
    private BigDecimal balance; // 可用余额
    private BigDecimal frozenAmount; // 冻结金额
    private BigDecimal totalIncome; // 累计收入
    private BigDecimal totalExpense; // 累计支出
}
