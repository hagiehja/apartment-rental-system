-- 合同服务数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS apartment_contract DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE apartment_contract;

-- 合同表
DROP TABLE IF EXISTS `contract`;
CREATE TABLE `contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_no` VARCHAR(100) NOT NULL UNIQUE COMMENT '合同编号(唯一)',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `landlord_id` BIGINT NOT NULL COMMENT '房东ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租客ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `house_name` VARCHAR(200) NOT NULL COMMENT '房源名称',
  `house_address` VARCHAR(500) DEFAULT NULL COMMENT '房源地址',
  `rental_amount` DECIMAL(10,2) NOT NULL COMMENT '租金金额(元/月)',
  `deposit_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '押金金额',
  `start_date` DATE NOT NULL COMMENT '租赁开始日期',
  `end_date` DATE NOT NULL COMMENT '租赁结束日期',
  `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING-待签署/LANDLORD_SIGNED-房东已签署/COMPLETED-已完成/ARCHIVED-已归档/CANCELLED-已作废)',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '合同PDF文件路径',
  `landlord_signed_at` DATETIME DEFAULT NULL COMMENT '房东签署时间',
  `tenant_signed_at` DATETIME DEFAULT NULL COMMENT '租客签署时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁合同表';

-- 合同签署记录表
DROP TABLE IF EXISTS `contract_signature`;
CREATE TABLE `contract_signature` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` BIGINT NOT NULL COMMENT '合同ID',
  `user_id` BIGINT NOT NULL COMMENT '签署用户ID',
  `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型(LANDLORD-房东/TENANT-租客)',
  `signature_data` TEXT DEFAULT NULL COMMENT '签名数据(Base64编码,可选)',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '签署IP地址',
  `signed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签署时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同签署记录表';
