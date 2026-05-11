package com.example.user.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具类
 * 用于生成和验证 BCrypt 加密密码
 */
public class PasswordEncoderUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 加密密码 "123456"
        String rawPassword = "123456";
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("========================================");
        System.out.println("BCrypt 密码加密工具");
        System.out.println("========================================");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密后密码: " + encodedPassword);
        System.out.println();

        // 验证密码
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("密码验证结果: " + (matches ? "✅ 匹配成功" : "❌ 匹配失败"));
        System.out.println();

        // 生成 SQL 插入语句
        System.out.println("========================================");
        System.out.println("可用的 SQL 插入语句:");
        System.out.println("========================================");
        System.out.println("INSERT INTO user (username, password, phone, role, create_time, update_time)");
        System.out.println("VALUES ('testuser', '" + encodedPassword + "', '13800138000', 'TENANT', NOW(), NOW());");
        System.out.println();

        // 测试已知的加密密码
        String knownEncoded = "$2a$10$N.zmdr9k7uOEXYqjIZBBHuPRqvHyV2TUBq5oi98b46EKTe3LqJxEO";
        boolean knownMatches = encoder.matches(rawPassword, knownEncoded);
        System.out.println("========================================");
        System.out.println("验证预设密码:");
        System.out.println("========================================");
        System.out.println("预设加密密码: " + knownEncoded);
        System.out.println("验证 '123456': " + (knownMatches ? "✅ 匹配成功" : "❌ 匹配失败"));
    }
}
