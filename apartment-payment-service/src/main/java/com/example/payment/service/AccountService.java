package com.example.payment.service;

import com.example.payment.dto.AccountBalanceDTO;

import java.math.BigDecimal;

/**
 * 账户服务接口
 */
public interface AccountService {

    /**
     * 查询账户余额
     */
    AccountBalanceDTO getBalance(Long userId);

    /**
     * 扣款（支付）
     */
    void deductBalance(Long userId, BigDecimal amount, String relatedNo, String remark);

    /**
     * 退款
     */
    void refundBalance(Long userId, BigDecimal amount, String relatedNo, String remark);

    /**
     * 收款（给房东）
     */
    void receiveIncome(Long landlordId, BigDecimal amount, String relatedNo, String remark);

    /**
     * 查询交易记录
     */
    java.util.List<com.example.payment.entity.AccountTransaction> getTransactionList(Long userId);
}
