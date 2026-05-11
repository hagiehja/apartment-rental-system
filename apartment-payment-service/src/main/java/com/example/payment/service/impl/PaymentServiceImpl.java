package com.example.payment.service.impl;

import com.example.payment.dto.PaymentCreateDTO;
import com.example.payment.dto.PaymentCreateResultDTO;
import com.example.payment.entity.Payment;
import com.example.payment.entity.UserAccount;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.exception.BusinessException;
import com.example.payment.mapper.PaymentMapper;
import com.example.payment.mapper.UserAccountMapper;
import com.example.payment.service.AccountService;
import com.example.payment.service.PaymentService;
import com.example.payment.utils.PaymentNoGenerator;
import com.example.payment.feign.OrderFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;  
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final UserAccountMapper accountMapper;
    private final AccountService accountService;
    private final OrderFeignClient orderFeignClient;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public PaymentCreateResultDTO createPayment(PaymentCreateDTO createDTO, Long userId) {
        log.info("创建支付单，userId={}, orderNo={}", userId, createDTO.getOrderNo());

        // 【分布式锁】防止同一订单重复创建支付单
        String lockKey = "payment:create:lock:" + createDTO.getOrderNo();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待5秒，锁自动释放时间15秒
            boolean acquired = lock.tryLock(5, 15, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("支付单创建中，请勿重复提交");
            }

            // 1. 验证订单是否可支付
            boolean valid = validateOrder(createDTO.getOrderNo());
            if (!valid) {
                throw new BusinessException("订单不可支付");
            }

            // 2. 获取支付金额
            BigDecimal amount = BigDecimal.ZERO;
            Map<String, Object> orderRes = orderFeignClient.getOrderDetail(createDTO.getOrderNo(), userId);
            if (orderRes == null || !Integer.valueOf(200).equals(orderRes.get("code"))) {
                throw new BusinessException("获取订单信息失败");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> orderInfo = (Map<String, Object>) orderRes.get("data");

            if (createDTO.getInstallmentId() != null) {
                // 分期支付：从分期计划中查找金额
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> installments = (List<Map<String, Object>>) orderInfo.get("installments");
                boolean found = false;
                if (installments != null) {
                    for (Map<String, Object> inst : installments) {
                        // 处理数字类型可能的差异
                        Object idObj = inst.get("id");
                        Long instId = idObj instanceof Number ? ((Number) idObj).longValue()
                                : Long.parseLong(idObj.toString());

                        if (instId.equals(createDTO.getInstallmentId())) {
                            amount = new BigDecimal(inst.get("amount").toString());
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    throw new BusinessException("无效的分期ID");
                }
            } else {
                // 全额支付：使用订单总金额
                amount = new BigDecimal(orderInfo.get("totalAmount").toString());
            }

            // 3. 查询用户余额
            UserAccount account = accountMapper.selectByUserId(userId);
            if (account == null) {
                account = ((AccountServiceImpl) accountService).initializeAccount(userId);
            }

            BigDecimal currentBalance = account.getBalance();
            BigDecimal balanceAfter = currentBalance.subtract(amount);

            if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("余额不足");
            }

            // 4. 创建支付单
            Payment payment = new Payment();
            payment.setPaymentNo(PaymentNoGenerator.generatePaymentNo());
            payment.setOrderNo(createDTO.getOrderNo());
            payment.setInstallmentId(createDTO.getInstallmentId());
            payment.setUserId(userId);
            payment.setAmount(amount);
            payment.setPaymentMethod(createDTO.getPaymentMethod());
            payment.setPaymentStatus(PaymentStatus.PENDING.name());
            payment.setRefundAmount(BigDecimal.ZERO);

            paymentMapper.insert(payment);

            // 5. 返回结果
            PaymentCreateResultDTO result = new PaymentCreateResultDTO();
            result.setPaymentNo(payment.getPaymentNo());
            result.setAmount(amount);
            result.setCurrentBalance(currentBalance);
            result.setBalanceAfterPayment(balanceAfter);

            log.info("支付单创建成功，paymentNo={}", payment.getPaymentNo());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取支付创建锁失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void executeBalancePayment(String paymentNo, Long userId) {
        log.info("执行余额支付，paymentNo={}, userId={}", paymentNo, userId);

        // 【分布式锁】防止重复执行支付
        String lockKey = "payment:execute:lock:" + paymentNo;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待5秒，锁自动释放时间30秒
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("支付处理中，请勿重复提交");
            }

            // 1. 查询支付单
            Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
            if (payment == null) {
                throw new BusinessException("支付单不存在");
            }

            if (!payment.getUserId().equals(userId)) {
                throw new BusinessException("无权操作此支付单");
            }

            if (!PaymentStatus.PENDING.name().equals(payment.getPaymentStatus())) {
                throw new BusinessException("支付单状态不正确，当前状态：" + payment.getPaymentStatus());
            }

            try {
                // 2. 扣减用户余额 (租客 -X)
                accountService.deductBalance(
                        userId,
                        payment.getAmount(),
                        payment.getPaymentNo(),
                        "支付订单：" + payment.getOrderNo());

                // 2.1 获取订单信息以获取房东ID
                Long landlordId = null;
                try {
                    Map<String, Object> orderRes = orderFeignClient.getOrderDetail(payment.getOrderNo(), userId);
                    if (orderRes != null && Integer.valueOf(200).equals(orderRes.get("code"))) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> orderInfo = (Map<String, Object>) orderRes.get("data");
                        Object landlordIdObj = orderInfo.get("landlordId");
                        if (landlordIdObj instanceof Number) {
                            landlordId = ((Number) landlordIdObj).longValue();
                        } else {
                            landlordId = Long.parseLong(landlordIdObj.toString());
                        }
                    }
                } catch (Exception e) {
                    log.error("获取房东信息失败，即使扣款成功也无法转账给房东: orderNo={}", payment.getOrderNo(), e);
                    // 严重错误：钱扣了但没给房东。应该抛出异常回滚，或者进入人工处理队列。
                    // 这里为了资金安全，选择抛出异常回滚
                    throw new BusinessException("系统异常：无法获取房东信息，支付已取消");
                }

                if (landlordId != null) {
                    // 2.2 增加房东余额 (房东 +X)
                    accountService.receiveIncome(
                            landlordId,
                            payment.getAmount(),
                            payment.getPaymentNo(),
                            "收到租金：" + payment.getOrderNo());
                } else {
                    throw new BusinessException("系统异常：房东ID为空");
                }

                // 3. 更新支付单状态
                payment.setPaymentStatus(PaymentStatus.SUCCESS.name());
                payment.setSuccessTime(LocalDateTime.now());
                paymentMapper.updateById(payment);

                // 4. 回调订单服务
                notifyOrderPaymentSuccess(payment.getOrderNo(), payment.getInstallmentId());

                log.info("支付成功，已转账给房东，paymentNo={}, landlordId={}", paymentNo, landlordId);

            } catch (Exception e) {
                // 支付失败
                // 如果是 BusinessException (如余额不足), catch住并回滚
                // 注意：由于本方法有 @Transactional，抛出异常会自动回滚数据库操作

                // 但我们需要更新支付单状态为 FAILED 吗？
                // 如果回滚了，支付单状态更新也会回滚。
                // 通常我们希望支付单保持 PENDING 或者标记为 FAILED。
                // 如果要标记 FAILED，需要在一个新的事务中执行，或者 catch 后不再抛出异常(但这样 deductBalance 可能会提交IfNeeded)。
                // 这是一个经典的分布式/本地事务问题。

                // 简单处理：抛出异常让整个事务回滚 (Account balance changes rolled back)
                // 支付单状态仍为 PENDING (因为 updateById(SUCCESS) 回滚了)
                // 用户可以重试。
                log.error("支付逻辑异常，事务回滚", e);
                throw e; // 重新抛出以触发回滚
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取支付执行锁失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void refundPayment(String paymentNo, Long userId) {
        log.info("申请退款，paymentNo={}, userId={}", paymentNo, userId);

        // 1. 查询支付单
        Payment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (payment == null) {
            throw new BusinessException("支付单不存在");
        }

        if (!PaymentStatus.SUCCESS.name().equals(payment.getPaymentStatus())) {
            throw new BusinessException("支付单状态不支持退款");
        }

        // 1.1 获取房东ID
        Long landlordId = null;
        try {
            Map<String, Object> orderRes = orderFeignClient.getOrderDetail(payment.getOrderNo(), userId);
            if (orderRes != null && Integer.valueOf(200).equals(orderRes.get("code"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> orderInfo = (Map<String, Object>) orderRes.get("data");
                Object landlordIdObj = orderInfo.get("landlordId");
                if (landlordIdObj instanceof Number) {
                    landlordId = ((Number) landlordIdObj).longValue();
                } else {
                    landlordId = Long.parseLong(landlordIdObj.toString());
                }
            }
        } catch (Exception e) {
            log.error("获取订单信息失败，无法处理退款: orderNo={}", payment.getOrderNo(), e);
            throw new BusinessException("退款失败：无法获取订单信息");
        }

        if (landlordId == null) {
            throw new BusinessException("退款失败：房东信息缺失");
        }

        // 2. 执行退款逻辑 (原子性：房东扣款，租客退款)

        // 2.1 扣除房东余额 (房东 -X)
        // 注意：如果房东余额不足，deductBalance 会抛出异常，整个事务回滚，退款失败
        try {
            accountService.deductBalance(
                    landlordId,
                    payment.getAmount(),
                    payment.getPaymentNo(),
                    "订单退款扣除：" + payment.getOrderNo());
        } catch (BusinessException e) {
            throw new BusinessException("退款失败：房东余额不足以支付退款");
        }

        // 2.2 退款到用户余额 (租客 +X)
        accountService.refundBalance(
                payment.getUserId(),
                payment.getAmount(),
                payment.getPaymentNo(),
                "订单退款");

        // 3. 更新支付单状态
        payment.setPaymentStatus(PaymentStatus.REFUNDED.name());
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        log.info("退款成功，从房东扣款并退还租客，paymentNo={}, amount={}", paymentNo, payment.getAmount());
    }

    @Override
    @Transactional
    public void refundPaymentByOrderNo(String orderNo, Long userId) {
        log.info("根据订单号申请退款，orderNo={}, userId={}", orderNo, userId);

        // 1. 根据订单号查询支付成功的支付单
        List<Payment> payments = paymentMapper.selectByOrderNo(orderNo);
        Payment targetPayment = null;

        for (Payment p : payments) {
            if (PaymentStatus.SUCCESS.name().equals(p.getPaymentStatus())) {
                targetPayment = p;
                break;
            }
        }

        if (targetPayment == null) {
            log.warn("未找到可退款的支付单，orderNo={}", orderNo);
            // 可能该订单金额为0（如全额抵扣），或者尚未支付
            // 在这种情况下，我们不需要抛出异常阻止退租流程，只是记录日志
            throw new BusinessException("未找到可退款的支付单，退款失败");
        }

        // 2. 执行退款
        refundPayment(targetPayment.getPaymentNo(), userId);
    }

    /**
     * 验证订单是否可支付（使用 Feign）
     */
    @SuppressWarnings("unchecked")
    private boolean validateOrder(String orderNo) {
        try {
            Map<String, Object> response = orderFeignClient.validateOrder(orderNo);
            if (response != null && Integer.valueOf(200).equals(response.get("code"))) {
                return (Boolean) response.get("data");
            }
        } catch (Exception e) {
            log.error("验证订单失败，orderNo={}", orderNo, e);
        }
        return false;
    }

    /**
     * 通知订单服务支付成功（使用 Feign）
     */
    private void notifyOrderPaymentSuccess(String orderNo, Long installmentId) {
        try {
            orderFeignClient.paymentSuccess(orderNo, installmentId);
            log.info("通知订单服务支付成功，orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("通知订单服务失败，orderNo={}", orderNo, e);
            // 这里不抛异常，避免影响支付流程
        }
    }
}
