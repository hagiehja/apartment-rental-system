package com.example.user.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 验证数据库中的密码
 */
public class VerifyDatabasePassword {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 数据库中 tenant1 的密码
        String dbPassword = "$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa";

        // 测试各种可能的明文密码
        String[] testPasswords = { "123456", "tenant1", "password", "admin", "123" };

        System.out.println("========================================");
        System.out.println("验证数据库密码");
        System.out.println("========================================");
        System.out.println("数据库密码: " + dbPassword);
        System.out.println();

        for (String password : testPasswords) {
            boolean matches = encoder.matches(password, dbPassword);
            System.out.println("测试密码 '" + password + "': " + (matches ? "✅ 匹配" : "❌ 不匹配"));
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("如果都不匹配，请使用以下密码更新数据库：");
        System.out.println("========================================");

        String correctPassword = encoder.encode("123456");
        System.out.println("UPDATE user SET password = '" + correctPassword + "' WHERE username = 'tenant1';");
    }
}
