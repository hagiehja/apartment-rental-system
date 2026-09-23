-- 通知服务数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS apartment_notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE apartment_notification;

-- 消息表
DROP TABLE IF EXISTS `notification_message`;
CREATE TABLE `notification_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
  `type` VARCHAR(50) NOT NULL COMMENT '消息类型(ORDER订单/PAYMENT支付/CONTRACT合同)',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID(订单ID/支付ID/合同ID)',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读(0-未读,1-已读)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';

-- 消息模板表
DROP TABLE IF EXISTS `notification_template`;
CREATE TABLE `notification_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(100) NOT NULL UNIQUE COMMENT '模板编码(唯一)',
  `name` VARCHAR(200) NOT NULL COMMENT '模板名称',
  `type` VARCHAR(50) NOT NULL COMMENT '消息类型(ORDER/PAYMENT/CONTRACT)',
  `title_template` VARCHAR(200) NOT NULL COMMENT '标题模板',
  `content_template` TEXT NOT NULL COMMENT '内容模板',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用(0-禁用,1-启用)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息模板表';

-- 插入默认消息模板
INSERT INTO `notification_template` (`code`, `name`, `type`, `title_template`, `content_template`) VALUES
-- 订单相关模板
('ORDER_CREATED', '新订单通知', 'ORDER', '您收到了一个新的订单', '您的房源【{{houseName}}】收到了新订单，订单号：{{orderNo}}，租期：{{startDate}} 至 {{endDate}}，租金：¥{{amount}}元/月。请及时处理。'),
('ORDER_PAID', '订单支付成功通知', 'ORDER', '订单支付成功', '订单{{orderNo}}已成功支付，房源：【{{houseName}}】，租期：{{startDate}} 至 {{endDate}}，租金：¥{{amount}}元/月。'),
('ORDER_CANCELLED', '订单取消通知', 'ORDER', '订单已取消', '订单{{orderNo}}已取消，房源：【{{houseName}}】，取消原因：{{reason}}。'),

-- 支付相关模板
('PAYMENT_SUCCESS', '支付成功通知', 'PAYMENT', '支付成功', '您的订单{{orderNo}}支付成功，支付金额：¥{{amount}}元，支付方式：{{paymentMethod}}。'),
('PAYMENT_FAILED', '支付失败通知', 'PAYMENT', '支付失败', '订单{{orderNo}}支付失败，失败原因：{{reason}}。请重新支付或联系客服。'),

-- 合同相关模板
('CONTRACT_CREATED', '合同生成通知', 'CONTRACT', '租赁合同已生成', '订单{{orderNo}}的租赁合同已生成（合同编号：{{contractNo}}），请及时查看并签署。'),
('CONTRACT_LANDLORD_SIGNED', '房东已签署合同', 'CONTRACT', '房东已签署合同', '合同{{contractNo}}房东已签署，请您尽快查看并签署，完成租赁流程。'),
('CONTRACT_TENANT_SIGNED', '租客已签署合同', 'CONTRACT', '租客已签署合同', '合同{{contractNo}}租客已签署，请您查看合同详情。'),
('CONTRACT_COMPLETED', '合同签署完成', 'CONTRACT', '合同签署完成', '合同{{contractNo}}双方已签署完成，租赁关系正式生效。您可以下载合同查看详情。');

-- 插入一些测试数据(可选)
-- INSERT INTO `notification_message` (`user_id`, `type`, `title`, `content`, `biz_id`) VALUES
-- (1, 'ORDER', '测试订单通知', '这是一条测试消息', 1);
