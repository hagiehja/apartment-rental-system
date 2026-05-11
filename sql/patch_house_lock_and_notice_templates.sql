-- ============================================================
-- 数据库补丁：防超卖乐观锁字段 + 支付与合同通知模板
-- house 表在 apartment_db，notification_template 在 apartment_notification
-- ============================================================

-- 1. house 表新增乐观锁 version 列（若已存在会报错，忽略即可）
ALTER TABLE apartment_db.house
    ADD COLUMN `version` INT NOT NULL DEFAULT 0
    COMMENT '乐观锁版本号，用于防止并发超卖';

-- 2. 新增租客支付成功通知模板
INSERT INTO apartment_notification.notification_template
    (code, name, type, title_template, content_template, enabled)
VALUES
('TENANT_PAY_SUCCESS', '租客支付成功及租赁确认', 'PAYMENT',
 '支付成功，租赁确认',
 '您好！您已成功支付订单 {{orderNo}} 的租金 ¥{{amount}} 元，房源【{{houseTitle}}】已正式租赁给您。合同将在稍后生成，请留意消息通知。',
 1);

-- 3. 新增房东租金到账通知模板
INSERT INTO apartment_notification.notification_template
    (code, name, type, title_template, content_template, enabled)
VALUES
('LANDLORD_RENT_RECEIVED', '房东租金到账通知', 'PAYMENT',
 '租金已到账，房源已出租',
 '您好！您的房源【{{houseTitle}}】已被成功租赁，订单号 {{orderNo}} 的租金 ¥{{amount}} 元已到账，请在"我的钱包"查看余额。',
 1);

-- 4. 新增合同生成通知模板（租客和房东通用）
INSERT INTO apartment_notification.notification_template
    (code, name, type, title_template, content_template, enabled)
VALUES
('CONTRACT_CREATED_NOTICE', '租赁合同已生成通知', 'CONTRACT',
 '租赁合同已生成，请查看并签署',
 '您好！房源【{{houseTitle}}】的租赁合同（合同编号：{{contractNo}}）已生成。请登录系统查看合同详情并完成签署，双方签署后租赁关系正式生效。',
 1);

-- 5. 验证结果
SELECT code, name, type, enabled FROM apartment_notification.notification_template
WHERE code IN ('TENANT_PAY_SUCCESS', 'LANDLORD_RENT_RECEIVED', 'CONTRACT_CREATED_NOTICE');
