package com.example.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通知服务启动类
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.example.notification", "com.example.common"})
@MapperScan("com.example.notification.mapper")
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚀 通知服务启动成功! 端口: 8091");
        System.out.println("========================================");
    }
}
