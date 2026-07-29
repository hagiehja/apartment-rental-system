package com.example.contract.feign;

import com.example.common.api.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "apartment-order-service", url = "${feign.url.order-service:http://apartment-order-service:8088}")
public interface OrderFeignClient {

    @GetMapping("/order/{orderNo}")
    Result<Map<String, Object>> getOrderDetail(@PathVariable("orderNo") String orderNo,
            @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/order/id/{orderId}")
    Result<Map<String, Object>> getOrderDetailById(@PathVariable("orderId") Long orderId);

    @PostMapping("/order/{orderNo}/refund-success")
    Result<Void> refundSuccess(@PathVariable("orderNo") String orderNo);
}
