package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API网关启动类
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚪 API网关启动成功! 端口: 8080");
        System.out.println("========================================");
        System.out.println("路由配置:");
        System.out.println("  • /api/user/**       → 用户服务 (8081)");
        System.out.println("  • /api/house/**      → 房源服务 (8083)");
        System.out.println("  • /api/order/**      → 订单服务 (8088)");
        System.out.println("  • /api/payment/**    → 支付服务 (8087)");
        System.out.println("  • /api/account/**    → 支付服务 (8087)");
        System.out.println("  • /api/notification/**→ 通知服务 (8091)");
        System.out.println("  • /api/contract/**   → 合同服务 (8092)");
        System.out.println("========================================");
    }
}
