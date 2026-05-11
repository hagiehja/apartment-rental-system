-- ============================================
-- 公寓租赁系统 - 完整测试数据初始化脚本
-- ============================================
-- 说明：本脚本包含所有微服务的测试数据
-- 密码：所有用户密码为 123456
-- ============================================

-- ============================================
-- 1. 用户服务测试数据
-- ============================================
USE apartment_db;

-- 清空现有数据（如果需要）
-- DELETE FROM `user` WHERE user_id > 0;

-- 插入测试用户（已在 init-user-data.sql 中定义，此处确保存在）
INSERT IGNORE INTO `user` (`user_id`, `username`, `phone`, `password`, `role`) VALUES
-- 租客用户
(1, 'tenant1', '13800001001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT'),
(2, 'tenant2', '13800001002', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT'),
(3, 'zhangsan', '13900001001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT'),
-- 房东用户
(4, 'landlord1', '13800002001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD'),
(5, 'landlord2', '13800002002', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD'),
(6, 'lisi', '13900002001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD'),
-- 管理员用户
(7, 'admin', '13800000000', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'ADMIN'),
(8, 'superadmin', '13900000000', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'ADMIN');

-- ============================================
-- 2. 房源服务测试数据
-- ============================================

-- 插入房源
INSERT IGNORE INTO `house` (`house_id`, `landlord_id`, `title`, `description`, `city`, `district`, `address`, `area`, `room_count`, `hall_count`, `bathroom_count`, `floor`, `total_floor`, `orientation`, `decoration`, `rent_type`, `price`, `payment_method`, `facilities`, `status`) VALUES
(1, 4, '朝阳精品两居室 近地铁', '位于市中心，交通便利，配套设施齐全', '北京', '朝阳区', '朝阳路88号', 85.50, 2, 1, 1, 10, 20, '南', '精装', 'WHOLE', 5500.00, '押一付三', '["空调","冰箱","洗衣机","热水器","宽带"]', '1'),
(2, 4, '海淀温馨单间', '干净整洁，家具齐全', '北京', '海淀区', '中关村大街100号', 20.00, 1, 0, 1, 5, 12, '南', '简装', 'SHARED', 2800.00, '押一付一', '["空调","床","衣柜","书桌","宽带"]', '1'),
(3, 5, '浦东豪华江景三居', '高楼层江景房，采光充足', '上海', '浦东新区', '陆家嘴环路200号', 120.00, 3, 2, 2, 25, 30, '东南', '豪华装修', 'WHOLE', 12000.00, '押一付三', '["空调","冰箱","洗衣机","热水器","宽带","电视","沙发"]', '1'),
(4, 5, '静安商务公寓', '靠近写字楼，适合白领', '上海', '静安区', '南京西路500号', 55.00, 1, 1, 1, 15, 25, '西', '精装', 'WHOLE', 6800.00, '押一付三', '["空调","冰箱","洗衣机","宽带","书桌"]', '1'),
(5, 6, '天河精品一居', '地铁上盖，购物便利', '广州', '天河区', '天河路300号', 45.00, 1, 1, 1, 8, 18, '东', '精装', 'WHOLE', 4200.00, '押一付三', '["空调","冰箱","洗衣机","宽带"]', '1');

-- 插入房源图片
INSERT IGNORE INTO `house_image` (`image_id`, `house_id`, `image_url`, `is_cover`, `sort_order`) VALUES
(1, 1, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267', 1, 1),
(2, 1, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688', 0, 2),
(3, 1, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2', 0, 3),
(4, 2, 'https://images.unsplash.com/photo-1540518614846-7eded433c457', 1, 1),
(5, 2, 'https://images.unsplash.com/photo-1616047006789-b7af5afb8c20', 0, 2),
(6, 3, 'https://images.unsplash.com/photo-1556912173-46c336c7fd55', 1, 1),
(7, 3, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb', 0, 2),
(8, 3, 'https://images.unsplash.com/photo-1484154218962-a197022b5858', 0, 3),
(9, 4, 'https://images.unsplash.com/photo-1513584684374-8bab748fbf90', 1, 1),
(10, 4, 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4', 0, 2),
(11, 5, 'https://images.unsplash.com/photo-1502672023488-70e25813eb80', 1, 1),
(12, 5, 'https://images.unsplash.com/photo-1505693314120-0d443867891c', 0, 2);

-- ============================================
-- 3. 支付服务测试数据
-- ============================================

-- 为所有用户创建账户（初始余额10万）
INSERT IGNORE INTO `user_account` (`account_id`, `user_id`, `balance`, `frozen_amount`, `total_income`, `total_expense`) VALUES
(1, 1, 100000.00, 0.00, 0.00, 0.00),
(2, 2, 100000.00, 0.00, 0.00, 0.00),
(3, 3, 100000.00, 0.00, 0.00, 0.00),
(4, 4, 50000.00, 0.00, 0.00, 0.00),
(5, 5, 50000.00, 0.00, 0.00, 0.00),
(6, 6, 50000.00, 0.00, 0.00, 0.00);

-- ============================================
-- 4. 订单服务测试数据（示例已支付订单）
-- ============================================

-- 插入已完成订单示例
INSERT IGNORE INTO `rental_order` (`order_id`, `order_no`, `tenant_id`, `house_id`, `landlord_id`, `rent_start_date`, `rent_end_date`, `rent_months`, `monthly_rent`, `deposit`, `total_amount`, `first_payment_amount`, `installment_enabled`, `order_status`, `payment_status`, `expire_time`, `create_time`, `pay_time`) VALUES
(1, 'ORD202601290001', 1, 1, 4, '2026-02-01', '2026-08-01', 6, 5500.00, 5500.00, 38500.00, 11000.00, 0, 'PAID', 'PAID', DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'ORD202601290002', 2, 3, 5, '2026-02-15', '2027-02-15', 12, 12000.00, 12000.00, 156000.00, 24000.00, 1, 'PAID', 'PARTIAL_PAID', DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 订单2的分期计划
INSERT IGNORE INTO `installment_plan` (`installment_id`, `order_no`, `period_no`, `amount`, `due_date`, `payment_status`, `payment_no`, `payment_time`) VALUES
(1, 'ORD202601290002', 1, 24000.00, '2026-02-01', 'PAID', 'PAY202601280001', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'ORD202601290002', 2, 12000.00, '2026-03-01', 'UNPAID', NULL, NULL),
(3, 'ORD202601290002', 3, 12000.00, '2026-04-01', 'UNPAID', NULL, NULL),
(4, 'ORD202601290002', 4, 12000.00, '2026-05-01', 'UNPAID', NULL, NULL),
(5, 'ORD202601290002', 5, 12000.00, '2026-06-01', 'UNPAID', NULL, NULL),
(6, 'ORD202601290002', 6, 12000.00, '2026-07-01', 'UNPAID', NULL, NULL),
(7, 'ORD202601290002', 7, 12000.00, '2026-08-01', 'UNPAID', NULL, NULL),
(8, 'ORD202601290002', 8, 12000.00, '2026-09-01', 'UNPAID', NULL, NULL),
(9, 'ORD202601290002', 9, 12000.00, '2026-10-01', 'UNPAID', NULL, NULL),
(10, 'ORD202601290002', 10, 12000.00, '2026-11-01', 'UNPAID', NULL, NULL),
(11, 'ORD202601290002', 11, 12000.00, '2026-12-01', 'UNPAID', NULL, NULL),
(12, 'ORD202601290002', 12, 12000.00, '2027-01-01', 'UNPAID', NULL, NULL);

-- 支付单记录
INSERT IGNORE INTO `payment` (`payment_id`, `payment_no`, `order_no`, `installment_id`, `user_id`, `amount`, `payment_method`, `payment_status`, `success_time`, `create_time`) VALUES
(1, 'PAY202601280001', 'ORD202601290001', NULL, 1, 11000.00, 'BALANCE', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'PAY202601280002', 'ORD202601290002', 1, 2, 24000.00, 'BALANCE', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 账户流水
INSERT IGNORE INTO `account_transaction` (`transaction_id`, `transaction_no`, `user_id`, `amount`, `transaction_type`, `balance_before`, `balance_after`, `related_no`, `remark`, `create_time`) VALUES
(1, 'TXN202601280001', 1, -11000.00, 'RENT_PAYMENT', 100000.00, 89000.00, 'PAY202601280001', '订单ORD202601290001首付款', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'TXN202601280002', 2, -24000.00, 'RENT_PAYMENT', 100000.00, 76000.00, 'PAY202601280002', '订单ORD202601290002首付款', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 5. 合同服务测试数据
-- ============================================
USE apartment_contract;

-- 插入合同记录
INSERT IGNORE INTO `contract` (`id`, `contract_no`, `order_id`, `landlord_id`, `tenant_id`, `house_id`, `house_name`, `house_address`, `rental_amount`, `deposit_amount`, `start_date`, `end_date`, `status`, `landlord_signed_at`, `tenant_signed_at`, `created_at`) VALUES
(1, 'CT202601290001', 1, 4, 1, 1, '朝阳精品两居室 近地铁', '北京朝阳区朝阳路88号', 5500.00, 5500.00, '2026-02-01', '2026-08-01', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'CT202601290002', 2, 5, 2, 3, '浦东豪华江景三居', '上海浦东新区陆家嘴环路200号', 12000.00, 12000.00, '2026-02-15', '2027-02-15', 'LANDLORD_SIGNED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 合同签署记录
INSERT IGNORE INTO `contract_signature` (`id`, `contract_id`, `user_id`, `user_type`, `ip_address`, `signed_at`) VALUES
(1, 1, 4, 'LANDLORD', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 1, 'TENANT', '192.168.1.101', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 2, 5, 'LANDLORD', '192.168.1.102', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 6. 通知服务测试数据
-- ============================================
USE apartment_notification;

-- 插入测试消息
INSERT IGNORE INTO `notification_message` (`id`, `user_id`, `type`, `title`, `content`, `biz_id`, `is_read`, `created_at`) VALUES
(1, 1, 'ORDER', '订单支付成功', '订单ORD202601290001已成功支付，房源：【朝阳精品两居室 近地铁】，租期：2026-02-01 至 2026-08-01，租金：¥5500元/月。', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 1, 'CONTRACT', '租赁合同已生成', '订单ORD202601290001的租赁合同已生成（合同编号：CT202601290001），请及时查看并签署。', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 1, 'CONTRACT', '合同签署完成', '合同CT202601290001双方已签署完成，租赁关系正式生效。您可以下载合同查看详情。', 1, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 2, 'ORDER', '订单支付成功', '订单ORD202601290002已成功支付首付款，房源：【浦东豪华江景三居】。', 2, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 2, 'CONTRACT', '房东已签署合同', '合同CT202601290002房东已签署，请您尽快查看并签署，完成租赁流程。', 2, 0, NOW()),
(6, 4, 'ORDER', '您收到了一个新的订单', '您的房源【朝阳精品两居室 近地铁】收到了新订单，订单号：ORD202601290001，租期：2026-02-01 至 2026-08-01，租金：¥5500元/月。请及时处理。', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7, 5, 'ORDER', '您收到了一个新的订单', '您的房源【浦东豪华江景三居】收到了新订单，订单号：ORD202601290002，租期：2026-02-15 至 2027-02-15，租金：¥12000元/月。请及时处理。', 2, 0, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 验证数据
-- ============================================
USE apartment_db;
SELECT '=== 用户数据 ===' AS ''; SELECT user_id, username, role FROM user;
SELECT '=== 房源数据 ===' AS ''; SELECT house_id, title, city, price, status FROM house;
SELECT '=== 账户数据 ===' AS ''; SELECT account_id, user_id, balance FROM user_account;
SELECT '=== 订单数据 ===' AS ''; SELECT order_id, order_no, order_status, payment_status FROM rental_order;
SELECT '=== 支付数据 ===' AS ''; SELECT payment_id, payment_no, amount, payment_status FROM payment;

USE apartment_contract;
SELECT '=== 合同数据 ===' AS ''; SELECT id, contract_no, status FROM contract;

USE apartment_notification;
SELECT '=== 通知数据 ===' AS ''; SELECT id, user_id, type, title, is_read FROM notification_message;

SELECT '============================================' AS '';
SELECT '测试数据初始化完成！' AS '状态';
SELECT '所有用户密码: 123456' AS '提示';
SELECT '============================================' AS '';
