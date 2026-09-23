-- 创建房东收入表
-- 执行时间：2026-02-05

USE apartment_db;

CREATE TABLE IF NOT EXISTS landlord_income (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    landlord_id BIGINT NOT NULL COMMENT '房东ID',
    contract_id BIGINT COMMENT '合同ID',
    order_id BIGINT COMMENT '订单ID',
    income_type VARCHAR(20) NOT NULL COMMENT '收入类型：RENTAL-租金收入/REFUND-退款扣减',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    total_payment DECIMAL(10,2) COMMENT '租客支付总额',
    deposit_amount DECIMAL(10,2) COMMENT '押金金额',
    commission_rate DECIMAL(5,2) DEFAULT 0.80 COMMENT '分成比例',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_landlord (landlord_id),
    INDEX idx_contract (contract_id)
) COMMENT '房东收入记录表';
