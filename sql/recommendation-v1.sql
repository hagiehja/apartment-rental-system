-- Recommendation V1 tables for behavior tracking and user preference profiles.
-- Execute this after the base apartment schema is created.

CREATE TABLE IF NOT EXISTS user_behavior (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  house_id BIGINT NOT NULL,
  behavior_type VARCHAR(32) NOT NULL COMMENT 'VIEW/CLICK/FAVORITE/ORDER/PAY',
  score INT NOT NULL DEFAULT 1,
  source VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_time (user_id, create_time),
  KEY idx_house_type (house_id, behavior_type),
  KEY idx_behavior_type_time (behavior_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-house behavior events for recommendation';

CREATE TABLE IF NOT EXISTS user_preference (
  user_id BIGINT PRIMARY KEY,
  city VARCHAR(64) DEFAULT NULL,
  district VARCHAR(64) DEFAULT NULL,
  min_price DECIMAL(10,2) DEFAULT NULL,
  max_price DECIMAL(10,2) DEFAULT NULL,
  room_count INT DEFAULT NULL,
  min_area DECIMAL(10,2) DEFAULT NULL,
  max_area DECIMAL(10,2) DEFAULT NULL,
  rent_type VARCHAR(32) DEFAULT NULL,
  commute_address VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_city_district (city, district),
  KEY idx_price_room (min_price, max_price, room_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User rental preference profile';
