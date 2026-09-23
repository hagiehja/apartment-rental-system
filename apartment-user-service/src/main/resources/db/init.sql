CREATE DATABASE IF NOT EXISTS apartment_db 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE apartment_db;

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
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

INSERT INTO `user` (`username`, `phone`, `password`, `role`, `create_time`, `update_time`) VALUES
('tenant1', '13800001001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT', NOW(), NOW()),
('tenant2', '13800001002', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT', NOW(), NOW()),
('zhangsan', '13900001001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'TENANT', NOW(), NOW()),
('landlord1', '13800002001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD', NOW(), NOW()),
('landlord2', '13800002002', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD', NOW(), NOW()),
('lisi', '13900002001', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'LANDLORD', NOW(), NOW()),
('admin', '13800000000', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'ADMIN', NOW(), NOW()),
('superadmin', '13900000000', '$2a$10$lwzAShIeKxNfg2yMLaWQgOQ6oTXiGG5vRiEL7ra3Ebp7jN9yhJRFa', 'ADMIN', NOW(), NOW());

SELECT COUNT(*) as total_users FROM `user`;
