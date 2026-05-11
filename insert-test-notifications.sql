-- 插入测试消息数据
USE apartment_notification;

-- 先检查是否已有消息数据
SELECT COUNT(*) as message_count FROM notification_message;

-- 插入测试消息 (假设用户ID为1和2)
INSERT INTO notification_message (user_id, type, title, content, biz_id, is_read, created_at) VALUES
(1, 'ORDER', '您收到了一个新的订单', '您的房源【精装两室一厅】收到了新订单，订单号：ORD20260214001，租期：2026-03-01 至 2026-05-31，租金：¥3000元/月。请及时处理。', 1, 0, NOW()),
(1, 'PAYMENT', '支付成功', '您的订单ORD20260214001支付成功，支付金额：¥9000元，支付方式：钱包支付。', 1, 0, NOW()),
(1, 'CONTRACT', '租赁合同已生成', '订单ORD20260214001的租赁合同已生成（合同编号：CON20260214001），请及时查看并签署。', 1, 0, NOW()),
(2, 'ORDER', '订单支付成功通知', '订单ORD20260214002已成功支付，房源：【温馨单间】，租期：2026-03-01 至 2026-06-30，租金：¥1500元/月。', 2, 0, NOW()),
(2, 'CONTRACT', '合同签署完成', '合同CON20260214002双方已签署完成，租赁关系正式生效。您可以下载合同查看详情。', 2, 1, NOW());

-- 查询插入的数据
SELECT * FROM notification_message ORDER BY created_at DESC;
