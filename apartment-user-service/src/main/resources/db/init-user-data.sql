-- ============================================
-- 公寓租赁系统 - 用户服务数据库初始化脚本
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS apartment_db 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE apartment_db;

-- ============================================
-- 创建用户表
-- ============================================
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名，用于登录',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号，用于登录',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `role` VARCHAR(20) NOT NULL DEFAULT 'TENANT' COMMENT '角色：TENANT-租客, LANDLORD-房东, ADMIN-管理员',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 插入测试数据
-- ============================================
-- 说明：所有测试用户的密码都是 "123456"
-- BCrypt 加密后的密码 ($2a$10$...)：123456

INSERT INTO `user` (`username`, `phone`, `password`, `role`, `create_time`, `update_time`) VALUES
-- 租客用户
('tenant1', '13800001001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT', NOW(), NOW()),
('tenant2', '13800001002', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT', NOW(), NOW()),
('zhangsan', '13900001001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT', NOW(), NOW()),

-- 房东用户
('landlord1', '13800002001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD', NOW(), NOW()),
('landlord2', '13800002002', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD', NOW(), NOW()),
('lisi', '13900002001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD', NOW(), NOW()),

-- 管理员用户
('admin', '13800000000', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'ADMIN', NOW(), NOW()),
('superadmin', '13900000000', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'ADMIN', NOW(), NOW());

-- ============================================
-- 验证数据插入
-- ============================================
SELECT 
    user_id,
    username,
    phone,
    role,
    create_time
FROM `user`
ORDER BY role, user_id;

-- ============================================
-- 测试账号信息总结
-- ============================================
-- +------------+----------+---------------+----------+
-- | 用户名     | 手机号   | 密码          | 角色     |
-- +------------+----------+---------------+----------+
-- | tenant1    | 138...01 | 123456        | TENANT   |
-- | tenant2    | 138...02 | 123456        | TENANT   |
-- | zhangsan   | 139...01 | 123456        | TENANT   |
-- | landlord1  | 138...01 | 123456        | LANDLORD |
-- | landlord2  | 138...02 | 123456        | LANDLORD |
-- | lisi       | 139...01 | 123456        | LANDLORD |
-- | admin      | 138...00 | 123456        | ADMIN    |
-- | superadmin | 139...00 | 123456        | ADMIN    |
-- +------------+----------+---------------+----------+
