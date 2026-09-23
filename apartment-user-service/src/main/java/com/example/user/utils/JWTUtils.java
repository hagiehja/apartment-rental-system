package com.example.user.utils;

import com.example.user.dto.UserInfoDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Component
public class JWTUtils {

    private static final Logger log = LoggerFactory.getLogger(JWTUtils.class);
    private static final long EXPIRATION_TIME = 86400000; // 24小时

    /**
     * [VULN-09 修复] 从外部配置注入密钥, 不再硬编码
     * 优先级: env JWT_SECRET > application.yml 的 jwt.secret
     * 支持明文与 Base64 两种格式(Base64 兼容历史 .env.app)
     */
    @Value("${jwt.secret:}")
    private String secretRaw;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secretRaw == null || secretRaw.isBlank()) {
            throw new IllegalStateException(
                "[VULN-09] 未配置 JWT 密钥. 请通过环境变量 JWT_SECRET 或 application.yml 的 jwt.secret 注入. " +
                "生成方法: openssl rand -base64 48");
        }
        String secret;
        try {
            // 兼容 Base64 编码的密钥
            byte[] decoded = Base64.getDecoder().decode(secretRaw.trim());
            secret = new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // 不是 Base64, 当作明文用
            secret = secretRaw.trim();
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "[VULN-09] JWT 密钥长度不足 32 字符 (当前 " + secret.length() + "). " +
                "HS384 要求至少 48 字节, 请使用 openssl rand -base64 48 重新生成");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("[VULN-09] JWT 密钥已从外部配置加载 (长度 {}, HS384)", secret.length());
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(UserInfoDTO userInfo) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(userInfo.getUserId().toString())
                .claim("username", userInfo.getUsername())
                .claim("phone", userInfo.getPhone())
                .claim("role", userInfo.getRole())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
}
