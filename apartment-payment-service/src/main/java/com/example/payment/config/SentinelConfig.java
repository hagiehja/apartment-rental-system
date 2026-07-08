package com.example.payment.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [L0 容灾] Sentinel 全局熔断/限流降级处理器
 * - 限流 (FlowException)        -> 429 友好提示
 * - 熔断降级 (DegradeException)  -> 503 降级提示
 * - 系统保护 (SystemBlockException) -> 503
 * 返回统一 JSON,前端可按 code 做对应交互。
 */
@Configuration
public class SentinelConfig {

    @Bean
    public BlockExceptionHandler blockExceptionHandler() {
        return (HttpServletRequest request, HttpServletResponse response, BlockException e) -> {
            int code;
            String msg;
            if (e instanceof FlowException) {
                code = 429;
                msg = "请求过于频繁,请稍后再试";
            } else if (e instanceof DegradeException) {
                code = 503;
                msg = "服务降级保护中,请稍后重试";
            } else if (e instanceof AuthorityException) {
                code = 403;
                msg = "无权限访问";
            } else if (e instanceof SystemBlockException) {
                code = 503;
                msg = "系统过载自保中,请稍后重试";
            } else {
                code = 503;
                msg = "请求被拦截:" + e.getClass().getSimpleName();
            }
            response.setStatus(200);  // 业务码放 body,HTTP 200 便于前端统一处理
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", code, msg));
        };
    }
}
