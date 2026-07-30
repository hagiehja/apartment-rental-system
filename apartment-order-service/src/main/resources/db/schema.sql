USE apartment_db;

CREATE TABLE IF NOT EXISTS `rental_order` (
    `order_id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `house_id` BIGINT NOT NULL,
    `landlord_id` BIGINT NOT NULL,
    `rent_start_date` DATE,
    `rent_end_date` DATE,
    `rent_months` INT,
    `monthly_rent` DECIMAL(12,2),
    `deposit` DECIMAL(12,2),
    `total_amount` DECIMAL(12,2),
    `first_payment_amount` DECIMAL(12,2),
    `installment_enabled` TINYINT DEFAULT 0,
    `order_status` VARCHAR(20),
    `payment_status` VARCHAR(20),
    `cancel_reason` VARCHAR(255),
    `remark` VARCHAR(255),
    `expire_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `pay_time` DATETIME,
    `cancel_time` DATETIME,
    `refund_time` DATETIME,
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_landlord` (`landlord_id`),
    KEY `idx_house` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `installment_plan` (
    `installment_id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) NOT NULL,
    `period_no` INT,
    `amount` DECIMAL(12,2),
    `due_date` DATE,
    `payment_status` VARCHAR(20),
    `payment_no` VARCHAR(64),
    `payment_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`installment_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
