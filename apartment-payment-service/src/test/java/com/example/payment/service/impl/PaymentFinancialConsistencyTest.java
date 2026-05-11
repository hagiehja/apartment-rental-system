package com.example.payment.service.impl;

import com.example.payment.dto.AccountBalanceDTO;
import com.example.payment.entity.Payment;
import com.example.payment.entity.UserAccount;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.enums.TransactionType;
import com.example.payment.exception.BusinessException;
import com.example.payment.feign.OrderFeignClient;
import com.example.payment.mapper.AccountTransactionMapper;
import com.example.payment.mapper.PaymentMapper;
import com.example.payment.mapper.UserAccountMapper;
import com.example.payment.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentFinancialConsistencyTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private UserAccountMapper accountMapper;

    @Mock
    private AccountTransactionMapper transactionMapper;

    @Mock
    private OrderFeignClient orderFeignClient;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock // Mocking actual implementation to focus on logic flow if we were running full
          // context,
          // but here we want to test PaymentService logic calling AccountService.
          // Wait, PaymentServiceImpl calls AccountService.
          // If I mock AccountService, I test PaymentService's flow.
          // If I want to test atomic consistency, I should use the real AccountService or
          // verify calls.
    private AccountService accountService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        try {
            lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testExecuteBalancePayment_Success() {
        Long tenantId = 101L;
        Long landlordId = 202L;
        String paymentNo = "PAY123";
        String orderNo = "ORD123";
        BigDecimal amount = new BigDecimal("1000.00");

        // Mock Payment
        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderNo(orderNo);
        payment.setUserId(tenantId);
        payment.setAmount(amount);
        payment.setPaymentStatus("PENDING");
        when(paymentMapper.selectByPaymentNo(paymentNo)).thenReturn(payment);

        // Mock Order Feign to return Landlord ID
        Map<String, Object> orderRes = new HashMap<>();
        orderRes.put("code", 200);
        Map<String, Object> data = new HashMap<>();
        data.put("landlordId", landlordId);
        orderRes.put("data", data);
        when(orderFeignClient.getOrderDetail(orderNo, tenantId)).thenReturn(orderRes);

        // Execute
        paymentService.executeBalancePayment(paymentNo, tenantId);

        // Verify Tenant Deducted
        verify(accountService).deductBalance(eq(tenantId), eq(amount), eq(paymentNo), contains("支付订单"));

        // Verify Landlord Received (Critical for Financial Consistency)
        verify(accountService).receiveIncome(eq(landlordId), eq(amount), eq(paymentNo), contains("收到租金"));

        // Verify Payment Status Updated
        assertEquals("SUCCESS", payment.getPaymentStatus());
        verify(paymentMapper).updateById(payment);
    }

    @Test
    void testRefundPayment_Success() {
        Long tenantId = 101L;
        Long landlordId = 202L;
        String paymentNo = "PAY123";
        String orderNo = "ORD123";
        BigDecimal amount = new BigDecimal("1000.00");

        // Mock Payment
        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderNo(orderNo);
        payment.setUserId(tenantId);
        payment.setAmount(amount);
        payment.setPaymentStatus("SUCCESS");
        when(paymentMapper.selectByPaymentNo(paymentNo)).thenReturn(payment);

        // Mock Order Feign for Landlord ID
        Map<String, Object> orderRes = new HashMap<>();
        orderRes.put("code", 200);
        Map<String, Object> data = new HashMap<>();
        data.put("landlordId", landlordId);
        orderRes.put("data", data);
        when(orderFeignClient.getOrderDetail(orderNo, tenantId)).thenReturn(orderRes);

        // Execute
        paymentService.refundPayment(paymentNo, tenantId);

        // Verify Landlord Deducted (Critical for Financial Consistency)
        verify(accountService).deductBalance(eq(landlordId), eq(amount), eq(paymentNo), contains("订单退款扣除"));

        // Verify Tenant Refunded
        verify(accountService).refundBalance(eq(tenantId), eq(amount), eq(paymentNo), contains("订单退款"));

        // Verify Payment Status Updated
        assertEquals("REFUNDED", payment.getPaymentStatus());
        verify(paymentMapper).updateById(payment);
    }
}
