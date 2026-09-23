package com.example.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 订单服务 Feign 客户端
 * 用于支付服务调用订单服务
 */
@FeignClient(name = "apartment-order-service", url = "${feign.url.order-service:http://apartment-order-service:8088}")
public interface OrderFeignClient {

    /**
     * 验证订单信息
     * 
     * @param orderNo 订单编号
     * @return 订单验证结果
     */
    @GetMapping("/order/{orderNo}/validate")
    Map<String, Object> validateOrder(@PathVariable("orderNo") String orderNo);

    /**
     * 支付成功回调
     * 
     * @param orderNo       订单编号
     * @param installmentId 分期ID（可选）
     */
    @PostMapping("/order/{orderNo}/payment-success")
    void paymentSuccess(@PathVariable("orderNo") String orderNo,
            @RequestParam(required = false) Long installmentId);

    /**
     * 获取订单详情
     *
     * @param orderNo 订单编号
     * @param userId  用户ID
     * @return 订单详情
     */
    @GetMapping("/order/{orderNo}")
    Map<String, Object> getOrderDetail(@PathVariable("orderNo") String orderNo,
            @RequestHeader("X-User-Id") Long userId);
}
