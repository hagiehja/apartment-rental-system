package com.example.payment.config;

import org.springframework.context.annotation.Configuration;

/**
 * 配置类
 * RestTemplate 已移除，使用 OpenFeign 代替
 */
@Configuration
public class AppConfig {
    // 使用 OpenFeign 进行服务间调用，不再需要 RestTemplate
}
