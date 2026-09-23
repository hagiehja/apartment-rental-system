package com.example.order.service.impl;

import com.example.common.enums.HouseStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.order.dto.*;
import com.example.order.entity.InstallmentPlan;
import com.example.order.entity.RentalOrder;
import com.example.order.enums.OrderStatus;
import com.example.order.enums.PaymentStatus;
import com.example.common.exception.BusinessException;
import com.example.order.lock.HouseLock;
import com.example.order.mapper.InstallmentPlanMapper;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.PageResult;
import com.example.order.service.InstallmentService;
import com.example.order.service.OrderService;
import com.example.order.utils.OrderNoGenerator;
import com.example.order.feign.HouseFeignClient;
import com.example.order.feign.ContractFeignClient;
import com.example.order.feign.PaymentFeignClient;
import com.example.common.api.Result;
import com.example.order.mq.OrderNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final InstallmentPlanMapper installmentPlanMapper;
    private final InstallmentService installmentService;
    private final HouseFeignClient houseFeignClient;
    private final ContractFeignClient contractFeignClient;
    private final HouseLock houseLock;
    private final OrderNotificationPublisher orderNotificationPublisher;
    // 调用支付服务处理退款
    private final PaymentFeignClient paymentFeignClient;

    @Override
    public OrderCreateResultDTO createOrder(OrderCreateDTO createDTO, Long tenantId) {
        log.info("创建订单请求，tenantId={}, houseId={}", tenantId, createDTO.getHouseId());

        // 1. 在锁外先获取房源信息，减少持锁时间（优化点）
        Map<String, Object> houseInfo = getHouseInfo(createDTO.getHouseId());
        if (houseInfo == null) {
            throw new BusinessException("房源不存在");
        }

        // 2. 使用分布式锁包裹核心事务逻辑
        try {
            return houseLock.executeWithLockDefault(createDTO.getHouseId(), () -> {
                // 调用内部事务方法，确保事务在锁释放前提交
                return self().doCreateOrder(createDTO, tenantId, houseInfo);
            });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建订单并发操作失败，houseId={}", createDTO.getHouseId(), e);
            throw new BusinessException("创建订单系统繁忙，请重试");
        }
    }

    /**
     * 核心订单创建事务方法
     * 注意：必须使用 public 并通过代理对象调用，以确保 @Transactional 生效
     */
    @Transactional
    public OrderCreateResultDTO doCreateOrder(OrderCreateDTO createDTO, Long tenantId, Map<String, Object> houseInfo) {
        // ===== Double-Check: 获取锁并开启事务后再次检查房源状态 =====

        // 1. 检查该房源是否已有未完成订单 (此时在事务内且持有锁，能看到前一个线程已提交的数据)
        LambdaQueryWrapper<RentalOrder> houseOrderQuery = new LambdaQueryWrapper<>();
        houseOrderQuery.eq(RentalOrder::getHouseId, createDTO.getHouseId())
                .in(RentalOrder::getOrderStatus, OrderStatus.PENDING_PAYMENT.name(), OrderStatus.PAID.name(), OrderStatus.RENTING.name());
        Long houseOrderCount = orderMapper.selectCount(houseOrderQuery);
        if (houseOrderCount > 0) {
            throw new BusinessException("该房源已有未完成订单，请选择其他房源");
        }

        // 2. 再次检查房源状态（防止在获取锁前状态已改变）
        String houseStatus = (String) houseInfo.get("status");
        if (!HouseStatus.AVAILABLE.name().equals(houseStatus)) {
            throw new BusinessException("房源已被租赁或下架，请选择其他房源");
        }

        BigDecimal monthlyRent = new BigDecimal(houseInfo.get("price").toString());
        Long landlordId = Long.parseLong(houseInfo.get("landlordId").toString());

        // 3. 计算订单金额
        BigDecimal deposit = monthlyRent; // 默认押一付一
        BigDecimal totalAmount = monthlyRent.add(deposit);
        BigDecimal firstPaymentAmount = createDTO.getInstallmentEnabled()
                ? monthlyRent.divide(new BigDecimal(createDTO.getRentMonths()), 2, BigDecimal.ROUND_HALF_UP)
                        .add(deposit)
                : totalAmount;

        // 4. 保存订单
        RentalOrder order = new RentalOrder();
        order.setOrderNo(OrderNoGenerator.generateOrderNo());
        order.setTenantId(tenantId);
        order.setHouseId(createDTO.getHouseId());
        order.setLandlordId(landlordId);
        order.setRentStartDate(createDTO.getRentStartDate());
        order.setRentEndDate(createDTO.getRentStartDate().plusMonths(createDTO.getRentMonths()));
        order.setRentMonths(createDTO.getRentMonths());
        order.setMonthlyRent(monthlyRent);
        order.setDeposit(deposit);
        order.setTotalAmount(totalAmount);
        order.setFirstPaymentAmount(firstPaymentAmount);
        order.setInstallmentEnabled(createDTO.getInstallmentEnabled() ? 1 : 0);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setPaymentStatus(PaymentStatus.UNPAID.name());
        order.setRemark(createDTO.getRemark());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));

        orderMapper.insert(order);
        log.info("订单事务创建成功，orderNo={}, houseId={}", order.getOrderNo(), createDTO.getHouseId());

        // 5. 如果启用分期，生成分期计划
        if (createDTO.getInstallmentEnabled()) {
            installmentService.generateInstallmentPlan(
                    order.getOrderNo(),
                    monthlyRent,
                    deposit,
                    createDTO.getRentMonths(),
                    createDTO.getRentStartDate());
        }

        // 6. 构造返回结果
        OrderCreateResultDTO result = new OrderCreateResultDTO();
        result.setOrderNo(order.getOrderNo());
        result.setTotalAmount(totalAmount);
        result.setDeposit(deposit);
        result.setFirstPaymentAmount(firstPaymentAmount);
        result.setInstallmentEnabled(createDTO.getInstallmentEnabled());
        result.setExpireTime(order.getExpireTime());

        return result;
    }

    /**
     * 获取自身代理对象，用于调用内部带有 @Transactional 注解的方法
     */
    private OrderServiceImpl self() {
        return org.springframework.aop.framework.AopContext.currentProxy() != null
                ? (OrderServiceImpl) org.springframework.aop.framework.AopContext.currentProxy()
                : this;
    }

    @Override
    public OrderDetailDTO getOrderDetail(String orderNo, Long userId) {
        RentalOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 验证权限
        if (!order.getTenantId().equals(userId) && !order.getLandlordId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }

        OrderDetailDTO detail = new OrderDetailDTO();
        BeanUtils.copyProperties(order, detail);
        detail.setInstallmentEnabled(order.getInstallmentEnabled() == 1);

        // 获取房源信息
        Map<String, Object> houseInfo = getHouseInfo(order.getHouseId());
        if (houseInfo != null) {
            detail.setHouseTitle((String) houseInfo.get("title"));
            detail.setHouseAddress((String) houseInfo.get("address"));
        }

        // 如果是分期订单，获取分期计划
        if (order.getInstallmentEnabled() == 1) {
            List<InstallmentDTO> installments = installmentService.getInstallmentList(orderNo);
            detail.setInstallments(installments);
        }

        return detail;
    }

    @Override
    public OrderDetailDTO getOrderDetailById(Long orderId) {
        RentalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return getOrderDetail(order.getOrderNo(), order.getTenantId()); // Reuse existing logic
    }

    @Override
    public PageResult<OrderListDTO> getMyOrders(OrderQueryDTO queryDTO, Long userId) {
        Page<RentalOrder> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalOrder::getTenantId, userId);

        if (queryDTO.getOrderStatusList() != null && !queryDTO.getOrderStatusList().isEmpty()) {
            List<String> statusList = queryDTO.getOrderStatusList();
            // 处理前端可能传来的逗号分隔字符串映射到单列表元素的情况
            if (statusList.size() == 1 && statusList.get(0).contains(",")) {
                statusList = java.util.Arrays.asList(statusList.get(0).split(","));
            }
            wrapper.in(RentalOrder::getOrderStatus, statusList);
        } else if (queryDTO.getOrderStatus() != null) {
            wrapper.eq(RentalOrder::getOrderStatus, queryDTO.getOrderStatus());
        }
        if (queryDTO.getPaymentStatus() != null) {
            wrapper.eq(RentalOrder::getPaymentStatus, queryDTO.getPaymentStatus());
        }

        wrapper.orderByDesc(RentalOrder::getCreateTime);

        Page<RentalOrder> orderPage = orderMapper.selectPage(page, wrapper);

        List<OrderListDTO> list = orderPage.getRecords().stream().map(order -> {
            OrderListDTO dto = new OrderListDTO();
            BeanUtils.copyProperties(order, dto);
            dto.setInstallmentEnabled(order.getInstallmentEnabled() == 1);

            // 获取房源信息
            Map<String, Object> houseInfo = getHouseInfo(order.getHouseId());
            if (houseInfo != null) {
                dto.setHouseTitle((String) houseInfo.get("title"));
                dto.setHouseAddress((String) houseInfo.get("address"));
            }

            return dto;
        }).collect(Collectors.toList());

        return new PageResult<>(orderPage.getTotal(), queryDTO.getPageNum(),
                queryDTO.getPageSize(), list);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo, String cancelReason, Long userId) {
        RentalOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 验证权限
        if (!order.getTenantId().equals(userId)) {
            throw new BusinessException("无权取消此订单");
        }

        // 只有待支付状态的订单可以取消
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }

        // 更新订单状态
        order.setOrderStatus(OrderStatus.CANCELLED.name());
        order.setCancelReason(cancelReason);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单取消成功，orderNo={}, reason={}", orderNo, cancelReason);
    }

    @Override
    @Transactional
    public void updateOrderPaymentStatus(String orderNo, Long installmentId) {
        RentalOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 使用分布式锁防止并发问题
        try {
            houseLock.executeWithLockDefault(order.getHouseId(), () -> {
                // Double-Check: 再次检查订单状态
                RentalOrder latestOrder = orderMapper.selectByOrderNo(orderNo);
                if (latestOrder == null) {
                    throw new BusinessException("订单不存在");
                }

                // 更新订单支付状态
                if (installmentId != null) {
                    InstallmentPlan unpaidPlan = installmentService.getNextUnpaidInstallment(orderNo);
                    if (unpaidPlan == null) {
                        latestOrder.setPaymentStatus(PaymentStatus.PAID.name());
                        latestOrder.setOrderStatus(OrderStatus.RENTING.name());
                        latestOrder.setPayTime(LocalDateTime.now());
                    } else {
                        if (unpaidPlan.getPeriodNo() == 1) {
                            latestOrder.setOrderStatus(OrderStatus.PAID.name());
                        }
                        latestOrder.setPaymentStatus(PaymentStatus.PARTIAL_PAID.name());
                    }
                } else {
                    latestOrder.setPaymentStatus(PaymentStatus.PAID.name());
                    latestOrder.setOrderStatus(OrderStatus.RENTING.name());
                    latestOrder.setPayTime(LocalDateTime.now());
                }

                orderMapper.updateById(latestOrder);
                log.info("订单支付状态已更新，orderNo={}, paymentStatus={}",
                        orderNo, latestOrder.getPaymentStatus());

                // 只有首次支付成功时才更新房源状态为已租赁
                if (PaymentStatus.PAID.name().equals(latestOrder.getPaymentStatus()) ||
                        PaymentStatus.PARTIAL_PAID.name().equals(latestOrder.getPaymentStatus())) {

                    // 更新房源状态为已租赁
                    try {
                        houseFeignClient.updateHouseStatus(latestOrder.getHouseId(), HouseStatus.RENTED.name());
                        log.info("房源状态已更新为已租赁: houseId={}", latestOrder.getHouseId());
                    } catch (Exception e) {
                        log.error("更新房源状态失败，将进行重试: houseId={}", latestOrder.getHouseId(), e);
                        // 记录到本地消息表，后续补偿
                    }

                    // 调用合同服务生成合同（带重试机制），返回合同 ID
                    Long contractId = generateContractWithRetry(latestOrder);

                    // 支付成功后的通知统一走 RocketMQ
                    try {
                        // 获取房源名称用于通知模板
                        String houseTitle = "";
                        try {
                            Map<String, Object> hi = getHouseInfo(latestOrder.getHouseId());
                            if (hi != null) {
                                houseTitle = hi.getOrDefault("title", "").toString();
                            }
                        } catch (Exception ignored) {
                        }

                        orderNotificationPublisher.publishPaymentSuccess(latestOrder, houseTitle);

                        // 3. 合同生成成功后，通知租客和房东合同已生成
                        if (contractId != null) {
                            orderNotificationPublisher.publishContractCreated(latestOrder.getTenantId(),
                                    latestOrder.getLandlordId(), houseTitle, contractId);
                        }

                        log.info("支付成功通知已发送到 RocketMQ: orderNo={}", latestOrder.getOrderNo());
                    } catch (Exception e) {
                        log.error("发送支付成功通知失败", e);
                    }

                }
            });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新订单支付状态失败，orderNo={}", orderNo, e);
            throw new BusinessException("支付处理失败，请联系客服");
        }
    }

    /**
     * 生成合同（带重试机制）
     *
     * @return 合同 ID，生成失败则返回 null
     */
    private Long generateContractWithRetry(RentalOrder order) {
        int maxRetries = 3;
        int retryCount = 0;
        Long contractId = null;

        while (retryCount < maxRetries && contractId == null) {
            try {
                ContractGenerateDTO contractDTO = new ContractGenerateDTO();
                contractDTO.setOrderId(order.getOrderId());
                contractDTO.setTenantId(order.getTenantId());
                contractDTO.setLandlordId(order.getLandlordId());
                contractDTO.setHouseId(order.getHouseId());
                contractDTO.setRentalAmount(order.getMonthlyRent());
                contractDTO.setDepositAmount(order.getDeposit());
                contractDTO.setStartDate(order.getRentStartDate());
                contractDTO.setEndDate(order.getRentEndDate());
                contractDTO.setActualCheckInDate(java.time.LocalDate.now());

                // 从房源服务获取房源名称和地址（合同表 house_name 为 NOT NULL）
                try {
                    Map<String, Object> houseInfo = getHouseInfo(order.getHouseId());
                    if (houseInfo != null) {
                        contractDTO.setHouseName(
                                houseInfo.getOrDefault("title", "未知房源").toString());
                        contractDTO.setHouseAddress(
                                houseInfo.getOrDefault("address", "").toString());
                    } else {
                        contractDTO.setHouseName("房源ID:" + order.getHouseId());
                    }
                } catch (Exception ex) {
                    log.warn("获取房源信息失败，使用默认值: houseId={}", order.getHouseId());
                    contractDTO.setHouseName("房源ID:" + order.getHouseId());
                }

                // Feign 返回 Result<Long>，需要从中提取合同 ID
                Result<Long> result = contractFeignClient.generateContract(contractDTO);
                if (result != null && result.getCode() == 200 && result.getData() != null) {
                    contractId = result.getData();
                    log.info("合同生成成功: contractId={}, orderNo={}", contractId, order.getOrderNo());
                } else {
                    String errMsg = result != null ? result.getMessage() : "返回结果为空";
                    log.error("合同服务返回失败: {}, orderNo={}", errMsg, order.getOrderNo());
                    retryCount++;
                    continue;
                }

            } catch (Exception e) {
                retryCount++;
                log.error("合同生成失败，第{}次重试, orderNo={}", retryCount, order.getOrderNo(), e);
                if (retryCount >= maxRetries) {
                    log.error("合同生成最终失败，需要人工处理: orderNo={}", order.getOrderNo());
                } else {
                    try {
                        Thread.sleep(1000L * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return contractId;
    }

    @Override
    @Transactional
    public void updateOrderRefundStatus(String orderNo) {
        RentalOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 1. 调用支付服务退款（若已退款则跳过，实现幂等）
        boolean alreadyRefunded = OrderStatus.REFUNDED.name().equals(order.getOrderStatus());
        if (!alreadyRefunded) {
            try {
                paymentFeignClient.refundByOrderNo(orderNo, order.getTenantId());
                log.info("退款请求已发送: orderNo={}, tenantId={}", orderNo, order.getTenantId());
            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : "";
                // 如果是"已退款"的错误（退款幂等），继续后续流程；否则抛出
                if (errMsg.contains("已退款") || errMsg.contains("REFUNDED") || errMsg.contains("重复")) {
                    log.warn("支付记录已是退款状态，跳过退款步骤继续更新状态: orderNo={}", orderNo);
                } else {
                    log.error("调用支付服务退款失败: orderNo={}", orderNo, e);
                    throw new BusinessException("退款处理失败，请联系客服");
                }
            }
        } else {
            log.info("订单已是退款状态，跳过支付退款步骤: orderNo={}", orderNo);
        }

        // 2. 更新订单状态为已退款
        order.setOrderStatus(OrderStatus.REFUNDED.name());
        order.setPaymentStatus(PaymentStatus.REFUNDED.name());
        order.setRefundTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单状态已更新为已退款: orderNo={}", orderNo);

        // 3. 恢复房源状态为可租赁
        try {
            houseFeignClient.updateHouseStatus(order.getHouseId(), HouseStatus.AVAILABLE.name());
            log.info("房源状态已恢复为可租赁: houseId={}", order.getHouseId());
        } catch (Exception e) {
            log.error("恢复房源状态失败: houseId={}", order.getHouseId(), e);
        }

        // 4. 取消关联合同（若合同已是TERMINATED状态，cancelContract不会重复处理）
        try {
            contractFeignClient.cancelContract(order.getOrderId());
            log.info("合同已取消: orderId={}", order.getOrderId());
        } catch (Exception e) {
            log.error("取消合同失败: orderId={}", order.getOrderId(), e);
        }

        // 5. 发送退租通知（房东 + 租客）
        try {
            // 获取房源标题，用于通知内容
            String houseTitle = "";
            try {
                Map<String, Object> houseInfo = getHouseInfo(order.getHouseId());
                if (houseInfo != null) {
                    houseTitle = houseInfo.getOrDefault("title", "").toString();
                }
            } catch (Exception ignored) {
            }

            orderNotificationPublisher.publishRefundNotifications(order, houseTitle);
            log.info("退租通知已发送到 RocketMQ: orderNo={}", orderNo);

        } catch (Exception e) {
            // 通知失败不影响退款主流程
            log.error("发送退租通知失败: orderNo={}", orderNo, e);
        }
    }

    @Override
    public boolean validateOrderForPayment(String orderNo) {
        RentalOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return false;
        }

        // 检查订单状态
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())
                && !OrderStatus.PAID.name().equals(order.getOrderStatus())) {
            return false;
        }

        // 检查是否过期
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    @Override
    public List<InstallmentDTO> getInstallmentPlan(String orderNo) {
        return installmentService.getInstallmentList(orderNo);
    }

    /**
     * 调用房源服务获取房源信息（使用 Feign）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getHouseInfo(Long houseId) {
        try {
            Map<String, Object> response = houseFeignClient.getHouseDetail(houseId);
            if (response != null && Integer.valueOf(200).equals(response.get("code"))) {
                return (Map<String, Object>) response.get("data");
            }
        } catch (Exception e) {
            log.error("获取房源信息失败，houseId={}", houseId, e);
        }
        return null;
    }

    @Override
    public PageResult<OrderListDTO> getLandlordOrders(OrderQueryDTO queryDTO, Long landlordId) {
        Page<RentalOrder> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalOrder::getLandlordId, landlordId);

        if (queryDTO.getOrderStatusList() != null && !queryDTO.getOrderStatusList().isEmpty()) {
            List<String> statusList = queryDTO.getOrderStatusList();
            // 处理前端可能传来的逗号分隔字符串映射到单列表元素的情况
            if (statusList.size() == 1 && statusList.get(0).contains(",")) {
                statusList = java.util.Arrays.asList(statusList.get(0).split(","));
            }
            wrapper.in(RentalOrder::getOrderStatus, statusList);
        } else if (queryDTO.getOrderStatus() != null) {
            wrapper.eq(RentalOrder::getOrderStatus, queryDTO.getOrderStatus());
        }
        if (queryDTO.getPaymentStatus() != null) {
            wrapper.eq(RentalOrder::getPaymentStatus, queryDTO.getPaymentStatus());
        }

        wrapper.orderByDesc(RentalOrder::getCreateTime);

        Page<RentalOrder> orderPage = orderMapper.selectPage(page, wrapper);

        List<OrderListDTO> list = orderPage.getRecords().stream().map(order -> {
            OrderListDTO dto = new OrderListDTO();
            BeanUtils.copyProperties(order, dto);
            dto.setInstallmentEnabled(order.getInstallmentEnabled() == 1);

            // 获取房源信息
            Map<String, Object> houseInfo = getHouseInfo(order.getHouseId());
            if (houseInfo != null) {
                dto.setHouseTitle((String) houseInfo.get("title"));
                dto.setHouseAddress((String) houseInfo.get("address"));
            }

            return dto;
        }).collect(Collectors.toList());

        return new PageResult<>(orderPage.getTotal(), queryDTO.getPageNum(),
                queryDTO.getPageSize(), list);
    }
}
