-- ============================================
-- apartment_db 数据库完整初始化脚本
-- 根据现有Entity类生成，确保表结构与Entity完全匹配
-- ============================================

-- ============================================
-- 清空所有现有表和数据
-- ============================================

-- 禁用外键检查，避免删除表时的外键约束问题
SET FOREIGN_KEY_CHECKS = 0;

-- 删除所有表（按照依赖关系逆序删除）
DROP TABLE IF EXISTS `notification_template`;
DROP TABLE IF EXISTS `notification_message`;
DROP TABLE IF EXISTS `contract`;
DROP TABLE IF EXISTS `account_transaction`;
DROP TABLE IF EXISTS `user_account`;
DROP TABLE IF EXISTS `payment`;
DROP TABLE IF EXISTS `installment_plan`;
DROP TABLE IF EXISTS `rental_order`;
DROP TABLE IF EXISTS `house_image`;
DROP TABLE IF EXISTS `house`;
DROP TABLE IF EXISTS `user`;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 创建数据库表
-- ============================================

-- 1. 用户表 (User Entity)
CREATE TABLE IF NOT EXISTS `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(11) NOT NULL COMMENT '手机号',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：TENANT, LANDLORD, ADMIN',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 房源表 (House Entity)
CREATE TABLE IF NOT EXISTS `house` (
  `house_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '房源ID',
  `landlord_id` BIGINT NOT NULL COMMENT '房东用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '房源标题',
  `description` TEXT COMMENT '房源描述',
  `province` VARCHAR(50) NOT NULL COMMENT '省份',
  `city` VARCHAR(50) NOT NULL COMMENT '城市',
  `district` VARCHAR(50) NOT NULL COMMENT '区/县',
  `address` VARCHAR(200) NOT NULL COMMENT '详细地址',
  `area` DECIMAL(10,2) NOT NULL COMMENT '房屋面积（平方米）',
  `room_count` INT COMMENT '房间数',
  `hall_count` INT COMMENT '厅数',
  `bathroom_count` INT COMMENT '卫生间数',
  `floor` INT COMMENT '楼层',
  `total_floor` INT COMMENT '总楼层',
  `orientation` VARCHAR(20) COMMENT '朝向',
  `decoration` VARCHAR(20) COMMENT '装修情况',
  `rent_type` VARCHAR(20) COMMENT '出租类型（WHOLE-整租/SHARED-合租）',
  `price` DECIMAL(10,2) NOT NULL COMMENT '租金（元/月）',
  `payment_method` VARCHAR(50) COMMENT '付款方式',
  `facilities` VARCHAR(500) COMMENT '配套设施',
  `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态（AVAILABLE/RENTED/OFFLINE）',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`house_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_city_district` (`city`, `district`),
  KEY `idx_status` (`status`),
  KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源表';

-- 3. 房源图片表 (HouseImage Entity)
CREATE TABLE IF NOT EXISTS `house_image` (
  `image_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `is_cover` INT NOT NULL DEFAULT 0 COMMENT '是否封面图（0-否/1-是）',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`image_id`),
  KEY `idx_house_id` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源图片表';

-- 4. 租赁订单表 (RentalOrder Entity)
CREATE TABLE IF NOT EXISTS `rental_order` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
  `tenant_id` BIGINT NOT NULL COMMENT '租客用户ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `landlord_id` BIGINT NOT NULL COMMENT '房东用户ID',
  `rent_start_date` DATE NOT NULL COMMENT '租期开始日期',
  `rent_end_date` DATE NOT NULL COMMENT '租期结束日期',
  `rent_months` INT NOT NULL COMMENT '租赁月数',
  `monthly_rent` DECIMAL(10,2) NOT NULL COMMENT '月租金',
  `deposit` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '押金',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `first_payment_amount` DECIMAL(10,2) NOT NULL COMMENT '首付金额',
  `installment_enabled` INT NOT NULL DEFAULT 0 COMMENT '是否分期（0-否，1-是）',
  `order_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态',
  `cancel_reason` VARCHAR(500) COMMENT '取消原因',
  `remark` VARCHAR(500) COMMENT '备注',
  `expire_time` DATETIME NOT NULL COMMENT '订单过期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `pay_time` DATETIME COMMENT '支付完成时间',
  `cancel_time` DATETIME COMMENT '取消时间',
  `refund_time` DATETIME COMMENT '退款时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_status` (`order_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁订单表';

-- 5. 分期付款计划表 (InstallmentPlan Entity)
CREATE TABLE IF NOT EXISTS `installment_plan` (
  `installment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分期ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
  `period_no` INT NOT NULL COMMENT '期数（1表示首付）',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '应付金额',
  `due_date` DATE NOT NULL COMMENT '应付日期',
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态',
  `payment_no` VARCHAR(32) COMMENT '支付单号',
  `payment_time` DATETIME COMMENT '实际支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`installment_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_status` (`payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分期付款计划表';

-- 6. 支付单表 (Payment Entity)
CREATE TABLE IF NOT EXISTS `payment` (
  `payment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付ID',
  `payment_no` VARCHAR(32) NOT NULL COMMENT '支付单号',
  `order_no` VARCHAR(32) NOT NULL COMMENT '关联订单号',
  `installment_id` BIGINT COMMENT '分期ID',
  `user_id` BIGINT NOT NULL COMMENT '支付用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `payment_method` VARCHAR(20) NOT NULL DEFAULT 'BALANCE' COMMENT '支付方式',
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
  `refund_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '退款金额',
  `refund_time` DATETIME COMMENT '退款时间',
  `success_time` DATETIME COMMENT '支付成功时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_installment_id` (`installment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`payment_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';

-- 7. 用户账户表 (UserAccount Entity)
CREATE TABLE IF NOT EXISTS `user_account` (
  `account_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
  `frozen_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '冻结金额',
  `total_income` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '累计收入',
  `total_expense` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '累计支出',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账户表';

-- 8. 账户流水表 (AccountTransaction Entity)
CREATE TABLE IF NOT EXISTS `account_transaction` (
  `transaction_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `transaction_no` VARCHAR(32) NOT NULL COMMENT '流水号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '交易金额',
  `transaction_type` VARCHAR(20) NOT NULL COMMENT '交易类型',
  `balance_before` DECIMAL(10,2) NOT NULL COMMENT '交易前余额',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '交易后余额',
  `related_no` VARCHAR(32) COMMENT '关联单号',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`transaction_id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_related_no` (`related_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户流水表';

-- 9. 租赁合同表 (Contract Entity)
-- 注意：根据Entity，使用order_id关联订单
CREATE TABLE IF NOT EXISTS `contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_no` VARCHAR(32) NOT NULL COMMENT '合同编号（唯一）',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `landlord_id` BIGINT NOT NULL COMMENT '房东ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租客ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `house_name` VARCHAR(200) COMMENT '房源名称',
  `house_address` VARCHAR(500) COMMENT '房源地址',
  `rental_amount` DECIMAL(10,2) NOT NULL COMMENT '租金金额（元/月）',
  `deposit_amount` DECIMAL(10,2) NOT NULL COMMENT '押金金额',
  `start_date` DATE NOT NULL COMMENT '租赁开始日期',
  `end_date` DATE NOT NULL COMMENT '租赁结束日期',
  `status` VARCHAR(20) NOT NULL COMMENT '状态（PENDING-待签署/LANDLORD_SIGNED-房东已签署/COMPLETED-已完成/ARCHIVED-已归档/CANCELLED-已作废）',
  `file_path` VARCHAR(500) COMMENT '合同PDF文件路径',
  `landlord_signed_at` DATETIME COMMENT '房东签署时间',
  `tenant_signed_at` DATETIME COMMENT '租客签署时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同表';

-- 10. 通知消息表 (NotificationMessage Entity)
CREATE TABLE IF NOT EXISTS `notification_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
  `type` VARCHAR(20) NOT NULL COMMENT '消息类型（ORDER订单/PAYMENT支付/CONTRACT合同）',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `biz_id` BIGINT COMMENT '业务ID（订单ID/支付ID/合同ID）',
  `is_read` INT NOT NULL DEFAULT 0 COMMENT '是否已读（0-未读,1-已读）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_at` DATETIME COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- 11. 通知模板表 (NotificationTemplate Entity)
CREATE TABLE IF NOT EXISTS `notification_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '模板编码（唯一）',
  `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `type` VARCHAR(20) NOT NULL COMMENT '消息类型（ORDER/PAYMENT/CONTRACT）',
  `title_template` VARCHAR(200) NOT NULL COMMENT '标题模板',
  `content_template` TEXT NOT NULL COMMENT '内容模板',
  `enabled` INT NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用,1-启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- ============================================
-- 测试数据插入
-- ============================================

-- 插入用户数据（密码都是 123456 的BCrypt加密结果）
INSERT INTO `user` (`username`, `password`, `phone`, `role`) VALUES
-- 房东用户
('landlord1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EbnKZpXlCHZ8cxUn.aNFSW', '13800000001', 'LANDLORD'),
('landlord2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EbnKZpXlCHZ8cxUn.aNFSW', '13800000002', 'LANDLORD'),
('landlord3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EbnKZpXlCHZ8cxUn.aNFSW', '13800000003', 'LANDLORD'),
-- 租客用户
('tenant1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EbnKZpXlCHZ8cxUn.aNFSW', '13900000001', 'TENANT'),
('tenant2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EbnKZpXlCHZ8cxUn.aNFSW', '13900000002', 'TENANT'),
('tenant3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EbnKZpXlCHZ8cxUn.aNFSW', '13900000003', 'TENANT');

-- 插入房源数据（所有房源都是房东发布的）
INSERT INTO `house` (`landlord_id`, `title`, `description`, `province`, `city`, `district`, `address`, `area`, `room_count`, `hall_count`, `bathroom_count`, `floor`, `total_floor`, `orientation`, `decoration`, `rent_type`, `price`, `payment_method`, `facilities`, `status`, `view_count`) VALUES
-- 房东1发布的房源 (user_id=1)
(1, '精装两室一厅 地铁口 拎包入住', '精装修两室一厅，紧邻地铁站，交通便利，周边配套设施齐全', '广东省', '深圳市', '南山区', '科技园南路88号', 85.00, 2, 1, 1, 15, 30, '南北', 'FINE', 'WHOLE', 5500.00, '押一付三', '空调,冰箱,洗衣机,热水器,宽带,衣柜,床', 'AVAILABLE', 120),
(1, '温馨一居室 适合单身人士', '温馨一居室，采光好，适合单身白领', '广东省', '深圳市', '南山区', '科苑路128号', 45.00, 1, 0, 1, 8, 20, '南', 'SIMPLE', 'WHOLE', 3200.00, '押一付三', '空调,冰箱,热水器,宽带', 'AVAILABLE', 85),
-- 房东2发布的房源 (user_id=2)
(2, '豪华三室两厅 高端小区', '豪华装修三室两厅，高端小区，24小时安保，健身房游泳池', '广东省', '深圳市', '福田区', '深南大道999号', 120.00, 3, 2, 2, 20, 35, '南', 'LUXURY', 'WHOLE', 8800.00, '押一付三', '空调,冰箱,洗衣机,热水器,宽带,衣柜,床,沙发,电视', 'AVAILABLE', 200),
(2, '市中心两室 交通便利', '市中心黄金地段，交通便利，购物方便', '广东省', '深圳市', '福田区', '华强北路66号', 75.00, 2, 1, 1, 12, 25, '东南', 'FINE', 'WHOLE', 4800.00, '押一付三', '空调,冰箱,洗衣机,热水器,宽带,衣柜', 'AVAILABLE', 150),
-- 房东3发布的房源 (user_id=3)
(3, '舒适两室 宜居小区', '舒适两室，小区环境优美，物业管理完善', '广东省', '深圳市', '宝安区', '新安路188号', 80.00, 2, 1, 1, 10, 18, '南北', 'FINE', 'WHOLE', 3800.00, '押一付三', '空调,冰箱,洗衣机,热水器,宽带,衣柜,床', 'AVAILABLE', 95),
(3, '经济实惠一居室 配套齐全', '经济实惠，家电齐全，适合刚毕业的年轻人', '广东省', '深圳市', '宝安区', '宝安大道200号', 50.00, 1, 0, 1, 6, 15, '南', 'SIMPLE', 'WHOLE', 2500.00, '押一付三', '空调,冰箱,热水器,宽带', 'AVAILABLE', 60);

-- 插入房源图片数据
INSERT INTO `house_image` (`house_id`, `image_url`, `is_cover`, `sort_order`) VALUES
(1, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?q=80&w=800&auto=format&fit=crop', 1, 1),
(1, 'https://images.unsplash.com/photo-1615529182904-14819c35db37?q=80&w=800&auto=format&fit=crop', 0, 2),
(1, 'https://images.unsplash.com/photo-1616594039964-40891a911a3d?q=80&w=800&auto=format&fit=crop', 0, 3),
(2, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?q=80&w=800&auto=format&fit=crop', 1, 1),
(3, 'https://images.unsplash.com/photo-1502005229766-071c7a2e98a1?q=80&w=800&auto=format&fit=crop', 1, 1),
(3, 'https://images.unsplash.com/photo-1600585154526-990dced4db0d?q=80&w=800&auto=format&fit=crop', 0, 2),
(4, 'https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?q=80&w=800&auto=format&fit=crop', 1, 1),
(5, 'https://images.unsplash.com/photo-1484154218962-a1c002085d2f?q=80&w=800&auto=format&fit=crop', 1, 1),
(6, 'https://images.unsplash.com/photo-1556020685-ae79c95edfbc?q=80&w=800&auto=format&fit=crop', 1, 1);

-- 插入用户账户数据（租客余额100000，房东余额0）
INSERT INTO `user_account` (`user_id`, `balance`, `frozen_amount`, `total_income`, `total_expense`) VALUES
-- 房东账户（初始余额0）
(1, 0.00, 0.00, 0.00, 0.00),
(2, 0.00, 0.00, 0.00, 0.00),
(3, 0.00, 0.00, 0.00, 0.00),
-- 租客账户（初始余额100000）
(4, 100000.00, 0.00, 0.00, 0.00),
(5, 100000.00, 0.00, 0.00, 0.00),
(6, 100000.00, 0.00, 0.00, 0.00);

-- 插入已完成的订单数据（用于测试合同和通知功能）
INSERT INTO `rental_order` (`order_no`, `tenant_id`, `house_id`, `landlord_id`, `rent_start_date`, `rent_end_date`, `rent_months`, `monthly_rent`, `deposit`, `total_amount`, `first_payment_amount`, `installment_enabled`, `order_status`, `payment_status`, `expire_time`, `pay_time`) VALUES
('ORDER20260130001', 4, 1, 1, '2026-02-01', '2026-07-31', 6, 5500.00, 5500.00, 38500.00, 38500.00, 0, 'PAID', 'PAID', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW());

-- 插入支付记录
INSERT INTO `payment` (`payment_no`, `order_no`, `user_id`, `amount`, `payment_method`, `payment_status`, `success_time`) VALUES
('PAY20260130001', 'ORDER20260130001', 4, 38500.00, 'BALANCE', 'SUCCESS', NOW());

-- 插入账户流水
INSERT INTO `account_transaction` (`transaction_no`, `user_id`, `amount`, `transaction_type`, `balance_before`, `balance_after`, `related_no`, `remark`) VALUES
-- 租客4支出
('TXN20260130001', 4, -38500.00, 'RENT_PAYMENT', 100000.00, 61500.00, 'ORDER20260130001', '支付租金订单 ORDER20260130001'),
-- 房东1收入
('TXN20260130002', 1, 38500.00, 'RENT_INCOME', 0.00, 38500.00, 'ORDER20260130001', '收到租金订单 ORDER20260130001');

-- 更新账户余额
UPDATE `user_account` SET `balance` = 61500.00, `total_expense` = 38500.00 WHERE `user_id` = 4;
UPDATE `user_account` SET `balance` = 38500.00, `total_income` = 38500.00 WHERE `user_id` = 1;

-- 插入租赁合同（支付完成后生成，使用order_id关联）
INSERT INTO `contract` (`contract_no`, `order_id`, `tenant_id`, `landlord_id`, `house_id`, `house_name`, `house_address`, `rental_amount`, `deposit_amount`, `start_date`, `end_date`, `status`, `file_path`, `landlord_signed_at`, `tenant_signed_at`, `created_at`) VALUES
('CONTRACT20260130001', 1, 4, 1, 1, '精装两室一厅 地铁口 拎包入住', '广东省深圳市南山区科技园南路88号', 5500.00, 5500.00, '2026-02-01', '2026-07-31', 'COMPLETED', '/contracts/CONTRACT20260130001.pdf', NOW(), NOW(), NOW());

-- 插入通知数据（支付完成后生成的合同通知）
INSERT INTO `notification_message` (`user_id`, `type`, `title`, `content`, `biz_id`, `is_read`) VALUES
-- 通知租客
(4, 'CONTRACT', '租赁合同已生成', '您的订单 ORDER20260130001 支付成功，租赁合同 CONTRACT20260130001 已自动生成，请查看合同详情。租期：2026-02-01 至 2026-07-31，月租金：5500元。', 1, 0),
-- 通知房东
(1, 'CONTRACT', '收到新租金', '租客 tenant1 已支付订单 ORDER20260130001，金额38500元，租赁合同 CONTRACT20260130001 已生成。租期：2026-02-01 至 2026-07-31。', 1, 0);

-- 插入通知模板数据
INSERT INTO `notification_template` (`code`, `name`, `type`, `title_template`, `content_template`, `enabled`) VALUES
('ORDER_CREATED', '订单创建通知', 'ORDER', '订单创建成功', '您的订单 {orderNo} 已创建成功，请在 {expireTime} 前完成支付。', 1),
('PAYMENT_SUCCESS', '支付成功通知', 'PAYMENT', '支付成功', '您已成功支付订单 {orderNo}，支付金额 {amount} 元。', 1),
('CONTRACT_SIGNED', '合同签署通知', 'CONTRACT', '合同已签署', '合同 {contractNo} 已签署完成，租期从 {startDate} 至 {endDate}。', 1);

-- ============================================
-- 数据验证查询（注释掉，需要时可取消注释）
-- ============================================

-- 查看所有用户
-- SELECT user_id, username, phone, role FROM user;

-- 查看所有房源及其房东信息
-- SELECT h.house_id, h.title, h.price, u.username AS landlord_name, h.status 
-- FROM house h 
-- JOIN user u ON h.landlord_id = u.user_id;

-- 查看所有账户余额
-- SELECT u.username, u.role, a.balance, a.total_income, a.total_expense
-- FROM user u
-- JOIN user_account a ON u.user_id = a.user_id;

-- 查看所有订单
-- SELECT o.order_no, t.username AS tenant, l.username AS landlord, h.title AS house, 
--        o.total_amount, o.order_status, o.payment_status
-- FROM rental_order o
-- JOIN user t ON o.tenant_id = t.user_id
-- JOIN user l ON o.landlord_id = l.user_id
-- JOIN house h ON o.house_id = h.house_id;

-- 查看所有合同（通过order_id关联）
-- SELECT c.contract_no, t.username AS tenant, l.username AS landlord, 
--        c.house_name, c.start_date, c.end_date, c.rental_amount, c.status
-- FROM contract c
-- JOIN user t ON c.tenant_id = t.user_id
-- JOIN user l ON c.landlord_id = l.user_id;

-- 查看所有通知
-- SELECT n.id, u.username AS receiver, n.title, n.type, n.is_read, n.created_at
-- FROM notification_message n
-- JOIN user u ON n.user_id = u.user_id;
