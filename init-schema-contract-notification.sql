-- Contract and Notification schemas
USE apartment_contract;

CREATE TABLE IF NOT EXISTS `contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contract_no` VARCHAR(100) NOT NULL UNIQUE,
  `order_id` BIGINT NOT NULL,
  `landlord_id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `house_id` BIGINT NOT NULL,
  `house_name` VARCHAR(200) NOT NULL,
  `house_address` VARCHAR(500) DEFAULT NULL,
  `rental_amount` DECIMAL(10,2) NOT NULL,
  `deposit_amount` DECIMAL(10,2) DEFAULT 0,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  `file_path` VARCHAR(500) DEFAULT NULL,
  `landlord_signed_at` DATETIME DEFAULT NULL,
  `tenant_signed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `contract_signature` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `user_type` VARCHAR(20) NOT NULL,
  `signature_data` TEXT DEFAULT NULL,
  `ip_address` VARCHAR(50) DEFAULT NULL,
  `signed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

USE apartment_notification;

CREATE TABLE IF NOT EXISTS `notification_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `biz_id` BIGINT DEFAULT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(100) NOT NULL UNIQUE,
  `name` VARCHAR(200) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `title_template` VARCHAR(200) NOT NULL,
  `content_template` TEXT NOT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
