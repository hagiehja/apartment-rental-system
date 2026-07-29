package com.example.contract.feign;

import com.example.common.api.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "apartment-payment-service", url = "${feign.url.payment-service:http://apartment-payment-service:8087}")
public interface PaymentFeignClient {

    @PostMapping("/payment/refund/order/{orderNo}")
    Result<Void> refundByOrderNo(@PathVariable("orderNo") String orderNo, @RequestHeader("X-User-Id") Long userId);
}
