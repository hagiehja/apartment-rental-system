package com.example.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 订单服务启动类
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableFeignClients
@EnableDiscoveryClient // 启用Nacos服务发现
@SpringBootApplication(scanBasePackages = {"com.example.order", "com.example.common"})
@MapperScan("com.example.order.mapper")
@EnableScheduling // 启用定时任务
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
