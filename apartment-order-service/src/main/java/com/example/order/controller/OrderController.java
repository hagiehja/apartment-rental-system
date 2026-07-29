package com.example.order.controller;

import com.example.order.dto.*;
import com.example.order.model.PageResult;
import com.example.common.api.Result;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public Result<OrderCreateResultDTO> createOrder(
            @Valid @RequestBody OrderCreateDTO createDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        OrderCreateResultDTO result = orderService.createOrder(createDTO, userId);
        return Result.success(result);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderNo}")
    public Result<OrderDetailDTO> getOrderDetail(
            @PathVariable String orderNo,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        OrderDetailDTO detail = orderService.getOrderDetail(orderNo, userId);
        return Result.success(detail);
    }

    /**
     * 根据ID查询订单详情（内部接口）
     */
    @GetMapping("/id/{orderId}")
    public Result<OrderDetailDTO> getOrderDetailById(@PathVariable Long orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailById(orderId);
        return Result.success(detail);
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/my/list")
    public Result<PageResult<OrderListDTO>> getMyOrders(
            OrderQueryDTO queryDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        PageResult<OrderListDTO> result = orderService.getMyOrders(queryDTO, userId);
        return Result.success(result);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error(401, "未登录");
        }

        String cancelReason = body != null ? body.get("cancelReason") : "用户取消";
        orderService.cancelOrder(orderNo, cancelReason, userId);
        return Result.success(null);
    }

    /**
     * 查询分期计划
     */
    @GetMapping("/{orderNo}/installments")
    public Result<List<InstallmentDTO>> getInstallmentPlan(@PathVariable String orderNo) {
        List<InstallmentDTO> installments = orderService.getInstallmentPlan(orderNo);
        return Result.success(installments);
    }

    /**
     * 验证订单是否可支付（内部接口）
     */
    @GetMapping("/{orderNo}/validate")
    public Result<Boolean> validateOrder(@PathVariable String orderNo) {
        boolean valid = orderService.validateOrderForPayment(orderNo);
        return Result.success(valid);
    }

    /**
     * 支付成功回调（内部接口）
     */
    @PostMapping("/{orderNo}/payment-success")
    public Result<Void> paymentSuccess(
            @PathVariable String orderNo,
            @RequestParam(required = false) Long installmentId) {

        orderService.updateOrderPaymentStatus(orderNo, installmentId);
        return Result.success(null);
    }

    /**
     * 退款成功更新状态（内部接口）
     */
    @PostMapping("/{orderNo}/refund-success")
    public Result<Void> refundSuccess(@PathVariable String orderNo) {
        try {
            orderService.updateOrderRefundStatus(orderNo);
            return Result.success(null);
        } catch (Exception e) {
            log.error("退款回调异常 orderNo={}", orderNo, e);
            return Result.error(500, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Order Service is running!");
    }

    /**
     * 获取房东的租客订单列表
     */
    @GetMapping("/landlord/orders")
    public Result<PageResult<OrderListDTO>> getLandlordOrders(
            OrderQueryDTO queryDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long landlordId) {
        if (landlordId == null) {
            return Result.error(401, "未登录");
        }
        PageResult<OrderListDTO> result = orderService.getLandlordOrders(queryDTO, landlordId);
        return Result.success(result);
    }
}
