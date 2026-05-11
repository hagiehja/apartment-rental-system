package com.example.contract;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 合同服务启动类
 */
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
@MapperScan("com.example.contract.mapper")
public class ContractApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractApplication.class, args);
        System.out.println("========================================");
        System.out.println("📝 合同服务启动成功! 端口: 8092");
        System.out.println("========================================");
    }
}
