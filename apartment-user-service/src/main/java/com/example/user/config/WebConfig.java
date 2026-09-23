package com.example.user.config;

import com.example.user.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // [VULN-06 修复] CORS 白名单替换通配符, 防止跨域携带凭证攻击
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:5173",      // Vite 开发服务器
                    "http://localhost:8080",      // 本地直连网关
                    "http://127.0.0.1:5173",
                    "http://192.168.24.129",      // 生产前端
                    "http://192.168.24.129:80",
                    "http://192.168.24.129:5173"   // 生产前端 (nginx 直连端口)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/user/health",
                        "/user/stats",
                        "/user/batch",
                        "/user/verify",
                        "/actuator/**"
                );
    }
}



