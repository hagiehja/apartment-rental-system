package com.example.house;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 房源服务启动类
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.example.house", "com.example.common"})
@MapperScan("com.example.house.mapper")
public class HouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(HouseApplication.class, args);
    }
}
