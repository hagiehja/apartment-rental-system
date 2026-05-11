-- 迁移脚本：添加退租功能相关字段到contract表
-- 执行时间：2026-02-05

USE apartment_db;

-- 1. 添加退租相关字段到contract表
ALTER TABLE contract 
ADD COLUMN actual_check_in_date DATE COMMENT '实际入住日期' AFTER end_date,
ADD COLUMN actual_check_out_date DATE COMMENT '实际退租日期' AFTER actual_check_in_date,
ADD COLUMN termination_reason VARCHAR(500) COMMENT '退租原因' AFTER actual_check_out_date,
ADD COLUMN refund_amount DECIMAL(10,2) COMMENT '退款金额' AFTER termination_reason,
ADD COLUMN landlord_income DECIMAL(10,2) COMMENT '房东收入金额' AFTER refund_amount;

-- 验证字段添加
DESC contract;
