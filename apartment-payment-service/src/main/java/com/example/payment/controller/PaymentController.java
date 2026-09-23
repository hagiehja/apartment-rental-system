package com.example.payment.controller;

import com.example.payment.dto.AccountBalanceDTO;
import com.example.payment.dto.PaymentCreateDTO;
import com.example.payment.dto.PaymentCreateResultDTO;
import com.example.common.api.Result;
import com.example.payment.service.AccountService;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 */
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final AccountService accountService;

    /**
     * 查询账户余额
     */
    @GetMapping("/account/balance")
    public Result<AccountBalanceDTO> getBalance(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        AccountBalanceDTO balance = accountService.getBalance(userId);
        return Result.success(balance);
    }

    /**
     * 查询交易记录
     */
    @GetMapping("/account/transactions")
    public Result<java.util.List<com.example.payment.entity.AccountTransaction>> getTransactions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        java.util.List<com.example.payment.entity.AccountTransaction> list = accountService.getTransactionList(userId);
        return Result.success(list);
    }

    /**
     * 模拟充值（用于演示）
     */
    @PostMapping("/account/recharge")
    public Result<Void> mockRecharge(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody RechargeDTO rechargeDTO) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        accountService.receiveIncome(userId, rechargeDTO.getAmount(), "MOCK_RECHARGE", "模拟充值");
        return Result.success(null);
    }

    /**
     * 充值DTO
     */
    @lombok.Data
    public static class RechargeDTO {
        private java.math.BigDecimal amount;
    }

    /**
     * 创建支付单
     */
    @PostMapping
    public Result<PaymentCreateResultDTO> createPayment(
            @Valid @RequestBody PaymentCreateDTO createDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        PaymentCreateResultDTO result = paymentService.createPayment(createDTO, userId);
        return Result.success(result);
    }

    /**
     * 执行支付
     */
    @PostMapping("/{paymentNo}/pay")
    public Result<Void> executePayment(
            @PathVariable String paymentNo,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        paymentService.executeBalancePayment(paymentNo, userId);
        return Result.success(null);
    }

    /**
     * 申请退款
     */
    @PostMapping("/{paymentNo}/refund")
    public Result<Void> refundPayment(
            @PathVariable String paymentNo,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        paymentService.refundPayment(paymentNo, userId);
        return Result.success(null);
    }

    /**
     * 根据订单号退款
     */
    @PostMapping("/refund/order/{orderNo}")
    public Result<Void> refundByOrderNo(
            @PathVariable String orderNo,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        try {
            paymentService.refundPaymentByOrderNo(orderNo, userId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("退款失败 orderNo={} userId={}", orderNo, userId, e);
            return Result.error(500, e.getMessage() != null ? e.getMessage() : "Unknown Payment Error");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Payment Service is running!");
    }
}
