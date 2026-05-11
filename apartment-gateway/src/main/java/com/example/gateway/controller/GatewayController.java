package com.example.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Gateway健康检查和路由信息接口
 */
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @Value("${gateway.info.version:1.0.0}")
    private String version;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "apartment-gateway");
        result.put("status", "UP");
        result.put("port", 8080);
        result.put("version", version);
        return result;
    }

    /**
     * 路由信息
     */
    @GetMapping("/routes")
    public Map<String, Object> routes() {
        Map<String, Object> result = new HashMap<>();
        result.put("gateway", "apartment-gateway");
        result.put("port", 8080);

        Map<String, Map<String, Object>> services = new HashMap<>();

        // 用户服务
        Map<String, Object> userService = new HashMap<>();
        userService.put("port", 8081);
        userService.put("paths", "/api/user/**");
        userService.put("description", "用户认证和管理");
        services.put("user-service", userService);

        // 房源服务
        Map<String, Object> houseService = new HashMap<>();
        houseService.put("port", 8083);
        houseService.put("paths", "/api/house/**");
        houseService.put("description", "房源信息管理");
        services.put("house-service", houseService);

        // 订单服务
        Map<String, Object> orderService = new HashMap<>();
        orderService.put("port", 8088);
        orderService.put("paths", "/api/order/**");
        orderService.put("description", "租赁订单管理");
        services.put("order-service", orderService);

        // 支付服务
        Map<String, Object> paymentService = new HashMap<>();
        paymentService.put("port", 8087);
        paymentService.put("paths", "/api/payment/**, /api/account/**");
        paymentService.put("description", "支付和账户管理");
        services.put("payment-service", paymentService);

        // 通知服务
        Map<String, Object> notificationService = new HashMap<>();
        notificationService.put("port", 8091);
        notificationService.put("paths", "/api/notification/**");
        notificationService.put("description", "站内消息通知");
        services.put("notification-service", notificationService);

        // 合同服务
        Map<String, Object> contractService = new HashMap<>();
        contractService.put("port", 8092);
        contractService.put("paths", "/api/contract/**");
        contractService.put("description", "租赁合同管理");
        services.put("contract-service", contractService);

        result.put("services", services);
        return result;
    }
}
