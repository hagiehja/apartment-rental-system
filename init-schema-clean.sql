-- Create tables without Chinese comments to avoid encoding issues
USE apartment_db;

CREATE TABLE IF NOT EXISTS `user` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'TENANT',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `house` (
    `house_id` BIGINT NOT NULL AUTO_INCREMENT,
    `landlord_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `province` VARCHAR(50),
    `city` VARCHAR(50) NOT NULL,
    `district` VARCHAR(50),
    `address` VARCHAR(200) NOT NULL,
    `area` DECIMAL(10,2),
    `room_count` INT,
    `hall_count` INT,
    `bathroom_count` INT,
    `floor` INT,
    `total_floor` INT,
    `orientation` VARCHAR(20),
    `decoration` VARCHAR(20),
    `rent_type` VARCHAR(20) NOT NULL,
    `price` DECIMAL(10,2) NOT NULL,
    `payment_method` VARCHAR(50),
    `facilities` JSON,
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    `view_count` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`house_id`),
    KEY `idx_landlord` (`landlord_id`),
    KEY `idx_city_status` (`city`, `status`),
    KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `house_image` (
    `image_id` BIGINT NOT NULL AUTO_INCREMENT,
    `house_id` BIGINT NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `is_cover` TINYINT DEFAULT 0,
    `sort_order` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`image_id`),
    KEY `idx_house` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `rental_order` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `house_id` BIGINT NOT NULL,
  `landlord_id` BIGINT NOT NULL,
  `rent_start_date` DATE NOT NULL,
  `rent_end_date` DATE NOT NULL,
  `rent_months` INT NOT NULL,
  `monthly_rent` DECIMAL(10,2) NOT NULL,
  `deposit` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `first_payment_amount` DECIMAL(10,2) NOT NULL,
  `installment_enabled` TINYINT NOT NULL DEFAULT 0,
  `order_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  `cancel_reason` VARCHAR(500),
  `remark` VARCHAR(500),
  `expire_time` DATETIME NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `pay_time` DATETIME,
  `cancel_time` DATETIME,
  `refund_time` DATETIME,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_status` (`order_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `installment_plan` (
  `installment_id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `period_no` INT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `due_date` DATE NOT NULL,
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  `payment_no` VARCHAR(32),
  `payment_time` DATETIME,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`installment_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_status` (`payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_account` (
  `account_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 100000.00,
  `frozen_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_income` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_expense` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `account_transaction` (
  `transaction_id` BIGINT NOT NULL AUTO_INCREMENT,
  `transaction_no` VARCHAR(32) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `transaction_type` VARCHAR(20) NOT NULL,
  `balance_before` DECIMAL(10,2) NOT NULL,
  `balance_after` DECIMAL(10,2) NOT NULL,
  `related_no` VARCHAR(32),
  `remark` VARCHAR(500),
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transaction_id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_related_no` (`related_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payment` (
  `payment_id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(32) NOT NULL,
  `order_no` VARCHAR(32) NOT NULL,
  `installment_id` BIGINT,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `payment_method` VARCHAR(20) NOT NULL DEFAULT 'BALANCE',
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `refund_amount` DECIMAL(10,2) DEFAULT 0,
  `refund_time` DATETIME,
  `success_time` DATETIME,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_installment_id` (`installment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`payment_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
