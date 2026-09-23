USE apartment_db;

CREATE TABLE IF NOT EXISTS `payment` (
    `payment_id` BIGINT NOT NULL AUTO_INCREMENT,
    `payment_no` VARCHAR(64) NOT NULL,
    `order_no` VARCHAR(64),
    `installment_id` BIGINT,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(12,2),
    `payment_method` VARCHAR(20),
    `payment_status` VARCHAR(20),
    `refund_amount` DECIMAL(12,2),
    `refund_time` DATETIME,
    `success_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`payment_id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_account` (
    `account_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `balance` DECIMAL(12,2) DEFAULT 0,
    `frozen_amount` DECIMAL(12,2) DEFAULT 0,
    `total_income` DECIMAL(12,2) DEFAULT 0,
    `total_expense` DECIMAL(12,2) DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`account_id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `account_transaction` (
    `transaction_id` BIGINT NOT NULL AUTO_INCREMENT,
    `transaction_no` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(12,2),
    `transaction_type` VARCHAR(20),
    `balance_before` DECIMAL(12,2),
    `balance_after` DECIMAL(12,2),
    `related_no` VARCHAR(64),
    `remark` VARCHAR(255),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`transaction_id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 给所有现有用户初始化演示账户(余额10万,足够支付测试)
INSERT IGNORE INTO user_account (user_id, balance, total_income)
SELECT user_id, 100000, 100000 FROM user;
