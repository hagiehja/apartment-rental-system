package com.example.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 支付服务 Feign 客户端
 * 用于订单服务调用支付服务（如退款）
 */
@FeignClient(name = "apartment-payment-service", url = "${feign.url.payment-service:http://apartment-payment-service:8087}")
public interface PaymentFeignClient {

    /**
     * 根据订单号退款（资金原路返还给租客）
     *
     * @param orderNo 订单号
     * @param userId  租客ID（退款操作发起人）
     * @return 操作结果
     */
    @PostMapping("/payment/refund/order/{orderNo}")
    Map<String, Object> refundByOrderNo(@PathVariable("orderNo") String orderNo,
            @RequestHeader("X-User-Id") Long userId);
}
