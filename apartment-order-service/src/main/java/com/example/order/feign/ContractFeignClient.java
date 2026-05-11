package com.example.order.feign;

import com.example.order.dto.ContractGenerateDTO;
import com.example.order.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 合同服务 Feign 客户端
 * 用于订单服务调用合同服务
 *
 * 注意：合同服务 Controller 返回 Result<Long>，因此这里必须用 Result 类型接收
 */
@FeignClient(name = "apartment-contract-service", url = "http://localhost:8092")
public interface ContractFeignClient {

    /**
     * 根据订单生成合同
     *
     * @param contract 合同信息
     * @return Result 包装的合同ID
     */
    @PostMapping("/contract/generate")
    Result<Long> generateContract(@RequestBody ContractGenerateDTO contract);

    /**
     * 取消合同（退款时调用）
     *
     * @param orderId 订单ID
     */
    @PostMapping("/contract/cancel/{orderId}")
    Map<String, Object> cancelContract(@PathVariable("orderId") Long orderId);
}
