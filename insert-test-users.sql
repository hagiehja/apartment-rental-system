-- ========================================
-- 公寓租赁系统 - 测试用户数据
-- ========================================
-- 使用数据库
USE apartment_db;

-- 删除可能存在的测试用户（可选）
DELETE FROM user WHERE username IN ('testuser', 'landlord', 'admin');

-- 插入租客测试用户
-- 用户名: testuser
-- 密码: 123456 (BCrypt加密)
-- 手机: 13800138000
INSERT INTO user (username, password, phone, role, create_time, update_time)
VALUES ('testuser', '$2a$10$N.zmdr9k7uOEXYqjIZBBHuPRqvHyV2TUBq5oi98b46EKTe3LqJxEO', '13800138000', 'TENANT', NOW(), NOW());

-- 插入房东测试用户
-- 用户名: landlord
-- 密码: 123456 (BCrypt加密)
-- 手机: 13900139000
INSERT INTO user (username, password, phone, role, create_time, update_time)
VALUES ('landlord', '$2a$10$N.zmdr9k7uOEXYqjIZBBHuPRqvHyV2TUBq5oi98b46EKTe3LqJxEO', '13900139000', 'LANDLORD', NOW(), NOW());

-- 插入管理员测试用户
-- 用户名: admin
-- 密码: 123456 (BCrypt加密)
-- 手机: 13700137000
INSERT INTO user (username, password, phone, role, create_time, update_time)
VALUES ('admin', '$2a$10$N.zmdr9k7uOEXYqjIZBBHuPRqvHyV2TUBq5oi98b46EKTe3LqJxEO', '13700137000', 'ADMIN', NOW(), NOW());

-- 查询验证
SELECT user_id, username, phone, role, create_time FROM user;

-- ========================================
-- 测试登录
-- ========================================
-- 登录接口: POST http://localhost:8080/api/user/login
-- 请求体:
-- {
--   "account": "testuser",    // 或使用手机号 "13800138000"
--   "password": "123456"
-- }
