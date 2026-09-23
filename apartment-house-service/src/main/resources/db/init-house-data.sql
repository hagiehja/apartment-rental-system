USE apartment_db;

DROP TABLE IF EXISTS `house`;

CREATE TABLE `house` (
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
    `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号(MyBatis-Plus @Version)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`house_id`),
    KEY `idx_landlord` (`landlord_id`),
    KEY `idx_city_status` (`city`, `status`),
    KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `house_image`;

CREATE TABLE `house_image` (
    `image_id` BIGINT NOT NULL AUTO_INCREMENT,
    `house_id` BIGINT NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `is_cover` TINYINT DEFAULT 0,
    `sort_order` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`image_id`),
    KEY `idx_house` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `house` (`landlord_id`, `title`, `description`, `city`, `district`, `address`, `area`, `room_count`, `hall_count`, `bathroom_count`, `floor`, `total_floor`, `orientation`, `decoration`, `rent_type`, `price`, `payment_method`, `facilities`, `status`) VALUES
(4, 'Deluxe 2BR Apt Near Subway', 'City center, convenient transportation, all facilities', 'Beijing', 'Chaoyang', 'Chaoyang Road 88', 85.50, 2, 1, 1, 10, 20, 'South', 'Deluxe', 'WHOLE', 5500.00, 'Pay 3 months', '["AC","Fridge","Washer","Heater","Internet"]', 'AVAILABLE'),
(4, 'Cozy Single Room', 'Clean single room, fully furnished', 'Beijing', 'Haidian', 'Zhongguancun Street 100', 20.00, 1, 0, 1, 5, 12, 'South', 'Simple', 'SHARED', 2800.00, 'Pay 1 month', '["AC","Bed","Wardrobe","Desk","Internet"]', 'AVAILABLE'),
(5, 'Luxury 3BR River View', 'High floor river view, bright and spacious', 'Shanghai', 'Pudong', 'Lujiazui Ring Road 200', 120.00, 3, 2, 2, 25, 30, 'Southeast', 'Luxury', 'WHOLE', 12000.00, 'Pay 3 months', '["AC","Fridge","Washer","Heater","Internet","TV","Sofa"]', 'AVAILABLE');

INSERT INTO `house_image` (`house_id`, `image_url`, `is_cover`, `sort_order`) VALUES
(1, 'https://example.com/house1-cover.jpg', 1, 1),
(1, 'https://example.com/house1-room1.jpg', 0, 2),
(1, 'https://example.com/house1-room2.jpg', 0, 3),
(2, 'https://example.com/house2-cover.jpg', 1, 1),
(2, 'https://example.com/house2-room.jpg', 0, 2),
(3, 'https://example.com/house3-cover.jpg', 1, 1),
(3, 'https://example.com/house3-living.jpg', 0, 2),
(3, 'https://example.com/house3-bedroom.jpg', 0, 3);

SELECT house_id, title, city, price, status FROM house;
SELECT COUNT(*) as total_houses FROM house;
