-- ============ 公寓系统论文级测试数据生成 ============
-- 目标:3万用户 + 8万房源 + 30万订单 + 15万支付
-- 关键:分批 commit(每5000条),避免大事务撑爆内存
USE apartment_db;
SET autocommit=0;
SET unique_checks=0;
SET foreign_key_checks=0;

-- ============ 1. 用户表(30,000)============
DROP PROCEDURE IF EXISTS gen_users;
DELIMITER $$
CREATE PROCEDURE gen_users(IN n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= n DO
    INSERT INTO user(username, phone, password, role, create_time)
    VALUES (
      CONCAT('testuser_', LPAD(i, 7, '0')),
      CONCAT('13', LPAD(i, 9, '0')),
      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
      ELT(1 + FLOOR(RAND()*10), 'TENANT','TENANT','TENANT','TENANT','TENANT','LANDLORD','LANDLORD','LANDLORD','ADMIN','TENANT'),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*730) DAY)
    );
    IF i % 5000 = 0 THEN COMMIT; START TRANSACTION; END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END$$
DELIMITER ;

-- ============ 2. 房源表(80,000)============
DROP PROCEDURE IF EXISTS gen_houses;
DELIMITER $$
CREATE PROCEDURE gen_houses(IN n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE ld INT;
  WHILE i <= n DO
    SET ld = 1 + FLOOR(RAND() * 30000);
    INSERT INTO house(landlord_id, title, description, province, city, district, address, area, room_count, hall_count, bathroom_count, floor, total_floor, orientation, decoration, rent_type, price, payment_method, facilities, status, view_count, create_time)
    VALUES (
      ld,
      CONCAT('精选', ELT(1+FLOOR(RAND()*4),'两居室','三居室','一居室','四居室'), ' ', LPAD(i, 6, '0')),
      CONCAT('交通便利配套齐全拎包入住房源编号', i),
      '北京', '北京市',
      ELT(1+FLOOR(RAND()*8),'朝阳区','海淀区','西城区','东城区','丰台区','石景山区','通州区','昌平区'),
      CONCAT('XX路', FLOOR(RAND()*999), '号'),
      ROUND(30 + RAND()*200, 2),
      1+FLOOR(RAND()*4), 1+FLOOR(RAND()*2), 1+FLOOR(RAND()*2),
      1+FLOOR(RAND()*30), 6+FLOOR(RAND()*30),
      ELT(1+FLOOR(RAND()*5),'南','北','东','西','南北'),
      ELT(1+FLOOR(RAND()*4),'精装','简装','毛坯','豪装'),
      ELT(1+FLOOR(RAND()*2),'WHOLE','SHARED'),
      ROUND(1500 + RAND()*15000, 2),
      ELT(1+FLOOR(RAND()*3),'月付','季付','年付'),
      '空调,洗衣机,冰箱,热水器,宽带',
      ELT(1+FLOOR(RAND()*10),'AVAILABLE','AVAILABLE','AVAILABLE','AVAILABLE','AVAILABLE','AVAILABLE','AVAILABLE','RENTED','RENTED','OFFLINE'),
      FLOOR(RAND()*5000),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*730) DAY)
    );
    IF i % 5000 = 0 THEN COMMIT; START TRANSACTION; END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END$$
DELIMITER ;

-- ============ 3. 订单表(300,000)============
DROP PROCEDURE IF EXISTS gen_orders;
DELIMITER $$
CREATE PROCEDURE gen_orders(IN n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE tid INT; DECLARE hid INT; DECLARE ldid INT;
  DECLARE rent DECIMAL(10,2); DECLARE months INT;
  DECLARE stat VARCHAR(20); DECLARE pstat VARCHAR(20);
  WHILE i <= n DO
    SET tid = 1 + FLOOR(RAND()*30000);
    SET hid = 1 + FLOOR(RAND()*80000);
    SET ldid = 1 + FLOOR(RAND()*30000);
    SET rent = ROUND(1500 + RAND()*15000, 2);
    SET months = 1 + FLOOR(RAND()*24);
    SET stat = ELT(1+FLOOR(RAND()*8),'PENDING_PAYMENT','PENDING_PAYMENT','PAID','PAID','PAID','CONFIRMED','COMPLETED','CANCELLED');
    SET pstat = CASE WHEN stat IN ('PAID','CONFIRMED','COMPLETED') THEN 'PAID' WHEN stat='CANCELLED' THEN 'REFUNDED' ELSE 'UNPAID' END;
    INSERT INTO rental_order(order_no, tenant_id, house_id, landlord_id, rent_start_date, rent_end_date, rent_months, monthly_rent, deposit, total_amount, first_payment_amount, order_status, payment_status, expire_time, create_time, pay_time)
    VALUES (
      CONCAT('ORD', LPAD(i, 9, '0')),
      tid, hid, ldid,
      DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*730) DAY),
      DATE_ADD(CURDATE(), INTERVAL months MONTH),
      months, rent, rent*2, rent*months+rent*2, rent+rent*2,
      stat, pstat,
      DATE_ADD(NOW(), INTERVAL 1 HOUR),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*730) DAY),
      CASE WHEN pstat='PAID' THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*700) DAY) ELSE NULL END
    );
    IF i % 5000 = 0 THEN COMMIT; START TRANSACTION; END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END$$
DELIMITER ;

-- ============ 4. 支付表(150,000,对应已支付订单)============
DROP PROCEDURE IF EXISTS gen_payments;
DELIMITER $$
CREATE PROCEDURE gen_payments(IN n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE uid INT; DECLARE amt DECIMAL(10,2); DECLARE mtd VARCHAR(20); DECLARE pstat VARCHAR(20);
  WHILE i <= n DO
    SET uid = 1 + FLOOR(RAND()*30000);
    SET amt = ROUND(1500 + RAND()*30000, 2);
    SET mtd = ELT(1+FLOOR(RAND()*3),'BALANCE','ALIPAY','WECHAT');
    SET pstat = ELT(1+FLOOR(RAND()*10),'SUCCESS','SUCCESS','SUCCESS','SUCCESS','SUCCESS','SUCCESS','SUCCESS','PENDING','FAILED','REFUNDED');
    INSERT INTO payment(payment_no, order_no, user_id, amount, payment_method, payment_status, refund_amount, success_time, create_time)
    VALUES (
      CONCAT('PAY', LPAD(i, 9, '0')),
      CONCAT('ORD', LPAD(i, 9, '0')),
      uid, amt, mtd, pstat,
      CASE WHEN pstat='REFUNDED' THEN amt ELSE 0 END,
      CASE WHEN pstat='SUCCESS' THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*700) DAY) ELSE NULL END,
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*730) DAY)
    );
    IF i % 5000 = 0 THEN COMMIT; START TRANSACTION; END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END$$
DELIMITER ;

-- ============ 执行生成 ============
SELECT '=== 开始生成 ===' AS status, NOW() AS time;
START TRANSACTION;
CALL gen_users(30000);
SELECT 'users done' AS step, COUNT(*) AS cnt FROM user;
CALL gen_houses(80000);
SELECT 'houses done' AS step, COUNT(*) AS cnt FROM house;
CALL gen_orders(300000);
SELECT 'orders done' AS step, COUNT(*) AS cnt FROM rental_order;
CALL gen_payments(150000);
SELECT 'payments done' AS step, COUNT(*) AS cnt FROM payment;
SELECT '=== 全部完成 ===' AS status, NOW() AS time;

SET unique_checks=1;
SET foreign_key_checks=1;
SET autocommit=1;
