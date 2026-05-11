package com.example.payment.service;

import com.example.payment.dto.PaymentCreateDTO;
import com.example.payment.dto.PaymentCreateResultDTO;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付单
     */
    PaymentCreateResultDTO createPayment(PaymentCreateDTO createDTO, Long userId);

    /**
     * 执行余额支付
     */
    void executeBalancePayment(String paymentNo, Long userId);

    /**
     * 申请退款
     */
    /**
     * 申请退款
     */
    void refundPayment(String paymentNo, Long userId);

    /**
     * 根据订单号退款
     */
    void refundPaymentByOrderNo(String orderNo, Long userId);
}
