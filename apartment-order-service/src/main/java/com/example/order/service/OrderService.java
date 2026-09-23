package com.example.order.service;

import com.example.order.dto.*;
import com.example.order.model.PageResult;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     */
    OrderCreateResultDTO createOrder(OrderCreateDTO createDTO, Long tenantId);

    /**
     * 查询订单详情
     */
    /**
     * 查询订单详情
     */
    OrderDetailDTO getOrderDetail(String orderNo, Long userId);

    /**
     * 根据ID查询订单详情
     */
    OrderDetailDTO getOrderDetailById(Long orderId);

    /**
     * 我的订单列表（分页）
     */
    PageResult<OrderListDTO> getMyOrders(OrderQueryDTO queryDTO, Long userId);

    /**
     * 取消订单
     */
    void cancelOrder(String orderNo, String cancelReason, Long userId);

    /**
     * 更新订单支付状态（内部接口，供支付服务回调）
     */
    void updateOrderPaymentStatus(String orderNo, Long installmentId);

    /**
     * 更新订单退款状态
     */
    void updateOrderRefundStatus(String orderNo);

    /**
     * 验证订单是否可支付
     */
    boolean validateOrderForPayment(String orderNo);

    /**
     * 查询分期计划
     */
    java.util.List<InstallmentDTO> getInstallmentPlan(String orderNo);

    /**
     * 查询房东的租客订单列表（房东视图）
     * 
     * @param queryDTO   查询条件
     * @param landlordId 房东ID
     * @return 订单列表
     */
    PageResult<OrderListDTO> getLandlordOrders(OrderQueryDTO queryDTO, Long landlordId);
}
